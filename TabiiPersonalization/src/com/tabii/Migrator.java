package com.tabii;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.tabii.data.transformers.mongoToPg.MongoToPostgresContentExporter;
import com.tabii.data.transformers.mongoToPg.MongoToPostgresLookups;
import com.tabii.data.transformers.mongoToPg.MongoToPostgresShowLookupImagesExporter;
import com.tabii.data.transformers.pgToRedis.ImagesToRedisExporter;
import com.tabii.data.transformers.pgToRedis.LookupObjectsToRedisExporter;
import com.tabii.data.transformers.pgToRedis.PgContentsToRedisExporter;
import com.tabii.helpers.TableManager;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;
import com.tabii.utils.RedisProperties;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public class Migrator {

	private static final Logger logger = Logger.getLogger("Migrator");

	public static void main(String[] args) throws Exception {

		cleanUp();
		buildUp();
	}

	public static void buildUp() throws Exception {
		mongoTopg();
		pgToredis();
	}

	public static void cleanUp() {
		cleanUpPg();
		cleanRedis();
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
		setDefaultValues();
	}
	
	private static void setDefaultValues() {
		RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();
		try (Jedis jedis = new Jedis(redisProperties.getUrl())) {
			jedis.connect();
			jedis.set("queue:1", "{\"rows\":[\"149015\"]}");
			
			jedis.set("row:149015", 
					"""
					{"rowType":"banner","shows":["191180","155856","154520","524755","154554","155919"]}
					""");

			jedis.set("row:335173", 
			"""
			{"rowType":"show",
				"shows":["551977","11780","190229","550159","535767","535772",
						 "235569","535573","386977","535347","531911","531620",
						 "530824","530718","498557","468020","521469","155034",
						 "432246","447209","447315","434445","6515","380479","408055",
						 "181","398692","429555","407752","164641","240372","189930",
						 "5931","190076","159429","8283","6496","434700","190061"]}			
			""");
			
			jedis.set("row:435386",
			"""
			{"rowType":"show",
				"shows":["524745","522092","436053","436525","420725","436138","436042",
					     "436037","436143","436440","436103","436108","436098","436133",
					     "437198","436128","436032","235569","7095","436022","165295",
					     "409029","161628","231582","308539","436148","436158","436652",
					     "436168","436163","436633","436093","436153","436012","441128"]}
			""");
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

	private static void cleanUpPg() {

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
