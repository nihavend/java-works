package com.tabii.data.transformers.pgToRedis;

import org.json.JSONArray;
import org.json.JSONObject;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;
import com.tabii.utils.RedisProperties;

import redis.clients.jedis.Jedis;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PgContentsToRedisExporter {

    public static void main(String[] args) {


    	// PostgreSQL connection settings
    	PgProperties pgProperties = CommonUtils.getPgConnectionProps();
    	
    	 // Redis connection settings
    	RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();

        try (
        		Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(), pgProperties.getDbPassword());
        		Jedis jedis = new Jedis(redisProperties.getHost(), redisProperties.getPort())
        ) {
            Statement stmt = pgConn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, title, description, spot, made_year, content_type FROM contents");

            while (rs.next()) {
                long contentId = rs.getLong("id");
                String title = rs.getString("title");
                String description = rs.getString("description");
                String spot = rs.getString("spot");
                Integer madeYear = rs.getInt("made_year");
                if (rs.wasNull()) madeYear = null;
                String contentType = rs.getString("content_type");

                // --- Fetch relations ---
                List<String> badges = fetchIds(pgConn, "SELECT badge_id FROM content_badges WHERE content_id = ?", contentId);
                List<String> genres = fetchIds(pgConn, "SELECT genre_id FROM content_genres WHERE content_id = ?", contentId);
                List<String> images = fetchIds(pgConn, "SELECT image_id FROM content_images WHERE content_id = ?", contentId);

                // --- Build JSON ---
                JSONObject obj = new JSONObject();
                obj.put("badges", new JSONArray(badges));
                obj.put("exclusiveBadges", new JSONArray()); // empty array for now
                obj.put("favorite", false);                  // default false
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
            }

            rs.close();
            stmt.close();
            System.out.println("🚀 Export completed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
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
