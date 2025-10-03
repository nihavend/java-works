package com.tabii.data.transformers.pgToRedis;

import org.json.JSONObject;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;
import com.tabii.utils.RedisProperties;

import redis.clients.jedis.Jedis;

import java.sql.*;

public class PgImagesToRedisExporter {

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
            ResultSet rs = stmt.executeQuery("SELECT id, image_type, filename, title FROM images");

            while (rs.next()) {
                int id = rs.getInt("id");
                String imageType = rs.getString("image_type");
                String filename = rs.getString("filename");
                String title = rs.getString("title");

                JSONObject obj = new JSONObject();
                obj.put("contentType", "image");
                obj.put("imageType", imageType);
                obj.put("name", filename);
                obj.put("title", title);
                obj.put("id", id);

                String redisKey = "image:" + id;
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
}
