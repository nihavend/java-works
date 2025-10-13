package old;

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
import org.json.JSONArray;
import org.json.JSONObject;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;
import com.tabii.utils.RedisProperties;

import redis.clients.jedis.Jedis;

public class PgLookupsToRedisExporterOld {

	public static void main(String[] args) {
		migrate();
	}

	public static void migrate() {

		// PostgreSQL connection settings
		PgProperties pgProperties = CommonUtils.getPgConnectionProps();

		// Redis connection settings
		RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();

		try (Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
				pgProperties.getDbPassword());
				Jedis jedis = new Jedis(redisProperties.getHost(), redisProperties.getPort())) {
			String sql = "SELECT id, type, typeid, audit, body, fields, isactive, metadata, path, "
					+ "paths, published, site, title, viewcount, audit_user_id " + "FROM lookup_objects";

			try (PreparedStatement ps = pgConn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {
					int id = rs.getInt("id");
					int typeid = rs.getInt("typeid");
					String type = rs.getString("type");
					
					switch (type) {
					case "exclusive-badge":
						migrateExclusiveBadge(jedis, type, id, typeid, rs);
						break;
					case "badges":
						migrateBadge(pgConn, jedis, type, id, typeid, rs);
						break;
					default:
						break;
					}
					
					
					

					if (type != null && type.equals("exclusive-badge")) {
	
					} else {
						
					}
				}
			}

			System.out.println("✅ Export completed successfully!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Object safeJson(String json) {
		if (json == null)
			return JSONObject.NULL;
		try {
			return new JSONObject(json);
		} catch (Exception e) {
			return json;
		}
	}

	/**
	 * Converts a JSON string containing a layout key into an exclusiveBadges JSON
	 * string.
	 *
	 * @param inputJson JSON string like
	 *                  {"layout":{"metadata":{},"value":"Premium","key":"premium"}}
	 * @return JSON string like: "exclusiveBadges":
	 *         [{"exclusiveBadgeType":"premium"}]
	 */
	public static String convertToExclusiveBadges(String inputJson) {
		JSONObject inputObj = new JSONObject(inputJson);

		// Extract key from layout
		String key = inputObj.getJSONObject("layout").getString("key");

		// Create exclusiveBadges JSON
		JSONObject badge = new JSONObject();
		badge.put("exclusiveBadgeType", key);

		JSONArray badgeArray = new JSONArray();
		badgeArray.put(badge);

		JSONObject result = new JSONObject();
		result.put("exclusiveBadges", badgeArray);

		return result.toString(4); // Pretty print with 4 spaces
	}	
	
	private static void migrateExclusiveBadge(Jedis jedis, String type, int id, int typeId, ResultSet rs) throws Exception {
		
		Object fields = safeJson(rs.getString("fields"));
		
		String result = convertToExclusiveBadges(fields.toString());
		// Redis key = type:id (unchanged)
		String redisKey = type + ":" + typeId;
		System.out.println("Redis Key : " + redisKey);
		// JSON value (now includes typeid too)
		JSONObject value = new JSONObject();
		value.put("id", id);
		value.put("type", type);
		value.put("typeid", rs.getLong("typeid")); // new column
		value.put("body", safeJson(result));
		
		jedis.set(redisKey, value.toString());
		System.out.println("Stored in Redis: " + redisKey);
	}
	
	private static void migrateBadge(Connection pgConn, Jedis jedis, String type, int id, int typeId, ResultSet rs) throws Exception {
		
		
		// Redis key = type:id (unchanged)
		String redisKey = type + ":" + typeId;
		System.out.println("Redis Key : " + redisKey);
		
		Object fieldsObj = safeJson(rs.getString("fields"));
		
		// JSON value (now includes typeid too)
		JSONObject value = new JSONObject();
		value.put("id", id);
		value.put("type", type);
		value.put("typeid", rs.getLong("typeid")); // new column
		value.put("audit", safeJson(rs.getString("audit")));
		value.put("body", safeJson(rs.getString("body")));
		value.put("fields", fieldsObj);
		value.put("isactive", rs.getBoolean("isactive"));
		value.put("metadata", safeJson(rs.getString("metadata")));
		value.put("path", rs.getString("path"));
		value.put("paths", safeJson(rs.getString("paths")));
		value.put("published", safeJson(rs.getString("published")));
		value.put("site", rs.getString("site"));
		value.put("title", rs.getString("title"));
		value.put("viewcount", rs.getLong("viewcount"));
		value.put("audit_user_id", rs.getLong("audit_user_id"));

//		jedis.set(redisKey, value.toString());
//		System.out.println("Stored in Redis: " + redisKey);
		
		
		// --- Log unmapped top-level fields ---
		Set<String> mappedFields = new HashSet<>();
		mappedFields.add("images");

		String displayTitle = null;
		String layoutKey = null;
		
		Document fields = Document.parse( fieldsObj.toString());

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
		long viewCount = rs.getLong("viewcount");

		// Document published = doc.get("published", Document.class);
		Document published = Document.parse(safeJson(rs.getString("published")).toString());
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
		// Document audit = doc.get("audit", Document.class);
		Document audit = Document.parse(safeJson(rs.getString("audit")).toString());
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

//		// --- Insert badge ---
//		String sql = "INSERT INTO badges " + "(id, isActive, path, site, title, display_title, layoutId, viewcount, "
//				+ "published_date, published_status, published_revisionId, "
//				+ "modified_user, modified_date, created_user, created_date) "
//				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " + "ON CONFLICT (id) DO NOTHING";
//
//		try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
//			ps.setLong(1, id);
//			ps.setBoolean(2, isActive);
//			ps.setString(3, path);
//			ps.setString(4, site);
//			ps.setString(5, title);
//			ps.setString(6, displayTitle);
//			ps.setInt(7, layoutId);
//			ps.setLong(8, viewCount);
//			ps.setTimestamp(9, publishedDate);
//			ps.setString(10, publishedStatus);
//			if (revisionId != null)
//				ps.setLong(11, revisionId);
//			else
//				ps.setNull(11, Types.BIGINT);
//			ps.setInt(12, modifiedUserId);
//			ps.setTimestamp(13, modifiedDate);
//			ps.setInt(14, createdUserId);
//			ps.setTimestamp(15, createdDate);
//			ps.executeUpdate();
//		}

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
}
