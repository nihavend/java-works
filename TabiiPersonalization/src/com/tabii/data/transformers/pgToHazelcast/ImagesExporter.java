package com.tabii.data.transformers.pgToHazelcast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.HazelcastProperties;
import com.tabii.utils.PgProperties;

public class ImagesExporter {

	public static void main(String[] args) {
		migrate();
	}

	public static void migrate() {
		// PostgreSQL connection
		PgProperties pgProperties = CommonUtils.getPgConnectionProps();

		// Hazelcast connection
		HazelcastProperties hzp = CommonUtils.getHazelcastConnectionProps();
		ClientConfig config = new ClientConfig();
		config.setClusterName(hzp.getClusterName());
		config.getNetworkConfig().setAddresses(CommonUtils.serversSplitter(hzp.getServers()));
		HazelcastInstance hzi = null;
		
		try (Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
				pgProperties.getDbPassword())) {
			hzi = HazelcastClient.newHazelcastClient(config);

			String sql = "SELECT id, image_type, filename, title FROM images";

			try (PreparedStatement ps = pgConn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
				// Write to Hazelcast
				IMap<String, String> map = hzi.getMap("imagesMap");
				
				while (rs.next()) {
					int id = rs.getInt("id");

					// Map key = "image:id"
					String key = "image:" + id;

					// Build JSON value
					JSONObject value = new JSONObject();
					value.put("id", id);
					value.put("contentType", "image"); // fixed value
					value.put("imageType", rs.getString("image_type"));
					value.put("name", rs.getString("filename"));
					value.put("title", rs.getString("title"));
					
					map.put(key, value.toString());
					System.out.println("Stored in Hazelcast: " + key);
				}
				
				System.out.println("Size of map : " + map.size());
				
			}

			System.out.println("✅ Export completed successfully!");
			
			IMap<String, String> map = hzi.getMap("imagesMap");
			System.out.println("✅ Size of the map " + map.size());

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			hzi.shutdown();
		}
	}
}