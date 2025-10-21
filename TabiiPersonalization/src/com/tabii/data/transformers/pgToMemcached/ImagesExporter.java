package com.tabii.data.transformers.pgToMemcached;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.MemcachedProperties;
import com.tabii.utils.PgProperties;

import net.spy.memcached.MemcachedClient;

public class ImagesExporter {

	public static void main(String[] args) {
		migrate();
	}
	
	public static void migrate() {
		// PostgreSQL connection
		PgProperties pgProperties = CommonUtils.getPgConnectionProps();

		// Memcached connection
		MemcachedProperties memcachedProperties = CommonUtils.getMemcachedConnectionProps();

		MemcachedClient memcachedClient = null;
		try (Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(), pgProperties.getDbPassword())) {
			memcachedClient = new MemcachedClient(CommonUtils.getServers(memcachedProperties.getServers()));
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

					// Write to Memcached
					memcachedClient.set(redisKey, 3600, value.toString());
					System.out.println("Stored in Memcached: " + redisKey);
				}
			}

			System.out.println("✅ Export completed successfully!");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (memcachedClient != null) {
				memcachedClient.shutdown();
			}
		}
	}
}