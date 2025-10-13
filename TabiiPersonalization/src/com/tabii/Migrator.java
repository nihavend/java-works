package com.tabii;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.tabii.data.transformers.mongoToPg.MongoToPostgresLookups;
import com.tabii.data.transformers.mongoToPg.MongoToPostgresShowLookupImagesExporter;
import com.tabii.data.transformers.pgToRedis.ImagesToRedisExporter;
import com.tabii.data.transformers.pgToRedis.PgContentsToRedisExporter;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;
import com.tabii.utils.RedisProperties;

import old.MongoToPostgresContentExporter_yeni;
import old.PgLookupsToRedisExporter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public class Migrator {

	private static final Logger logger = Logger.getLogger("Migrator");

	public static void main(String[] args) throws Exception {

		cleanUp();
		// buildUp();
	}
	
	public static void buildUp() throws Exception {
		// mongoTopg();
		pgToredis();
	}
	
	public static void cleanUp() {
		// truncateTables();
		cleanRedis();		
	}

	private static void mongoTopg() throws Exception {
		MongoToPostgresLookups.migrate();
		MongoToPostgresShowLookupImagesExporter.migrate();
		MongoToPostgresContentExporter_yeni.migrate();
	}

	private static void pgToredis() {
		ImagesToRedisExporter.migrate();
		PgLookupsToRedisExporter.migrate();
		PgContentsToRedisExporter.migrate();
	}

    public static void cleanRedis() {
    	
    	// Redis connection settings
    	RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();
    	
        // Example usage
        List<String> prefixes = Arrays.asList("show", "badge", "badges", "category", "exclusive-badge", "genre", "image", "parental-guide", "age-restriction");

        // Redis connection
        try (Jedis jedis = new Jedis(redisProperties.getHost(), redisProperties.getPort())) {
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
	
	private static void truncateTables() {

		// The table to truncate
		List<String> tableNameList = Arrays.asList("lookup_objects", "content_lookup_relations", "images",
				"contents" /* , "content_images", "content_genres", "content_badges" */);

		PgProperties pgProperties = CommonUtils.getPgConnectionProps();

		try (Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
				pgProperties.getDbPassword()); Statement stmt = pgConn.createStatement()) {

			for (String tableNames : tableNameList) {
				// SQL to truncate with cascade and reset identity
				String sql = "TRUNCATE TABLE " + tableNames + " RESTART IDENTITY CASCADE;";

				stmt.executeUpdate(sql);
				System.out
						.println("Table '" + tableNames + "' truncated successfully with identity reset and cascade.");

			}

		} catch (SQLException e) {
			System.err.println("Error truncating table: " + e.getMessage());
			logger.severe("Export failed: " + e.getMessage());
		}
	}
	
}
