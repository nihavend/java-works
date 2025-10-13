package com.tabii.data.transformers.pgToRedis;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;
import com.tabii.utils.RedisProperties;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ImagesToRedisExporter {

	public static void main(String[] args) {
		migrate();
	}
	
	public static void migrate() {
		// PostgreSQL connection
		PgProperties pgProperties = CommonUtils.getPgConnectionProps();

		// Redis connection
		RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();

		try (Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
				pgProperties.getDbPassword());
				Jedis jedis = new Jedis(redisProperties.getUrl())) {
			String sql = "SELECT id, image_type, filename, title FROM images";

			try (PreparedStatement ps = pgConn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {
					int id = rs.getInt("id");

					// Redis key = "image:id"
					String redisKey = "image:" + id;

					// Build JSON value
					JSONObject value = new JSONObject();
					value.put("id", id);
					value.put("contentType", "image"); // fixed value
					value.put("imageType", rs.getString("image_type"));
					value.put("name", rs.getString("filename"));
					value.put("title", rs.getString("title"));

					// Write to Redis
					jedis.set(redisKey, value.toString());
					System.out.println("Stored in Redis: " + redisKey);
				}
			}

			System.out.println("✅ Export completed successfully!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
