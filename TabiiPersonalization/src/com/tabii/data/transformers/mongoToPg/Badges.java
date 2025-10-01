package com.tabii.data.transformers.mongoToPg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.MongoProperties;
import com.tabii.utils.PgProperties;

public class Badges {

	public static void main(String[] args) throws Exception {

		// Load connection properties from internal utils
		PgProperties pgProperties = CommonUtils.getPgConnectionProps();
		MongoProperties mongoProperties = CommonUtils.getMongoConnectionProps();
		String mongoCollection = "lookup";

		MongoClient mongoClient = MongoClients.create(mongoProperties.getMongoUri());
		MongoDatabase mongoDb = mongoClient.getDatabase(mongoProperties.getMongoDb());
		MongoCollection<Document> collection = mongoDb.getCollection(mongoCollection);

		Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
				pgProperties.getDbPassword());

		for (Document doc : collection.find()) {
			if ("badge".equals(doc.getString("type"))) {
				migrateBadge(doc, pgConn);
			}
		}

		pgConn.close();
		mongoClient.close();

	}

	private static void migrateBadge(Document doc, Connection pgConn) throws Exception {
		// --- Log unmapped top-level fields ---
		Set<String> mappedFields = new HashSet<>();
		mappedFields.add("id");
		mappedFields.add("isActive");
		mappedFields.add("path");
		mappedFields.add("site");
		mappedFields.add("title");
		mappedFields.add("fields");
		mappedFields.add("viewCount");
		mappedFields.add("published");
		mappedFields.add("audit");
		mappedFields.add("images");
		mappedFields.add("type");

		for (String key : doc.keySet()) {
			if (!mappedFields.contains(key)) {
				System.out.println("Unmapped badge field: " + key + " = " + doc.get(key));
			}
		}

		// --- Extract badge fields ---
		long id = doc.get("id", Number.class).longValue();
		boolean isActive = doc.getBoolean("isActive", false);
		String path = doc.getString("path");
		String site = doc.getString("site");
		String title = doc.getString("title");

		Document fields = doc.get("fields", Document.class);
		String displayTitle = null;
		String layoutKey = null;

		if (fields != null) {
			if (fields.containsKey("display_title")) {
				displayTitle = ((Document) fields.get("display_title")).getString("text");
			}
			if (fields.containsKey("layout")) {
				Document layout = (Document) fields.get("layout");
				layoutKey = layout.getString("key");
			}
		}

		int layoutId = getLayoutIdFromPostgres(layoutKey, pgConn);
		long viewCount = doc.get("viewCount", Number.class) != null ? doc.get("viewCount", Number.class).longValue()
				: 0L;

		Document published = doc.get("published", Document.class);
		Timestamp publishedDate = null;
		String publishedStatus = null;
		Long revisionId = null;
		if (published != null) {
			if (published.containsKey("date")) {
				publishedDate = Timestamp.from(Instant.parse(published.getString("date")));
			}
			publishedStatus = String.valueOf(published.getBoolean("status", false));
			if (published.containsKey("revisionId")) {
				revisionId = published.get("revisionId", Number.class).longValue();
			}
		}

		// --- Safe extraction of created and modified users ---
		Document audit = doc.get("audit", Document.class);
		Document created = audit != null ? audit.get("created", Document.class) : null;
		Document modified = audit != null ? audit.get("modified", Document.class) : null;

		Document createdUserDoc = created != null ? created.get("user", Document.class) : null;
		Document modifiedUserDoc = modified != null ? modified.get("user", Document.class) : null;

		int createdUserId = 0;
		if (createdUserDoc != null) {
			String email = createdUserDoc.getString("email");
			if (email != null) {
				createdUserId = getUserIdByEmail(email, pgConn);
			}
		}

		int modifiedUserId = 0;
		if (modifiedUserDoc != null) {
			String email = modifiedUserDoc.getString("email");
			if (email != null) {
				modifiedUserId = getUserIdByEmail(email, pgConn);
			} else {
				modifiedUserId = createdUserId;
			}
		} else {
			modifiedUserId = createdUserId;
		}

		Timestamp createdDate = created != null && created.containsKey("date")
				? Timestamp.from(Instant.parse(created.getString("date")))
				: new Timestamp(System.currentTimeMillis());

		Timestamp modifiedDate = modified != null && modified.containsKey("date")
				? Timestamp.from(Instant.parse(modified.getString("date")))
				: createdDate;

		// --- Insert badge ---
		String sql = "INSERT INTO badges " + "(id, isActive, path, site, title, display_title, layoutId, viewcount, "
				+ "published_date, published_status, published_revisionId, "
				+ "modified_user, modified_date, created_user, created_date) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " + "ON CONFLICT (id) DO NOTHING";

		try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
			ps.setLong(1, id);
			ps.setBoolean(2, isActive);
			ps.setString(3, path);
			ps.setString(4, site);
			ps.setString(5, title);
			ps.setString(6, displayTitle);
			ps.setInt(7, layoutId);
			ps.setLong(8, viewCount);
			ps.setTimestamp(9, publishedDate);
			ps.setString(10, publishedStatus);
			if (revisionId != null)
				ps.setLong(11, revisionId);
			else
				ps.setNull(11, Types.BIGINT);
			ps.setInt(12, modifiedUserId);
			ps.setTimestamp(13, modifiedDate);
			ps.setInt(14, createdUserId);
			ps.setTimestamp(15, createdDate);
			ps.executeUpdate();
		}

		// --- Insert badge images ---
		if (doc.containsKey("images")) {
			List<Document> images = (List<Document>) doc.get("images");
			for (Document img : images) {
				int imageId = upsertImage(img, pgConn);
				linkBadgeImage(id, imageId, pgConn);
			}
		}
	}

	private static int getLayoutIdFromPostgres(String layoutKey, Connection pgConn) throws SQLException {
		if (layoutKey == null)
			return 0;
		String sql = "SELECT id FROM layouts WHERE key = ?";
		try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
			ps.setString(1, layoutKey);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt("id");
			} else {
				System.err.println("Layout not found: " + layoutKey);
				return 0;
			}
		}
	}

	private static int getUserIdByEmail(String email, Connection pgConn) throws SQLException {
		if (email == null)
			return 0;
		String sql = "SELECT id FROM users WHERE email = ?";
		try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
			ps.setString(1, email);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt("id");
			} else {
				System.err.println("User not found: " + email);
				return 0;
			}
		}
	}

	private static int upsertImage(Document img, Connection pgConn) throws SQLException {
		String filename = img.getString("filename");
		String title = img.getString("title");
		String type = img.getString("type");

		String select = "SELECT id FROM images WHERE filename = ?";
		try (PreparedStatement ps = pgConn.prepareStatement(select)) {
			ps.setString(1, filename);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt("id");
			}
		}

		String insert = "INSERT INTO images (filename, title, image_type) VALUES (?, ?, ?) RETURNING id";
		try (PreparedStatement ps = pgConn.prepareStatement(insert)) {
			ps.setString(1, filename);
			ps.setString(2, title);
			ps.setString(3, type);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt("id");
			}
		}
		throw new SQLException("Image insert failed for " + filename);
	}

	private static void linkBadgeImage(long badgeId, int imageId, Connection pgConn) throws SQLException {
		String sql = "INSERT INTO badge_images (badge_id, image_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
		try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
			ps.setLong(1, badgeId);
			ps.setInt(2, imageId);
			ps.executeUpdate();
		}
	}
}
