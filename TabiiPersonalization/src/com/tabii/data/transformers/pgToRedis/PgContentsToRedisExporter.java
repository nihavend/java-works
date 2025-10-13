package com.tabii.data.transformers.pgToRedis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;
import com.tabii.utils.RedisProperties;

import redis.clients.jedis.Jedis;

public class PgContentsToRedisExporter {

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
				Jedis jedis = new Jedis(redisProperties.getUrl())) {
			Statement stmt = pgConn.createStatement();
			ResultSet rs = stmt.executeQuery(
					"SELECT id, title, description, spot, made_year, content_type, exclusive_badges FROM contents");
			int counter = 0;
			while (rs.next()) {
				long contentId = rs.getLong("id");
				String title = rs.getString("title");
				String description = rs.getString("description");
				String spot = rs.getString("spot");
				Integer madeYear = rs.getInt("made_year");
				if (rs.wasNull())
					madeYear = null;
				String contentType = rs.getString("content_type");

				String exclusiveBadgesType = rs.getString("exclusive_badges");

				// --- Fetch relations ---
				List<String> badges = fetchIds(pgConn,
						"SELECT lookup_object_id FROM content_lookup_relations WHERE relation_type='badges' and content_id = ?",
						contentId);
				List<String> genres = fetchIds(pgConn,
						"SELECT lookup_object_id FROM content_lookup_relations WHERE relation_type='genre' and content_id = ?",
						contentId);
				List<String> images = fetchIds(pgConn, "SELECT image_id FROM content_images WHERE content_id = ?",
						contentId);

				// --- Build JSON ---
				JSONObject obj = new JSONObject();
				obj.put("badges", new JSONArray(badges));
				if (isExclusiveBadgesValid(exclusiveBadgesType)) {
					try {
						JSONObject ebj = new JSONObject(exclusiveBadgesType);
				        // Extract key
				        String key = ebj.keys().next(); // "exclusiveBadges"
				        // Extract value (JSONArray)
				        JSONArray valueArray = ebj.getJSONArray(key);
						obj.put("exclusiveBadges", valueArray);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				obj.put("favorite", false); // default false
				obj.put("genres", new JSONArray(genres));
				obj.put("images", new JSONArray(images));
				obj.put("contentType", contentType);
				obj.put("description", description);
				obj.put("madeYear", madeYear);
				obj.put("spot", spot);
				obj.put("title", title);

				String redisKey = "show:" + contentId;
				jedis.set(redisKey, obj.toString());

				System.out.println("✅ Saved to Redis: " + redisKey + " -> " + obj);
				counter++;
			}

			rs.close();
			stmt.close();
			System.out.println("🚀 Export completed." + counter);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static boolean isExclusiveBadgesValid(String s) {
		if (s != null && s.equals("[]")) return false;
		
		JSONObject jsonObject = new JSONObject(s);

        if (jsonObject.has("exclusiveBadges")) {
            JSONArray badgesArray = jsonObject.getJSONArray("exclusiveBadges");
            if (badgesArray.isEmpty()) {
                // System.out.println("exclusiveBadges is an empty array.");
                return false;
            } else {
                // System.out.println("exclusiveBadges is NOT empty.");
            }
        } else {
            System.out.println("exclusiveBadges key is missing.");
            return false;
        }
		return true;
	}

	private static List<String> fetchIds(Connection conn, String sql, long contentId) throws SQLException {
		List<String> ids = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, contentId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ids.add(String.valueOf(rs.getLong(1)));
			}
			rs.close();
		}
		return ids;
	}
}
