package com.tabii;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.tabii.data.transformers.mongoToPg.MongoToPostgresContentExporter;
import com.tabii.data.transformers.mongoToPg.MongoToPostgresLookups;
import com.tabii.data.transformers.mongoToPg.MongoToPostgresShowLookupImagesExporter;
import com.tabii.data.transformers.pgToMemcached.ContentsExporter;
import com.tabii.data.transformers.pgToMemcached.ImagesExporter;
import com.tabii.data.transformers.pgToMemcached.LookupsExporter;
import com.tabii.data.transformers.pgToRedis.ImagesToRedisExporter;
import com.tabii.data.transformers.pgToRedis.LookupObjectsToRedisExporter;
import com.tabii.data.transformers.pgToRedis.PgContentsToRedisExporter;
import com.tabii.helpers.DefaultsHolder;
import com.tabii.helpers.TableManager;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.DynamoDBProperties;
import com.tabii.utils.HazelcastProperties;
import com.tabii.utils.MemcachedProperties;
import com.tabii.utils.PgProperties;
import com.tabii.utils.RedisProperties;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class Migrator {

	private static final Logger logger = Logger.getLogger("Migrator");

	enum StorageType {
		POSTGRES, REDIS, MEMCACHED, HAZELCAST, DYNAMODB, ALL
	}
	// List of table names to delete
	private static List<String> tableNames = Arrays.asList("shows", "badges", "genres", "images", "queues", "rows", "exclusive-badges");

	
	public static final List<String> maps = Arrays.asList("imagesMap", "exclusiveBadgesMap", "badgesMap", "genreMap",
			"showsMap");

	public static void main(String[] args) throws Exception {
		
		var key = StorageType.DYNAMODB;
		
		switch (key) {
		case DYNAMODB:
			cleanDynamoDB();
			pgToDynamoDb();
			break;
		case HAZELCAST:
			cleanHazelCast();	
			pgToHazelcast();
			break;
		case MEMCACHED:
			cleanMemcached();
			pgToMemecached();
			break;
		case POSTGRES:
			cleanPg();
			mongoTopg();
			break;
		case REDIS:
			cleanRedis();
			pgToredis();
			break;
		case ALL:
			cleanUp();
			buildUp();
			break;
		default:
			break;
		}

	}

	public static void cleanUp() {
		cleanPg();
		cleanRedis();
		cleanMemcached();
		cleanHazelCast();
		cleanDynamoDB();
	}

	public static void buildUp() throws Exception {
		mongoTopg();
		pgToredis();
		pgToMemecached();
		pgToHazelcast();
		pgToDynamoDb();
	}

	private static void mongoTopg() throws Exception {
		MongoToPostgresShowLookupImagesExporter.migrate();
		MongoToPostgresLookups.migrate();
		MongoToPostgresContentExporter.migrate();
	}

	private static void pgToredis() {
		ImagesToRedisExporter.migrate();
		LookupObjectsToRedisExporter.migrate();
		PgContentsToRedisExporter.migrate();
		setRedisDefaultValues();
	}

	private static void pgToMemecached() {
		ImagesExporter.migrate();
		LookupsExporter.migrate();
		ContentsExporter.migrate();
		setMemcachedDefaultValues();
	}

	private static void pgToHazelcast() {
		com.tabii.data.transformers.pgToHazelcast.ImagesExporter.migrate();
		com.tabii.data.transformers.pgToHazelcast.LookupsExporter.migrate();
		com.tabii.data.transformers.pgToHazelcast.ContentsExporter.migrate();
		setHazelcastDefaultValues();
	}
	
	private static void pgToDynamoDb() {
		DynamoDbClient dynamoDbClient = CommonUtils.createDynamoDbClient();
		CommonUtils.createDynamoDbTables(dynamoDbClient, tableNames);
		dynamoDbClient.close();

		setDynamoDBDefaultValues();
		
		com.tabii.data.transformers.pgToDynamoDB.ImagesExporter.migrate();
		com.tabii.data.transformers.pgToDynamoDB.LookupsExporter.migrate();
		com.tabii.data.transformers.pgToDynamoDB.ContentsExporter.migrate();
	}

	private static void setRedisDefaultValues() {
		RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();
		try (Jedis jedis = new Jedis(redisProperties.getUrl())) {
			jedis.connect();

			String res = jedis.set(DefaultsHolder.defaultQueue[0], DefaultsHolder.defaultQueue[1]);
			System.out.println(res);
			res = jedis.set(DefaultsHolder.defaultQueue[2], DefaultsHolder.defaultQueue[3]);
			System.out.println(res);
			res = jedis.set(DefaultsHolder.defaultQueue[4], DefaultsHolder.defaultQueue[5]);
			System.out.println(res);

			res = jedis.set(DefaultsHolder.defaultRows[0], DefaultsHolder.defaultRows[1]);
			System.out.println(res);
			res = jedis.set(DefaultsHolder.defaultRows[2], DefaultsHolder.defaultRows[3]);
			System.out.println(res);
			res = jedis.set(DefaultsHolder.defaultRows[4], DefaultsHolder.defaultRows[5]);
			System.out.println(res);

			System.out.println(DefaultsHolder.defaultQueue[0] + " " + jedis.get(DefaultsHolder.defaultQueue[0]));
			System.out.println(DefaultsHolder.defaultQueue[2] + " " + jedis.get(DefaultsHolder.defaultQueue[2]));
			System.out.println(DefaultsHolder.defaultQueue[4] + " " + jedis.get(DefaultsHolder.defaultQueue[4]));

			System.out.println(DefaultsHolder.defaultRows[0] + " " + jedis.get(DefaultsHolder.defaultRows[0]));
			System.out.println(DefaultsHolder.defaultRows[2] + " " + jedis.get(DefaultsHolder.defaultRows[2]));
			System.out.println(DefaultsHolder.defaultRows[4] + " " + jedis.get(DefaultsHolder.defaultRows[4]));

		}
	}

	private static void setMemcachedDefaultValues() {
		MemcachedProperties memcachedProperties = CommonUtils.getMemcachedConnectionProps();
		MemcachedClient mc = null;

		try {

			mc = new MemcachedClient(CommonUtils.getServers(memcachedProperties.getServers()));

			OperationFuture<Boolean> b = mc.set(DefaultsHolder.defaultQueue[0], 360000, DefaultsHolder.defaultQueue[1]);
			System.out.println(b.isDone());
			b = mc.set(DefaultsHolder.defaultQueue[2], 360000, DefaultsHolder.defaultQueue[3]);
			System.out.println(b.isDone());
			b = mc.set(DefaultsHolder.defaultQueue[4], 360000, DefaultsHolder.defaultQueue[5]);
			System.out.println(b.isDone());
			b = mc.set(DefaultsHolder.defaultRows[0], 360000, DefaultsHolder.defaultRows[1]);
			System.out.println(b.isDone());
			b = mc.set(DefaultsHolder.defaultRows[2], 360000, DefaultsHolder.defaultRows[3]);
			System.out.println(b.isDone());
			b = mc.set(DefaultsHolder.defaultRows[4], 360000, DefaultsHolder.defaultRows[5]);

			System.out.println(DefaultsHolder.defaultQueue[0] + " " + mc.get(DefaultsHolder.defaultQueue[0]));
			System.out.println(DefaultsHolder.defaultQueue[2] + " " + mc.get(DefaultsHolder.defaultQueue[2]));
			System.out.println(DefaultsHolder.defaultQueue[4] + " " + mc.get(DefaultsHolder.defaultQueue[4]));

			System.out.println(DefaultsHolder.defaultRows[0] + " " + mc.get(DefaultsHolder.defaultRows[0]));
			System.out.println(DefaultsHolder.defaultRows[2] + " " + mc.get(DefaultsHolder.defaultRows[2]));
			System.out.println(DefaultsHolder.defaultRows[4] + " " + mc.get(DefaultsHolder.defaultRows[4]));

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (mc != null) {
				mc.shutdown();
			}
		}
	}

	private static void setHazelcastDefaultValues() {

		HazelcastProperties hzp = CommonUtils.getHazelcastConnectionProps();
		ClientConfig config = new ClientConfig();
		config.setClusterName(hzp.getClusterName());
		config.getNetworkConfig().setAddresses(CommonUtils.serversSplitter(hzp.getServers()));
		HazelcastInstance hzi = null;

		try {

			hzi = HazelcastClient.newHazelcastClient(config);

			IMap<String, String> qmap = hzi.getMap("queuesMap");
			qmap.put(DefaultsHolder.defaultQueue[0], DefaultsHolder.defaultQueue[1]);
			qmap.put(DefaultsHolder.defaultQueue[2], DefaultsHolder.defaultQueue[3]);
			qmap.put(DefaultsHolder.defaultQueue[4], DefaultsHolder.defaultQueue[5]);

			IMap<String, String> rmap = hzi.getMap("rowsMap");
			rmap.put(DefaultsHolder.defaultRows[0], DefaultsHolder.defaultRows[1]);
			rmap.put(DefaultsHolder.defaultRows[2], DefaultsHolder.defaultRows[3]);
			rmap.put(DefaultsHolder.defaultRows[4], DefaultsHolder.defaultRows[5]);

			System.out.println(DefaultsHolder.defaultQueue[0] + " " + qmap.get(DefaultsHolder.defaultQueue[0]));
			System.out.println(DefaultsHolder.defaultQueue[2] + " " + qmap.get(DefaultsHolder.defaultQueue[2]));
			System.out.println(DefaultsHolder.defaultQueue[4] + " " + qmap.get(DefaultsHolder.defaultQueue[4]));

			System.out.println(DefaultsHolder.defaultRows[0] + " " + rmap.get(DefaultsHolder.defaultRows[0]));
			System.out.println(DefaultsHolder.defaultRows[2] + " " + rmap.get(DefaultsHolder.defaultRows[2]));
			System.out.println(DefaultsHolder.defaultRows[4] + " " + rmap.get(DefaultsHolder.defaultRows[4]));

			Thread.sleep(5000);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (hzi != null) {
				hzi.shutdown();
			}
		}
	}

	private static void setDynamoDBDefaultValues() {
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

		try {

			CommonUtils.dynamoDataCreator(dc, "queues", DefaultsHolder.defaultQueue[0], DefaultsHolder.defaultQueue[1]);

			CommonUtils.dynamoDataCreator(dc, "queues", DefaultsHolder.defaultQueue[2], DefaultsHolder.defaultQueue[3]);

			CommonUtils.dynamoDataCreator(dc, "queues", DefaultsHolder.defaultQueue[4], DefaultsHolder.defaultQueue[5]);

			CommonUtils.dynamoDataCreator(dc, "rows", DefaultsHolder.defaultRows[0], DefaultsHolder.defaultRows[1]);
			CommonUtils.dynamoDataCreator(dc, "rows", DefaultsHolder.defaultRows[2], DefaultsHolder.defaultRows[3]);
			CommonUtils.dynamoDataCreator(dc, "rows", DefaultsHolder.defaultRows[4], DefaultsHolder.defaultRows[5]);

			System.out.println(DefaultsHolder.defaultQueue[0] + " "
					+ CommonUtils.dynamoDbGetById(dc, "queues", DefaultsHolder.defaultQueue[0]).getDescription());
			System.out.println(DefaultsHolder.defaultQueue[2] + " "
					+ CommonUtils.dynamoDbGetById(dc, "queues", DefaultsHolder.defaultQueue[2]).getDescription());
			System.out.println(DefaultsHolder.defaultQueue[4] + " "
					+ CommonUtils.dynamoDbGetById(dc, "queues", DefaultsHolder.defaultQueue[4]).getDescription());

			System.out.println(DefaultsHolder.defaultRows[0] + " "
					+ CommonUtils.dynamoDbGetById(dc, "rows", DefaultsHolder.defaultRows[0]).getDescription());
			System.out.println(DefaultsHolder.defaultRows[2] + " "
					+ CommonUtils.dynamoDbGetById(dc, "rows", DefaultsHolder.defaultRows[2]).getDescription());
			System.out.println(DefaultsHolder.defaultRows[4] + " "
					+ CommonUtils.dynamoDbGetById(dc, "rows", DefaultsHolder.defaultRows[4]).getDescription());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void cleanRedis() {

		// Redis connection settings
		RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();

		// Example usage
		List<String> prefixes = Arrays.asList("show", "badge", "badges", "category", "exclusive-badge", "genre",
				"image", "parental-guide", "age-restriction");

		// Redis connection
		try (Jedis jedis = new Jedis(redisProperties.getUrl())) {
			jedis.connect();
			deleteKeysByPrefixes(jedis, prefixes);
		}
	}

	public static void cleanMemcached() {

		// Redis connection settings
		RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();

		// Example usage
		List<String> prefixes = Arrays.asList("show", "badge", "badges", "category", "exclusive-badge", "genre",
				"image", "parental-guide", "age-restriction");

		// Redis connection
		try (Jedis jedis = new Jedis(redisProperties.getUrl())) {
			jedis.connect();
			deleteKeysByPrefixes(jedis, prefixes);
		}
	}

	public static void cleanHazelCast() {

		HazelcastProperties hzp = CommonUtils.getHazelcastConnectionProps();
		ClientConfig config = new ClientConfig();
		config.setClusterName(hzp.getClusterName());
		config.getNetworkConfig().setAddresses(CommonUtils.serversSplitter(hzp.getServers()));
		HazelcastInstance hzi = HazelcastClient.newHazelcastClient(config);

		// Example usage

		for (String map : maps) {
			hzi.getMap(map).destroy();
			System.out.println("✅ Map destroyed in Hazelcast: " + map);
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		hzi.shutdown();
	}

	private static void cleanDynamoDB() {

		DynamoDbClient dynamoDbClient = CommonUtils.createDynamoDbClient();
		CommonUtils.deleteAllTables(dynamoDbClient, tableNames);
		dynamoDbClient.close();
	}

	private static void deleteKeysByPrefixes(Jedis jedis, List<String> prefixes) {
		for (String prefix : prefixes) {
			String pattern = prefix + "*";
			System.out.println("Deleting keys with pattern: " + pattern);

			Set<String> keysToDelete = new HashSet<>();
			String cursor = ScanParams.SCAN_POINTER_START;
			ScanParams params = new ScanParams().match(pattern).count(1000);

			do {
				ScanResult<String> scanResult = jedis.scan(cursor, params);
				keysToDelete.addAll(scanResult.getResult());
				cursor = scanResult.getCursor();
			} while (!cursor.equals(ScanParams.SCAN_POINTER_START));

			if (!keysToDelete.isEmpty()) {
				jedis.del(keysToDelete.toArray(new String[0]));
				System.out.println("Deleted " + keysToDelete.size() + " keys for prefix: " + prefix);
			} else {
				System.out.println("No keys found for prefix: " + prefix);
			}
		}
	}

	private static void cleanPg() {

		PgProperties pgProperties = CommonUtils.getPgConnectionProps();

		try (Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
				pgProperties.getDbPassword()); Statement stmt = pgConn.createStatement()) {

			for (String tableName : TableManager.tableNameList) {
				if (tableExists(pgConn, tableName)) {
					System.out.println("Table exists: " + tableName + " — truncating...");
					truncateTable(pgConn, tableName);
				} else {
					System.out.println("Table does not exist: " + tableName + " — creating...");
					createTable(pgConn, tableName);
				}
			}

			System.out.println("CleanUp completed !");

		} catch (SQLException e) {
			System.err.println("Error truncating table: " + e.getMessage());
			logger.severe("Export failed: " + e.getMessage());
		}
	}

	// Check if table exists in public schema
	private static boolean tableExists(Connection conn, String tableName) throws SQLException {
		String query = "SELECT EXISTS (" + "SELECT 1 FROM information_schema.tables "
				+ "WHERE table_schema = 'public' AND table_name = ?" + ")";
		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setString(1, tableName);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getBoolean(1);
			}
		}
		return false;
	}

	// Truncate the table with CASCADE
	private static void truncateTable(Connection conn, String tableName) throws SQLException {
		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate("TRUNCATE TABLE " + tableName + " CASCADE");
		}
	}

	// Create the table (simple structure, adjust per table name)
	private static void createTable(Connection conn, String tableName) throws SQLException {
		String createSQL = switch (tableName) {
		case "users_id_seq" -> TableManager.create_users_id_seq();
		case "alter_users_id_seq" -> TableManager.alter_users_id_seq();
		case "users" -> TableManager.create_users();
		case "images" -> TableManager.create_images();
		case "images_id_seq" -> TableManager.create_images_id_seq();
		case "content_images" -> TableManager.create_content_images();
		case "alter_images_id_seq" -> TableManager.alter_images_id_seq();
		case "lookup_objects" -> TableManager.create_lookup_objects();
		case "contents" -> TableManager.create_contents();
		case "content_lookup_relations_id_seq" -> TableManager.create_content_lookup_relations_id_seq();
		case "content_lookup_relations" -> TableManager.create_content_lookup_relations();
		case "alter_content_lookup_relations_id_seq" -> TableManager.alter_content_lookup_relations_id_seq();
		default -> throw new IllegalArgumentException("Unknown table: " + tableName);
		};

		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate(createSQL);
		}
	}

}
