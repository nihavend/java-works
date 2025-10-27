package com.tabii.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.tabii.rest.dynamodb.Item;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

public class CommonUtils {

	public static final List<String> hcMaps = Arrays.asList("imagesMap", "exclusiveBadgesMap", "badgesMap", "genreMap",
			"showsMap");

	private static Properties loadDbProperties() {

		Properties props = new Properties();

		try (InputStream input = new FileInputStream("db.properties")) {
			props.load(input);
		} catch (IOException e) {
			System.err.println("❌ Failed to load db.properties file");
			e.printStackTrace();
			System.exit(1);
		}

		return props;
	}

	public static PgProperties getPgConnectionProps() {

		Properties dbProps = CommonUtils.loadDbProperties();

		String dbUrl = dbProps.getProperty("pg.db.url");
		String dbUser = dbProps.getProperty("pg.db.user");
		String dbPassword = dbProps.getProperty("pg.db.password");
		String datafilepath = dbProps.getProperty("pg.db.datafilepath");

		if (datafilepath != null) {
			return new PgProperties(dbUrl, dbUser, dbPassword, datafilepath);
		}

		return new PgProperties(dbUrl, dbUser, dbPassword);
	}

	public static MongoProperties getMongoConnectionProps() {

		Properties dbProps = CommonUtils.loadDbProperties();

		String pgUser = dbProps.getProperty("mongo.db.url");
		String pgPass = dbProps.getProperty("mongo.db.name");

		return new MongoProperties(pgUser, pgPass);
	}

	public static RedisProperties getRedisConnectionProps() {

		Properties dbProps = CommonUtils.loadDbProperties();

		String url = dbProps.getProperty("redis.url");

		return new RedisProperties(url);
	}

	public static MemcachedProperties getMemcachedConnectionProps() {

		Properties dbProps = CommonUtils.loadDbProperties();

		String url = dbProps.getProperty("memcached.servers");
		int expireSeconds = Integer.parseInt(dbProps.getProperty("memcached.expire.seconds"));

		return new MemcachedProperties(url, expireSeconds);
	}

	public static HazelcastProperties getHazelcastConnectionProps() {

		Properties dbProps = CommonUtils.loadDbProperties();

		String url = dbProps.getProperty("hazelcast.servers");
		String clusterName = dbProps.getProperty("cluster.name");

		return new HazelcastProperties(url, clusterName);
	}

	public static DynamoDBProperties getDynamoDBConnectionProps() {

		Properties dbProps = CommonUtils.loadDbProperties();

		String uri = dbProps.getProperty("dynamodb.uri");
		String accessKey = dbProps.getProperty("dynamodb.access.key");
		String secretKey = dbProps.getProperty("dynamodb.secret.key");

		return new DynamoDBProperties(uri, accessKey, secretKey);
	}

	public static List<InetSocketAddress> getServers(String serversStr) {
		List<InetSocketAddress> serverList = new ArrayList<>();
		for (String server : serversStr.split(",")) {
			String[] parts = server.split(":");
			serverList.add(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
		}

		return serverList;
	}

	public static List<String> serversSplitter(String servers) {
		List<String> addresses = List.of(servers.split(","));

		return addresses;
	}

	public static String mapLocation(String key) {
		switch (key) {
		case "upper_right_corner":
			return "rightTop";
		case "upper_left_corner":
			return "leftTop";
		case "lower_left_corner":
			return "leftBottom";
		case "on_top_of_the_logo":
			return "upLogo";
		case "under_the_logo":
			return "bottomLogo";
		case "do_not_show":
		default:
			return "invisible";
		}
	}

	public static PutItemResponse dynamoDataCreator(DynamoDbClient dynamoClient, String tableName, String k, String v) {
		// Write to DynamoDB
		Map<String, AttributeValue> item = new HashMap<>();

		AttributeValue key = AttributeValue.builder().s(k).build();
		AttributeValue value = AttributeValue.builder().s(v.toString()).build();

		System.out.println("Putting item: key" + " -> " + key.toString());
		System.out.println("Putting item: value" + " -> " + value.toString());

		item.put("PK", key);
		item.put("SK", key);
		item.put("data", value);

		PutItemRequest putReq = PutItemRequest.builder().tableName(tableName).item(item).build();

		PutItemResponse pr = dynamoClient.putItem(putReq);

		return pr;
	}

	public static Item dynamoDbGetById(DynamoDbClient dc, String tableName, String pk) {
		// Build the primary key map (PK + SK)
		Map<String, AttributeValue> key = Map.of("PK", AttributeValue.builder().s(pk).build(), "SK",
				AttributeValue.builder().s(pk).build());

		// Create the GetItem request
		GetItemRequest request = GetItemRequest.builder().tableName(tableName).key(key).build();

		// Fetch item
		GetItemResponse response = dc.getItem(request);

		if (response.hasItem()) {
			Map<String, AttributeValue> item = response.item();
			String pkVal = item.get("PK") != null ? item.get("PK").s() : null;
			String skVal = item.get("SK") != null ? item.get("SK").s() : null;
			String dataVal = item.get("data") != null ? item.get("data").s() : null;

			return new Item(pkVal, skVal, dataVal);
		}

		return null;
	}

	public static DynamoDbClient createDynamoDbClient() {

		DynamoDBProperties dynamoDBProperties = CommonUtils.getDynamoDBConnectionProps();

		// Configure connection pooling and timeouts
		ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder().maxConnections(100) // Default is 50
				.connectionTimeout(Duration.ofSeconds(10)).socketTimeout(Duration.ofSeconds(20))
				.connectionAcquisitionTimeout(Duration.ofSeconds(5));

		DynamoDbClient dc = DynamoDbClient.builder().endpointOverride(URI.create(dynamoDBProperties.getUri())) // DynamoDB
				// local
				.region(Region.US_EAST_1)
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
						.create(dynamoDBProperties.getAccessKey(), dynamoDBProperties.getSecretKey())))
				.httpClientBuilder(httpClientBuilder).build();

		return dc;
	}

	/**
	 * Deletes all tables in the current DynamoDB account (region) using the
	 * provided client.
	 *
	 * @param dynamoDbClient the DynamoDbClient instance
	 */
	public static void deleteAllTables(DynamoDbClient dynamoDbClient, List<String> tableNames) {

		// List all tables
		// ListTablesResponse listTablesResponse = dynamoDbClient.listTables();
		// var tableNames = listTablesResponse.tableNames();

		if (tableNames.isEmpty()) {
			System.out.println("No tables found.");
			return;
		}

		// Delete each table
		for (String tableName : tableNames) {
			System.out.println("Deleting table: " + tableName);

			DeleteTableRequest deleteRequest = DeleteTableRequest.builder().tableName(tableName).build();
			try {
				DeleteTableResponse deleteResponse = dynamoDbClient.deleteTable(deleteRequest);
				System.out.println("Deleted table: " + deleteResponse.tableDescription().tableName());
			} catch (ResourceNotFoundException e) {
				System.err.println("Table already deleted skipping..." + tableName);
			} catch (DynamoDbException e) {
				System.err.println("Error deleting tables: " + e.getMessage());
			}
		}

	}

	/**
	 * Creates DynamoDB tables with a simple key schema (Partition key: "id" as
	 * String).
	 *
	 * @param dynamoDbClient the DynamoDbClient instance
	 * @param tableNames     the list of table names to create
	 */
	public static void createDynamoDbTables(DynamoDbClient dynamoDbClient, List<String> tableNames) {
		for (String tableName : tableNames) {
			try {
				// Check if the table already exists
				ListTablesResponse listResponse = dynamoDbClient.listTables();
				if (listResponse.tableNames().contains(tableName)) {
					System.out.println("Table already exists: " + tableName);
					continue;
				}

				System.out.println("Creating table: " + tableName);

				// Define table schema (simple example)
				CreateTableRequest request = CreateTableRequest.builder().tableName(tableName)
						.keySchema(KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build(),
								KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build())
						.attributeDefinitions(
								AttributeDefinition.builder().attributeName("PK").attributeType(ScalarAttributeType.S)
										.build(),
								AttributeDefinition.builder().attributeName("SK").attributeType(ScalarAttributeType.S)
										.build())
						.provisionedThroughput(
								ProvisionedThroughput.builder().readCapacityUnits(5L).writeCapacityUnits(5L).build())
						.build();

				dynamoDbClient.createTable(request);

				// Wait until table is active
				DynamoDbWaiter dbWaiter = dynamoDbClient.waiter();
				DescribeTableRequest tableRequest = DescribeTableRequest.builder().tableName(tableName).build();

				WaiterResponse<DescribeTableResponse> waiterResponse = dbWaiter.waitUntilTableExists(tableRequest);
				waiterResponse.matched().response()
						.ifPresent(r -> System.out.println("Table created and active: " + r.table().tableName()));

			} catch (DynamoDbException e) {
				System.err.println("Error creating table " + tableName + ": " + e.getMessage());
			}
		}
	}
}
