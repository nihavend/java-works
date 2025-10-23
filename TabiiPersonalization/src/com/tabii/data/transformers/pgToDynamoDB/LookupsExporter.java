package com.tabii.data.transformers.pgToDynamoDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.DynamoDBProperties;
import com.tabii.utils.PgProperties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class LookupsExporter {

	public static void main(String[] args) {
		migrate();
	}

	public static void migrate() {
		// PostgreSQL connection
		PgProperties pgProperties = CommonUtils.getPgConnectionProps();

		DynamoDBProperties dynamoProperties = CommonUtils.getDynamoDBConnectionProps();

		// --- DynamoDB setup ---
		DynamoDbClient dynamoClient = DynamoDbClient.builder()
				.endpointOverride(java.net.URI.create(dynamoProperties.getUri()))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(dynamoProperties.getAccessKey(), dynamoProperties.getSecretKey())))
				.region(Region.US_EAST_1).build();

		try (Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
				pgProperties.getDbPassword())) {

			exportLookupObjects(pgConn, dynamoClient);

			System.out.println("✅ Export completed successfully.");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void exportLookupObjects(Connection pgConn, DynamoDbClient dC) throws Exception {

		String query = "SELECT id, type, fields FROM lookup_objects";
		try (PreparedStatement ps = pgConn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				long id = rs.getLong("id");
				String type = rs.getString("type");
				String fieldsJson = rs.getString("fields");

				if (fieldsJson == null || fieldsJson.isEmpty())
					continue;

				JSONObject fields = new JSONObject(fieldsJson);
				String redisKey = type + ":" + id;

				switch (type) {
				case "exclusive-badge":
					handleExclusiveBadge(dC, redisKey, fields);
					break;

				case "badges":
					handleBadge(pgConn, dC, redisKey, id, fields);
					break;

				case "genre":
					handleGenre(pgConn, dC, redisKey, id, fields);
					break;

				default:
					// ignore unknown types
					break;
				}
			}
		}
	}

	private static void handleExclusiveBadge(DynamoDbClient dC, String key, JSONObject fields) {
		String exclusiveType = fields.optJSONObject("layout") != null
				? fields.getJSONObject("layout").optString("key", "")
				: "";

		JSONObject badgeObj = new JSONObject();
		badgeObj.put("exclusiveBadgeType", exclusiveType);

		JSONArray arr = new JSONArray();
		arr.put(badgeObj);

		JSONObject root = new JSONObject();
		root.put("exclusiveBadges", arr);

		CommonUtils.dynamoDataCreator(dC, "exclusive-badges", key, badgeObj.toString());
	}

	private static void handleBadge(Connection pgConn, DynamoDbClient dC, String key, long id, JSONObject fields)
			throws SQLException {
		JSONObject badgeObj = new JSONObject();
		badgeObj.put("bannerLocation", CommonUtils.mapLocation(getNestedKey(fields, "banner_location.key")));
		badgeObj.put("id", id);
		badgeObj.put("images", getImageIdsFromFields(pgConn, fields));
		badgeObj.put("showLocation", CommonUtils.mapLocation(getNestedKey(fields, "show_card_location.key")));
		badgeObj.put("title", getNestedKey(fields, "display_title.text"));
		badgeObj.put("type", getNestedKey(fields, "layout.key"));

		CommonUtils.dynamoDataCreator(dC, "badges", key, badgeObj.toString());
		
	}

	private static void handleGenre(Connection pgConn, DynamoDbClient dC, String key, long id, JSONObject fields)
			throws SQLException {
		JSONObject genreObj = new JSONObject();
		genreObj.put("contentType", "genre");
		genreObj.put("id", id);
		genreObj.put("images", getImageIdsFromFields(pgConn, fields));
		genreObj.put("title", getNestedKey(fields, "display_title.text"));

		CommonUtils.dynamoDataCreator(dC, "genres", key, genreObj.toString());
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
