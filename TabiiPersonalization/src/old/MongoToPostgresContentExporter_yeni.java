package old;

import com.mongodb.client.*;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.MongoProperties;
import com.tabii.utils.PgProperties;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class MongoToPostgresContentExporter_yeni {

	private static final Logger logger = Logger.getLogger(MongoToPostgresContentExporter_yeni.class.getName());

	
	public static void main(String[] args) {
		migrate();
	}

	public static void migrate() {
		MongoProperties mongoProperties = CommonUtils.getMongoConnectionProps();
		String mongoCollection = "shows";

		PgProperties pgProperties = CommonUtils.getPgConnectionProps();

		try (MongoClient mongoClient = MongoClients.create(mongoProperties.getMongoUri());
				Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
						pgProperties.getDbPassword())) {

			MongoDatabase mongoDb = mongoClient.getDatabase(mongoProperties.getMongoDb());
			MongoCollection<Document> collection = mongoDb.getCollection(mongoCollection);

			exportShows(collection, pgConn);

		} catch (Exception e) {
			logger.severe("❌ Fatal error: " + e.getMessage());
			e.printStackTrace();
			System.exit(1); // stop on fatal error
		}
	}

	private static void exportShows(MongoCollection<Document> collection, Connection pgConn) throws SQLException {
		FindIterable<Document> shows = collection.find();

		for (Document show : shows) {
			Document fields = show.get("fields", Document.class);
			if (fields == null) {
				logger.warning("⚠️ Missing fields for show: " + show.toJson());
				continue;
			}

			Long contentId = insertContent(pgConn, show, fields);
			if (contentId == null) {
				throw new RuntimeException("Failed to insert content for show: " + show.toJson());
			}

			insertImages(pgConn, contentId, fields);
			insertLookupRelations(pgConn, contentId, fields);

			logger.info("✅ Exported show id=" + contentId);
		}
	}

	// ================= INSERT CONTENT ===========================
	private static Long insertContent(Connection conn, Document show, Document fields) throws SQLException {
		String sql = "INSERT INTO contents (id, title, description, spot, made_year, content_type, exclusive_badges) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb) RETURNING id";

		Long id = getContentId(show);
		String title = getStringFromFields(fields, "display_title", "title", show.getString("title"));
		String description = getStringFromFields(fields, "long_description", "text", null);
		String spot = getStringFromFields(fields, "spot", "text", null);
		Integer madeYear = getIntFromFields(fields, "made_year");

		String contentType = show.getString("type");

		List<Document> exclusiveBadgesList = (List<Document>) fields.get("exclusive_badge");
		String exclusiveBadgesJson = convertExclusiveBadges(exclusiveBadgesList);

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, id);
			ps.setString(2, title);
			ps.setString(3, description);
			ps.setString(4, spot);
			if (madeYear != null)
				ps.setInt(5, madeYear);
			else
				ps.setNull(5, Types.INTEGER);
			ps.setString(6, contentType);
			ps.setString(7, exclusiveBadgesJson);

			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getLong("id");
		}

		throw new RuntimeException("Failed to insert content id=" + id);
	}

	private static Long getContentId(Document show) {
		Object idObj = show.get("_id");
		if (idObj instanceof Document d && d.containsKey("$numberLong")) {
			return Long.parseLong(d.getString("$numberLong"));
		} else if (idObj instanceof Number n) {
			return n.longValue();
		} else if (idObj instanceof Long l) {
			return l;
		} else {
			throw new RuntimeException("Invalid _id type: " + idObj);
		}
	}

	private static String convertExclusiveBadges(List<Document> badges) {
		if (badges == null || badges.isEmpty())
			return "[]";
		List<Document> newList = new ArrayList<>();
		for (Document badge : badges) {
			String title = badge.getString("title");
			if (title != null) {
				newList.add(new Document("exclusiveBadgeType", title.toLowerCase()));
			}
		}
		return new Document("exclusiveBadges", newList).toJson();
	}

	private static String getStringFromFields(Document fields, String key, String subkey, String defaultValue) {
		try {
			Object val = fields.get(key);
			if (val instanceof Document doc) {
				if (subkey != null && doc.containsKey(subkey))
					return doc.getString(subkey);
				else
					return doc.toJson();
			} else if (val instanceof String s)
				return s;
		} catch (Exception ignored) {
		}
		return defaultValue;
	}

	private static Integer getIntFromFields(Document fields, String key) {
		try {
			Object val = fields.get(key);
			if (val instanceof Document doc) {
				Document numDoc = doc.get("number", Document.class);
				if (numDoc != null && numDoc.containsKey("$numberLong")) {
					return Integer.parseInt(numDoc.getString("$numberLong"));
				}
			} else if (val instanceof Integer i)
				return i;
			else if (val instanceof Long l)
				return l.intValue();
		} catch (Exception ignored) {
		}
		return null;
	}

	// ================= INSERT IMAGES ===========================
	private static void insertImages(Connection conn, Long contentId, Document fields) throws SQLException {
		if (fields == null)
			return;

		List<String> imageKeys = List.of("main_image", "main_image_with_logo", "cover_image", "mobile_cover_image",
				"vertical_image", "vertical_image_with_logo", "show_logo", "promo_image");

		String insertImgSql = "INSERT INTO images (image_type, filename, title) VALUES (?, ?, ?) "
				+ "ON CONFLICT (filename) DO NOTHING";
		try (PreparedStatement ps = conn.prepareStatement(insertImgSql)) {
			for (String key : imageKeys) {
				Document imgDoc = fields.get(key, Document.class);
				if (imgDoc != null && imgDoc.getString("fileName") != null) {
					ps.setString(1, key);
					ps.setString(2, imgDoc.getString("fileName"));
					ps.setString(3, imgDoc.getString("title"));
					ps.addBatch();
				}
			}
			ps.executeBatch();
		}

		String selectImgSql = "SELECT id FROM images WHERE filename = ?";
		String linkSql = "INSERT INTO content_images (content_id, image_id) VALUES (?, ?) ON CONFLICT DO NOTHING";

		try (PreparedStatement selectPs = conn.prepareStatement(selectImgSql);
				PreparedStatement linkPs = conn.prepareStatement(linkSql)) {

			for (String key : imageKeys) {
				Document imgDoc = fields.get(key, Document.class);
				if (imgDoc != null && imgDoc.getString("fileName") != null) {
					selectPs.setString(1, imgDoc.getString("fileName"));
					ResultSet rs = selectPs.executeQuery();
					if (rs.next()) {
						long imageId = rs.getLong("id");
						linkPs.setLong(1, contentId);
						linkPs.setLong(2, imageId);
						linkPs.addBatch();
					} else {
						logger.warning(
								"⚠️ Image not found: fields." + key + " filename=" + imgDoc.getString("fileName"));
					}
				}
			}
			linkPs.executeBatch();
		}
	}

	// ================= INSERT LOOKUP RELATIONS ===========================
	private static void insertLookupRelations(Connection conn, Long contentId, Document fields) throws SQLException {
	    if (fields == null) return;

	    Map<String, String> lookupMap = Map.of(
	            "genre", "genre",
	            "category", "category",
	            "age-restriction", "age-restriction",
	            "license", "license",
	            "channel", "channel"
	    );

	    for (Map.Entry<String, String> entry : lookupMap.entrySet()) {
	        String fieldKey = entry.getKey();
	        String type = entry.getValue();

	        Object val = fields.get(fieldKey);
	        if (val == null) continue;

	        List<Document> list = new ArrayList<>();
	        if (val instanceof Document d) {
	            // wrap single Document into list
	            list.add(d);
	        } else if (val instanceof List l) {
	            for (Object item : l) {
	                if (item instanceof Document doc) {
	                    list.add(doc);
	                } else if (item instanceof Number || item instanceof String) {
	                    // wrap primitive into Document
	                    Document temp = new Document("contentId", new Document("$numberLong", item.toString()));
	                    list.add(temp);
	                }
	            }
	        } else if (val instanceof Number || val instanceof String) {
	            // wrap primitive into Document
	            Document temp = new Document("contentId", new Document("$numberLong", val.toString()));
	            list.add(temp);
	        }

	        for (int i = 0; i < list.size(); i++) {
	            Document rel = list.get(i);
	            Long typeId = extractContentId(rel);
	            if (typeId == null) continue;

	            Long lookupId = findLookupObjectIdByTypeId(conn, type, typeId);

	            if (lookupId != null) {
	                insertRelation(conn, contentId, lookupId, type);
	            } else {
	                logger.warning("⚠️ Missing lookup: fields." + fieldKey + "[" + i + "] type=" + type + " typeId=" + typeId);
	            }
	        }
	    }
	}


	private static Long findLookupObjectIdByTypeId(Connection conn, String type, Long typeId) throws SQLException {
		String sql = "SELECT id FROM lookup_objects WHERE type = ? AND typeid = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, type);
			ps.setLong(2, typeId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getLong("id");
		}
		return null;
	}

	private static void insertRelation(Connection conn, Long contentId, Long lookupId, String type)
			throws SQLException {
		String sql = "INSERT INTO content_lookup_relations (content_id, lookup_object_id, relation_type) "
				+ "VALUES (?, ?, ?) ON CONFLICT DO NOTHING";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, contentId);
			ps.setLong(2, lookupId);
			ps.setString(3, type);
			ps.executeUpdate();
		}
	}
	
	private static Long extractContentId(Object val) {
	    if (val == null) return null;
	    if (val instanceof Document doc) {
	        // check if contentId is a document
	        Object cid = doc.get("contentId");
	        if (cid instanceof Document cidDoc && cidDoc.containsKey("$numberLong")) {
	            return Long.parseLong(cidDoc.getString("$numberLong"));
	        } else if (cid instanceof Number n) {
	            return ((Number) cid).longValue();
	        } else if (cid instanceof String s) {
	            return Long.parseLong(s);
	        }
	    } else if (val instanceof Number n) {
	        return ((Number) val).longValue();
	    } else if (val instanceof String s) {
	        return Long.parseLong(s);
	    }
	    return null;
	}

}
