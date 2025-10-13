package com.tabii.data.transformers.mongoToPg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.MongoProperties;
import com.tabii.utils.PgProperties;

public class MongoToPostgresContentExporter {

	private static final Logger logger = Logger.getLogger("MongoToPostgresContentExporter");

	public static void main(String[] args) throws Exception {
		migrate();
	}

	public static void migrate() throws Exception {

		MongoProperties mongoProperties = CommonUtils.getMongoConnectionProps();
		String mongoCollection = "shows"; // Mongo collection

		PgProperties pgProperties = CommonUtils.getPgConnectionProps();

		try (MongoClient mongoClient = MongoClients.create(mongoProperties.getMongoUri());
				Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
						pgProperties.getDbPassword())) {

			MongoDatabase mongoDb = mongoClient.getDatabase(mongoProperties.getMongoDb());
			MongoCollection<Document> collection = mongoDb.getCollection(mongoCollection);

			FindIterable<Document> shows = collection.find();
			long count = collection.countDocuments();
			System.out.println("#of shows " + count);
			int i= 0;
			for (Document show : shows) {
				exportShow(show, pgConn);
				System.out.println(i++);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.severe("Export failed: " + e.getMessage());
		}
		System.out.println("Contents exported successfully.");
	}

	private static void exportShow(Document show, Connection pgConn) {
		try {

			Long id = show.getLong("_id");

//			if(id.longValue() != 191180) {
//				return;
//			}

			String contentType = show.getString("type");
			String title = show.getString("title");
			String spot = null;
			Integer madeYear = null;
			String description = null;
			Document fields = show.get("fields", Document.class);
			if (fields != null) {

				spot = (String) getFieldValue(fields, "spot", String.class);
				Long madeYearLong = getLongFromDocument(fields, "made_year");

				// If your PG column is integer, cast safely
				madeYear = madeYearLong != null ? madeYearLong.intValue() : null;

				description = (String) getFieldValue(fields, "long_description", String.class);
			}

			JSONObject exclusiveBadgesJson = extractExclusiveBadges(show);

			// Insert into contents
			String insertContent = """
					INSERT INTO contents (id, title, description, spot, made_year, content_type, "exclusive_badges")
					VALUES (?, ?, ?, ?, ?, ?, to_json(?::jsonb))
					ON CONFLICT (id) DO NOTHING
					""";
			try (PreparedStatement ps = pgConn.prepareStatement(insertContent)) {
				ps.setLong(1, id);
				ps.setString(2, title);
				ps.setString(3, description);
				ps.setString(4, spot);
				if (madeYear != null)
					ps.setInt(5, madeYear);
				else
					ps.setNull(5, Types.INTEGER);
				ps.setString(6, contentType);
				ps.setString(7, exclusiveBadgesJson.toString());
				ps.executeUpdate();
			}

			// Process nested relations
			processImages(show, id, pgConn);
			LookupRelationProcessor.processLookupRelations(show, id, pgConn);
			LookupRelationProcessor.verifyLookupRelations(id, pgConn);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
			logger.warning("❌ Failed to export show: " + e.getMessage());
		}
	}

	private static void processImages(Document show, Long contentId, Connection pgConn) {
		List<PathNode> allDocs = flattenWithPaths(show, "show");

		for (PathNode node : allDocs) {
			Document doc = node.doc;
			if ("image".equalsIgnoreCase(doc.getString("type"))) {
				String filename = doc.getString("fileName");
				if (filename == null)
					continue;
				try {
					int imageId = getImageIdByFilename(pgConn, filename);
					if (imageId == -1) {
						logger.warning("⚠️ Image not found for filename=" + filename + " at path=" + node.path);
						continue;
					}
					insertRelation(pgConn, "content_images", "content_id", "image_id", contentId, (long) imageId);
				} catch (Exception e) {
					logger.warning("⚠️ Error mapping image at path=" + node.path + ": " + e.getMessage());
				}
			}
		}
	}


	private static int getImageIdByFilename(Connection pgConn, String filename) throws SQLException {
		String sql = "SELECT id FROM images WHERE filename = ?";
		try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
			ps.setString(1, filename);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getInt("id");
		}
		return -1;
	}

	private static void insertRelation(Connection pgConn, String table, String col1, String col2, Long id1, Long id2)
			throws SQLException {
		String sql = "INSERT INTO " + table + " (" + col1 + ", " + col2 + ") VALUES (?, ?) ON CONFLICT DO NOTHING";
		try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
			ps.setLong(1, id1);
			ps.setLong(2, id2);
			ps.executeUpdate();
		}
	}

	/** Helper class for document-path tracking **/
	private static class PathNode {
		Document doc;
		String path;

		PathNode(Document doc, String path) {
			this.doc = doc;
			this.path = path;
		}
	}

	/**
	 * Recursively flatten a Mongo Document tree and track JSON-like paths.
	 */
	private static List<PathNode> flattenWithPaths(Document doc, String currentPath) {
		List<PathNode> result = new ArrayList<>();
		result.add(new PathNode(doc, currentPath));

		for (Map.Entry<String, Object> entry : doc.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			String newPath = currentPath + "." + key;

			if (value instanceof Document childDoc) {
				result.addAll(flattenWithPaths(childDoc, newPath));
			} else if (value instanceof List<?>) {
				List<?> list = (List<?>) value;
				for (int i = 0; i < list.size(); i++) {
					Object item = list.get(i);
					if (item instanceof Document) {
						result.addAll(flattenWithPaths((Document) item, newPath + "[" + i + "]"));
					}
				}
			}
		}
		return result;
	}

	private static Object getFieldValue(Document doc, String key, Class<?> type) {
		if (doc == null)
			return null;

		Object value = doc.get(key);
		if (value == null)
			return null;

		// Handle String extraction
		if (type == String.class) {
			if (value instanceof String s)
				return s;

			// If it's a nested Document, try to get "text" key
			if (value instanceof Document d) {
				Object inner = d.get("text");
				if (inner instanceof String innerText)
					return innerText;
				// fallback to JSON if no "text" field found
				return d.toJson();
			}

			return value.toString(); // fallback
		}

		// Handle Integer
		if (type == Integer.class) {
			if (value instanceof Integer i)
				return i;
			if (value instanceof Number n)
				return n.intValue();
			return null;
		}

		// Handle Long
		if (type == Long.class) {
			if (value instanceof Long l)
				return l;
			if (value instanceof Number n)
				return n.longValue();
			return null;
		}

		// Handle Boolean
		if (type == Boolean.class) {
			if (value instanceof Boolean b)
				return b;
			return null;
		}

		// Fallback for JSON objects, lists, etc.
		return value;
	}

	private static Long getLongFromDocument(Document doc, String key) {
		if (doc == null)
			return null;
		Object value = doc.get(key);
		if (value == null)
			return null;

		// value is primitive long/number
		if (value instanceof Number n)
			return n.longValue();

		// value is nested Mongo Extended JSON
		if (value instanceof Document d) {
			Object numberObj = d.get("number");
			if (numberObj instanceof Document numDoc) {
				Object numberLong = numDoc.get("$numberLong");
				if (numberLong instanceof String s) {
					try {
						return Long.parseLong(s);
					} catch (NumberFormatException e) {
						return null;
					}
				}
			}
			// sometimes it could be just a Number inside "number"
			if (numberObj instanceof Number n2)
				return n2.longValue();
		}

		return null;
	}

	private static JSONObject extractExclusiveBadges(Document doc) {
		try {
			Document fields = doc.get("fields", Document.class);

			if (fields != null && fields.containsKey("exclusive_badge")) {
				@SuppressWarnings("unchecked")
				List<Document> badgeList = (List<Document>) fields.get("exclusive_badge");
				JSONArray resultArray = new JSONArray();

				for (Document badge : badgeList) {
					String title = badge.getString("title");
					if (title != null) {
						JSONObject badgeObj = new JSONObject();
						badgeObj.put("exclusiveBadgeType", title.toLowerCase());
						resultArray.put(badgeObj);
					}
				}

				JSONObject result = new JSONObject();
				result.put("exclusiveBadges", resultArray);
				return result;
			}

		} catch (Exception e) {
			// ignore and fallback to empty
		}

		// if field missing, return empty array
		JSONObject result = new JSONObject();
		result.put("exclusiveBadges", new JSONArray());
		return result;
	}

}
