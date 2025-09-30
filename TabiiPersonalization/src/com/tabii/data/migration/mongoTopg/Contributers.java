package com.tabii.migration.mongotopgsql;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Properties;
import java.util.UUID;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.tabii.utils.CommonUtils;

public class MongoToPostgres {

    public static void main(String[] args) {
    	
    	Properties dbProps = CommonUtils.loadDbProperties();
	    String mongoUri = dbProps.getProperty("mongo.db.url");
	    String mongoDb = dbProps.getProperty("mongo.db.name");
	    
	    String pgUrl = dbProps.getProperty("pg.db.url");
	    String pgUser = dbProps.getProperty("pg.db.user");
	    String pgPass = dbProps.getProperty("pg.db.password");
	    String mongoCollection = "persons";
	    
        try (
                MongoClient mongoClient = MongoClients.create(mongoUri);
                Connection pgConn = DriverManager.getConnection(pgUrl, pgUser, pgPass)
        ) {
            MongoCollection<Document> collection =
                    mongoClient.getDatabase(mongoDb).getCollection(mongoCollection);

            // contributors insert
            String insertContributorSql = """
                INSERT INTO contributors (
                    id, first_name, last_name, birth_date, nationality,
                    published_date, published_status,
                    created_date, modified_date, modified_user
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            // contributor_roles insert
            String insertRoleSql = """
                INSERT INTO contributor_roles (
                    id, contributor_id, content_id, role_type, character_name
                ) VALUES (?, ?, ?, ?, ?)
                """;

            try (
                    PreparedStatement stmtContributors = pgConn.prepareStatement(insertContributorSql);
                    PreparedStatement stmtRoles = pgConn.prepareStatement(insertRoleSql)
            ) {
                for (Document doc : collection.find()) {
                    String fullName = doc.get("fields", Document.class)
                                         .get("full_name", Document.class)
                                         .getString("text");

                    // 🔹 full_name → first_name + last_name ayrımı
                    String firstName = "";
                    String lastName = "";

                    if (fullName != null) {
                        String[] parts = fullName.trim().split(" ", 2);
                        firstName = parts[0];
                        if (parts.length > 1) {
                            lastName = parts[1];
                        } else {
                            System.out.printf("⚠️ Contributor '%s' has no last_name, inserting empty string%n", firstName);
                            lastName = ""; // NULL yerine boş string
                        }
                    }

                    // contributor id (UUID)
                    String contributorId = UUID.randomUUID().toString();

                    // published alanları
                    Document published = doc.get("published", Document.class);
                    LocalDate publishedDate = null;
                    boolean publishedStatus = false;
                    if (published != null) {
                        String pubDateStr = published.getString("date");
                        if (pubDateStr != null && !pubDateStr.isBlank()) {
                            publishedDate = LocalDate.parse(pubDateStr.substring(0, 10));
                        }
                        publishedStatus = published.getBoolean("status", false);
                    }

                    // audit alanları
                    Document audit = doc.get("audit", Document.class);
                    LocalDate createdDate = null;
                    LocalDate modifiedDate = null;
                    String modifiedUser = null;

                    if (audit != null) {
                        Document created = audit.get("created", Document.class);
                        if (created != null) {
                            String cDate = created.getString("date");
                            if (cDate != null && !cDate.isBlank()) {
                                createdDate = LocalDate.parse(cDate.substring(0, 10));
                            }
                        }
                        Document modified = audit.get("modified", Document.class);
                        if (modified != null) {
                            String mDate = modified.getString("date");
                            if (mDate != null && !mDate.isBlank()) {
                                modifiedDate = LocalDate.parse(mDate.substring(0, 10));
                            }
                        }
                        Document createdUser = created != null ? created.get("user", Document.class) : null;
                        if (createdUser != null) {
                            modifiedUser = createdUser.getString("name");
                        }
                    }

                    // contributors insert
                    stmtContributors.setObject(1, contributorId, Types.OTHER); // UUID
                    stmtContributors.setString(2, firstName);
                    stmtContributors.setString(3, lastName);
                    stmtContributors.setNull(4, Types.DATE); // birth_date yok → null
                    stmtContributors.setNull(5, Types.VARCHAR); // nationality yok → null

                    if (publishedDate != null) {
                        stmtContributors.setDate(6, Date.valueOf(publishedDate));
                    } else {
                        stmtContributors.setNull(6, Types.DATE);
                    }
                    stmtContributors.setBoolean(7, publishedStatus);

                    if (createdDate != null) {
                        stmtContributors.setDate(8, Date.valueOf(createdDate));
                    } else {
                        stmtContributors.setNull(8, Types.DATE);
                    }

                    if (modifiedDate != null) {
                        stmtContributors.setDate(9, Date.valueOf(modifiedDate));
                    } else {
                        stmtContributors.setNull(9, Types.DATE);
                    }

                    stmtContributors.setString(10, modifiedUser);

                    stmtContributors.executeUpdate();

                    // contributor_roles insert
                    Document fields = doc.get("fields", Document.class);
                    if (fields != null) {
                        Document jobTitle = fields.get("job_title", Document.class);
                        if (jobTitle != null) {
                            String roleType = jobTitle.getString("text");
                            if (roleType != null && !roleType.isBlank()) {
                                stmtRoles.setObject(1, UUID.randomUUID().toString(), Types.OTHER);
                                stmtRoles.setObject(2, contributorId, Types.OTHER); // FK contributor_id
                                stmtRoles.setNull(3, Types.OTHER); // content_id şu an null
                                stmtRoles.setString(4, roleType);
                                stmtRoles.setNull(5, Types.VARCHAR); // character_name nullable
                                stmtRoles.executeUpdate();
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
