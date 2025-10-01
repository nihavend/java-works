package com.tabii.data.transformers.mongoToPg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.MongoProperties;
import com.tabii.utils.PgProperties;

public class Contributers {

	public static void main(String[] args) {

		PgProperties pgProperties = CommonUtils.getPgConnectionProps();
		
		MongoProperties mongoProperties = CommonUtils.getMongoConnectionProps();
		String mongoCollection = "persons";

		// Varsayılan kullanıcı id (FK users.id)
		int systemUserId = 1; // migration user

		try (MongoClient mongoClient = MongoClients.create(mongoProperties.getMongoUri());
				Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(), pgProperties.getDbPassword())) {
			MongoCollection<Document> collection = mongoClient.getDatabase(mongoProperties.getMongoDb()).getCollection(mongoCollection);

			// contributors insert
			String insertContributorSql = """
					INSERT INTO contributors (
					    first_name, last_name, nationality, birth_date,
					    published_date, published_status,
					    modified_user, modified_date, created_user, created_date
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
					RETURNING id
					""";

			// contributor_roles insert
			String insertRoleSql = """
					INSERT INTO contributor_roles (
					    contributor_id, content_id, role_type, character_name
					) VALUES (?, ?, ?, ?)
					""";

			try (PreparedStatement stmtContributors = pgConn.prepareStatement(insertContributorSql);
					PreparedStatement stmtRoles = pgConn.prepareStatement(insertRoleSql)) {
				for (Document doc : collection.find()) {
					// ----------- Contributor Alanları -----------
					String fullName = doc.get("fields", Document.class).get("full_name", Document.class)
							.getString("text");

					String firstName = "";
					String lastName = "";

					if (fullName != null) {
						String[] parts = fullName.trim().split(" ", 2);
						firstName = parts[0];
						if (parts.length > 1) {
							lastName = parts[1];
						} else {
							System.out.printf("⚠️ Contributor '%s' has no last_name, inserting empty string%n",
									firstName);
							lastName = "";
						}
					}

					// published alanları
					Document published = doc.get("published", Document.class);
					Timestamp publishedDate = null;
					String publishedStatus = null;
					if (published != null) {
						String pubDateStr = published.getString("date");
						if (pubDateStr != null && !pubDateStr.isBlank()) {
							publishedDate = Timestamp.valueOf(pubDateStr.replace("T", " ").substring(0, 19));
						}
						Boolean status = published.getBoolean("status");
						if (status != null) {
							publishedStatus = status ? "true" : "false";
						}
					}

					// audit alanları
					Document audit = doc.get("audit", Document.class);
					Timestamp createdDate = null;
					Timestamp modifiedDate = null;

					if (audit != null) {
						Document created = audit.get("created", Document.class);
						if (created != null) {
							String cDate = created.getString("date");
							if (cDate != null && !cDate.isBlank()) {
								createdDate = Timestamp.valueOf(cDate.replace("T", " ").substring(0, 19));
							}
						}
						Document modified = audit.get("modified", Document.class);
						if (modified != null) {
							String mDate = modified.getString("date");
							if (mDate != null && !mDate.isBlank()) {
								modifiedDate = Timestamp.valueOf(mDate.replace("T", " ").substring(0, 19));
							}
						}
					}

					// contributors insert
					stmtContributors.setString(1, firstName);
					stmtContributors.setString(2, lastName);
					stmtContributors.setNull(3, Types.VARCHAR); // nationality
					stmtContributors.setNull(4, Types.TIMESTAMP); // birth_date
					if (publishedDate != null)
						stmtContributors.setTimestamp(5, publishedDate);
					else
						stmtContributors.setNull(5, Types.TIMESTAMP);
					stmtContributors.setString(6, publishedStatus);
					stmtContributors.setInt(7, systemUserId); // modified_user
					if (modifiedDate != null)
						stmtContributors.setTimestamp(8, modifiedDate);
					else
						stmtContributors.setNull(8, Types.TIMESTAMP);
					stmtContributors.setInt(9, systemUserId); // created_user
					if (createdDate != null)
						stmtContributors.setTimestamp(10, createdDate);
					else
						stmtContributors.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));

					ResultSet rsContrib = stmtContributors.executeQuery();
					int contributorId = -1;
					if (rsContrib.next()) {
						contributorId = rsContrib.getInt(1);
					}
					rsContrib.close();

					
					// ----------- Role Alanları (content_id eşleşmezse logla ve geç) -----------
					Document fields = doc.get("fields", Document.class);
					if (fields != null) {
					    Document jobTitle = fields.get("job_title", Document.class);
					    if (jobTitle != null) {
					        String roleType = jobTitle.getString("text");

					        // Mongo'dan content_id var mı?
					        Integer contentId = null;
					        if (fields.containsKey("content_id")) {
					            try {
					                contentId = fields.getInteger("content_id");
					            } catch (Exception ex) {
					                System.out.printf("⚠️ content_id parse hatası contributorId=%d → %s%n", contributorId, ex.getMessage());
					            }
					        }

					        if (roleType != null && !roleType.isBlank()) {
					            if (contentId != null) {
					                // PostgreSQL'de content var mı?
					                try (PreparedStatement checkContent =
					                             pgConn.prepareStatement("SELECT id FROM contents WHERE id = ?")) {
					                    checkContent.setInt(1, contentId);
					                    ResultSet rsContent = checkContent.executeQuery();
					                    if (rsContent.next()) {
					                        // eşleşme var → insert et
					                        stmtRoles.setInt(1, contributorId);
					                        stmtRoles.setInt(2, contentId);
					                        stmtRoles.setString(3, roleType);
					                        stmtRoles.setNull(4, Types.VARCHAR); // character_name
					                        stmtRoles.executeUpdate();
					                    } else {
					                        // eşleşme yok → logla
					                        System.out.printf("❌ Eşleşmeyen content_id=%d, contributorId=%d, role=%s%n",
					                                contentId, contributorId, roleType);
					                    }
					                    rsContent.close();
					                }
					            } else {
					                System.out.printf("❌ content_id bulunamadı contributorId=%d, role=%s%n",
					                        contributorId, roleType);
					            }
					        }
					    }
					}
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
