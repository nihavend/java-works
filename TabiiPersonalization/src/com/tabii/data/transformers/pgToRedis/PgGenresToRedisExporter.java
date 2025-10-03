package com.tabii.data.transformers.pgToRedis;

import org.json.JSONArray;
import org.json.JSONObject;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;
import com.tabii.utils.RedisProperties;

import redis.clients.jedis.Jedis;

import java.sql.*;

public class PgGenresToRedisExporter {

    public static void main(String[] args) {

    	// PostgreSQL connection settings
    	PgProperties pgProperties = CommonUtils.getPgConnectionProps();
    	
    	 // Redis connection settings
    	RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();

        try (
        		Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(), pgProperties.getDbPassword());
        		Jedis jedis = new Jedis(redisProperties.getHost(), redisProperties.getPort())
        ) {
            String sql = "SELECT id, typeid, title, fields FROM lookup_objects WHERE type = 'genre'";
            PreparedStatement stmt = pgConn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                long typeId = rs.getLong("typeid");
                String title = rs.getString("title");
                String fieldsJson = rs.getString("fields");

                JSONArray imagesArr = new JSONArray();

                if (fieldsJson != null) {
                    JSONObject fieldsObj = new JSONObject(fieldsJson);

                    if (fieldsObj.has("background")) {
                        JSONObject bg = fieldsObj.getJSONObject("background");
                        String filename = bg.optString("fileName", null);

                        if (filename != null) {
                            int imageId = findImageId(pgConn, filename);
                            if (imageId != -1) {
                                imagesArr.put(imageId);
                            }
                        }
                    }
                }

                // Build JSON
                JSONObject genreObj = new JSONObject();
                genreObj.put("contentType", "genre");
                genreObj.put("id", typeId);
                genreObj.put("title", title);
                genreObj.put("images", imagesArr);

                String redisKey = "genre:" + typeId;
                jedis.set(redisKey, genreObj.toString());

                System.out.println("✅ Saved to Redis: " + redisKey + " -> " + genreObj);
            }

            rs.close();
            stmt.close();
            System.out.println("🚀 Export completed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int findImageId(Connection conn, String filename) throws SQLException {
        String sql = "SELECT id FROM images WHERE filename = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filename);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return -1;
    }
}
