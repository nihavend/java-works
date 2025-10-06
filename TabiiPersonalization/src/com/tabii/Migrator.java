package com.tabii;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.tabii.data.transformers.mongoToPg.MongoToPostgresContentExporter;
import com.tabii.data.transformers.mongoToPg.MongoToPostgresLookups;
import com.tabii.data.transformers.mongoToPg.MongoToPostgresShowImagesExporter;
import com.tabii.data.transformers.pgToRedis.ImagesToRedisExporter;
import com.tabii.data.transformers.pgToRedis.PgBadgesToRedisExporter;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;

public class Migrator {

	private static final Logger logger = Logger.getLogger("Migrator");

	public static void main(String[] args) throws Exception {

		truncateTables();

		mongoTopg();

		pgToredis();

	}

	private static void mongoTopg() throws Exception {
		MongoToPostgresLookups.migrate();
		MongoToPostgresShowImagesExporter.migrate();
		MongoToPostgresContentExporter.migrate();
	}

	private static void pgToredis() {
		ImagesToRedisExporter.migrate();
		PgBadgesToRedisExporter.migrate();
		
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
