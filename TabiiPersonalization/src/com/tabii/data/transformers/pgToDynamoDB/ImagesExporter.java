package com.tabii.data.transformers.pgToDynamoDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONObject;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.DynamoDBProperties;
import com.tabii.utils.PgProperties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

public class ImagesExporter {

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

		String dynamoTableName = "images";
		
		// --- Export data ---
		try (Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
				pgProperties.getDbPassword())) {
			
			String sql = "SELECT id, image_type, filename, title FROM images";

			try (PreparedStatement ps = pgConn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int id = rs.getInt("id");
					String redisKey = "image:" + id;

					// Build JSON value
					JSONObject value = new JSONObject();

					value.put("id", id);
					value.put("contentType", "image"); // fixed value
					value.put("imageType", rs.getString("image_type"));
					value.put("name", rs.getString("filename"));
					value.put("title", rs.getString("title"));

					PutItemResponse pr = CommonUtils.dynamoDataCreator(dynamoClient, dynamoTableName, redisKey, value.toString());
					
					System.out.println("PutResponse :" + pr.toString() + " Inserted: " + id + " -> " + value);
				}
			}
			System.out.println("✅ Data export completed successfully!");

		} catch (SQLException e) {
			e.printStackTrace();
		}

		dynamoClient.close();
	}

}
