package com.tabii.data.transformers.mongoToPg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bson.Document;

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
			for (Document show : shows) {
				exportShow(show, pgConn);
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
			String contentType = show.getString("type");
			String title = show.getString("title");
			String spot = null;
			Integer madeYear = null;
			String description = null;
			// exclusiveBadges is now inside 'fields' and optional
			Object exclusiveBadges = null;
			Document fields = show.get("fields", Document.class);
			if (fields != null) {

				spot = (String) getFieldValue(fields, "spot", String.class);
				Long madeYearLong = getLongFromDocument(fields, "made_year");

				// If your PG column is integer, cast safely
				madeYear = madeYearLong != null ? madeYearLong.intValue() : null;

				description = (String) getFieldValue(fields, "long_description", String.class);
				
				// Get exclusive_badge from fields (optional)
				Object exclusiveBadgeObj = fields != null ? fields.get("exclusive_badge") : null;

				// Transform to simplified array
				List<Document> exclusiveBadgesList = transformExclusiveBadges(exclusiveBadgeObj);

				// Convert to JSON string for PostgreSQL column
				exclusiveBadges = exclusiveBadgesList.isEmpty() ? "[]" : exclusiveBadgesList.toString();
			}

			// Insert into contents
			String insertContent = """
					INSERT INTO contents (id, title, description, spot, made_year, content_type, "exclusiveBadges")
					VALUES (?, ?, ?, ?, ?, ?, to_json(?::text))
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
				ps.setString(7, exclusiveBadges != null ? exclusiveBadges.toString() : "{}");
				ps.executeUpdate();
			}

			// Process nested relations
			processImages(show, id, pgConn);
			processLookupRelations(show, id, pgConn);

		} catch (Exception e) {
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

	private static void processLookupRelations(Document show, Long contentId, Connection pgConn) {
		List<String> lookupTypes = Arrays.asList("parental-guide", "age-restriction", "category", "exclusive-badge",
				"badge", "genre", "badges");

		List<PathNode> allDocs = flattenWithPaths(show, "show");

		for (PathNode node : allDocs) {
			Document doc = node.doc;
			String type = doc.getString("type");
			if (type == null || !lookupTypes.contains(type))
				continue;

			try {
				Long lookupTypeId = getLookupTypeId(pgConn, type);
				if (lookupTypeId == null) {
					logger.warning("⚠️ Lookup object not found for type=" + type + " at path=" + node.path);
					continue;
				}

				String relTable = switch (type) {
				case "genre" -> "content_genres";
				case "badge", "badges", "exclusive-badge" -> "content_badges";
				default -> "content_lookup_relations";
				};

				String col2 = switch (type) {
				case "genre" -> "genre_id";
				case "badge", "badges", "exclusive-badge" -> "badge_id";
				default -> "lookup_id";
				};

				insertRelation(pgConn, relTable, "content_id", col2, contentId, lookupTypeId);

			} catch (Exception e) {
				logger.warning(
						"⚠️ Failed to map lookup type=" + type + " at path=" + node.path + ": " + e.getMessage());
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

	private static Long getLookupTypeId(Connection pgConn, String type) throws SQLException {
		String sql = "SELECT typeid FROM lookup_objects WHERE type = ?";
		try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
			ps.setString(1, type);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getLong("typeid");
		}
		return null;
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

		if (type == String.class) {
			if (value instanceof String s)
				return s;
			if (value instanceof Document d)
				return d.toJson();
			return value.toString(); // fallback
		}

		if (type == Integer.class) {
			if (value instanceof Integer i)
				return i;
			if (value instanceof Number n)
				return n.intValue();
			return null;
		}

		if (type == Long.class) {
			if (value instanceof Long l)
				return l;
			if (value instanceof Number n)
				return n.longValue();
			return null;
		}

		if (type == Boolean.class) {
			if (value instanceof Boolean b)
				return b;
			return null;
		}

		return value; // fallback for JSON objects, lists, etc.
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

	private static List<Document> transformExclusiveBadges(Object exclusiveBadgeObj) {
		List<Document> result = new ArrayList<>();

		if (exclusiveBadgeObj instanceof List<?> list) {
			for (Object item : list) {
				if (item instanceof Document doc) {
					String title = doc.getString("title");
					if (title != null) {
						Document badge = new Document();
						badge.append("exclusiveBadgeType", title.toLowerCase());
						result.add(badge);
					}
				}
			}
		}

		return result;
	}

}
