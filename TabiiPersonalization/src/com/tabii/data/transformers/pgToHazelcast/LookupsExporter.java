package com.tabii.data.transformers.pgToHazelcast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.HazelcastProperties;
import com.tabii.utils.PgProperties;

public class LookupsExporter {

	public static void main(String[] args) {
		migrate();
	}

	public static void migrate() {
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

			exportLookupObjects(pgConn, hzi);

			System.out.println("✅ Export completed successfully.");
			IMap<String, String> map = hzi.getMap("exclusiveBadgesMap");
			System.out.println("✅ Size of the map " + map.getName() + " : " + map.size());

			map = hzi.getMap("badgesMap");
			System.out.println("✅ Size of the map " + map.getName() + " : " + map.size());
			
			map = hzi.getMap("genreMap");
			System.out.println("✅ Size of the map " + map.getName() + " : " + map.size());

			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			hzi.shutdown();
		}
	}

	private static void exportLookupObjects(Connection pgConn, HazelcastInstance hzi) throws Exception {

		String query = "SELECT id, type, fields FROM lookup_objects";
		try (PreparedStatement ps = pgConn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				long id = rs.getLong("id");
				String type = rs.getString("type");
				String fieldsJson = rs.getString("fields");

				if (fieldsJson == null || fieldsJson.isEmpty())
					continue;

				JSONObject fields = new JSONObject(fieldsJson);
				String key = type + ":" + id;

				switch (type) {
				case "exclusive-badge":
					handleExclusiveBadge(hzi, key, fields);
					break;

				case "badges":
					handleBadge(pgConn, hzi, key, id, fields);
					break;

				case "genre":
					handleGenre(pgConn, hzi, key, id, fields);
					break;

				default:
					// ignore unknown types
					break;
				}
			}
		}
	}

	private static void handleExclusiveBadge(HazelcastInstance hzi, String key, JSONObject fields) {
		String exclusiveType = fields.optJSONObject("layout") != null
				? fields.getJSONObject("layout").optString("key", "")
				: "";

		JSONObject badgeObj = new JSONObject();
		badgeObj.put("exclusiveBadgeType", exclusiveType);

		JSONArray arr = new JSONArray();
		arr.put(badgeObj);

		JSONObject root = new JSONObject();
		root.put("exclusiveBadges", arr);

		IMap<String, String> map = hzi.getMap("exclusiveBadgesMap");
		map.put(key, root.toString());
		System.out.println("Stored in Hazelcast: " + key);
	}

	private static void handleBadge(Connection pgConn, HazelcastInstance hzi, String key, long id, JSONObject fields)
			throws SQLException {
		JSONObject badgeObj = new JSONObject();
		badgeObj.put("bannerLocation", CommonUtils.mapLocation(getNestedKey(fields, "banner_location.key")));
		badgeObj.put("id", id);
		badgeObj.put("images", getImageIdsFromFields(pgConn, fields));
		badgeObj.put("showLocation", CommonUtils.mapLocation(getNestedKey(fields, "show_card_location.key")));
		badgeObj.put("title", getNestedKey(fields, "display_title.text"));
		badgeObj.put("type", getNestedKey(fields, "layout.key"));

		IMap<String, String> map = hzi.getMap("badgesMap");
		map.put(key, badgeObj.toString());
		System.out.println("Stored in Hazelcast: " + key);
	}

	private static void handleGenre(Connection pgConn, HazelcastInstance hzi, String key, long id, JSONObject fields)
			throws SQLException {
		JSONObject genreObj = new JSONObject();
		genreObj.put("contentType", "genre");
		genreObj.put("id", id);
		genreObj.put("images", getImageIdsFromFields(pgConn, fields));
		genreObj.put("title", getNestedKey(fields, "display_title.text"));

		IMap<String, String> map = hzi.getMap("genreMap");
		map.put(key, genreObj.toString());
		System.out.println("Stored in Hazelcast: " + key);
	}

	private static JSONArray getImageIdsFromFields(Connection pgConn, JSONObject fields) throws SQLException {
		List<Integer> imageIds = new ArrayList<>();

		for (String key : fields.keySet()) {
			Object val = fields.get(key);
			if (val instanceof JSONObject) {
				JSONObject node = (JSONObject) val;
				if ("image".equalsIgnoreCase(node.optString("type"))) {
					String fileName = node.optString("fileName");
					if (fileName != null && !fileName.isEmpty()) {
						Integer id = findImageIdByFilename(pgConn, fileName);
						if (id != null)
							imageIds.add(id);
					}
				}
			}
		}

		JSONArray result = new JSONArray();
		for (Integer imgId : imageIds)
			result.put(imgId);
		return result;
	}

	private static Integer findImageIdByFilename(Connection pgConn, String filename) throws SQLException {
		String sql = "SELECT id FROM images WHERE filename = ?";
		try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
			ps.setString(1, filename);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getInt("id");
			}
		}
		return null;
	}

	private static String getNestedKey(JSONObject obj, String path) {
		String[] parts = path.split("\\.");
		JSONObject current = obj;
		for (int i = 0; i < parts.length - 1; i++) {
			if (current.has(parts[i]) && current.get(parts[i]) instanceof JSONObject) {
				current = current.getJSONObject(parts[i]);
			} else {
				return "";
			}
		}
		return current.optString(parts[parts.length - 1], "");
	}

}
