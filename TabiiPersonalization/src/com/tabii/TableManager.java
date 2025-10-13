package com.tabii;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;

public class TableManager {

	private final Connection connection;

	public TableManager(Connection connection) {
		this.connection = connection;
	}

	public static void main(String[] args) throws Exception {
		PgProperties pgProperties = CommonUtils.getPgConnectionProps();

		try (Connection conn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
				pgProperties.getDbPassword())) {
			TableManager manager = new TableManager(conn);

			Map<String, List<String>> tables = new HashMap<>();
			tables.put("public", List.of("""
					CREATE TABLE public.lookup_objects
					(
					    id SERIAL PRIMARY KEY,
					    type TEXT NOT NULL,          -- e.g. genre, badge, category
					    typeid BIGINT,               -- unique identifier used in Mongo
					    audit JSON,
					    body JSON,
					    fields JSON,
					    isactive BOOLEAN,
					    metadata JSON,
					    path TEXT,
					    paths JSON,
					    published JSON,
					    site TEXT,
					    title TEXT,
					    viewcount BIGINT,
					    audit_user_id BIGINT,
					    CONSTRAINT lookup_objects_audit_user_id_fkey FOREIGN KEY (audit_user_id)
					        REFERENCES public.users (id) MATCH SIMPLE
					        ON UPDATE NO ACTION
					        ON DELETE NO ACTION
					);
					              """, """
					      CREATE TABLE IF NOT EXISTS public.images
					(
					    id SERIAL PRIMARY KEY,
					    image_type TEXT,
					    filename TEXT UNIQUE,
					    title TEXT,
					    url TEXT
					);
					""",

					"""
							CREATE TABLE public.contents
							(
							    id BIGSERIAL PRIMARY KEY,
							    mongo_id TEXT UNIQUE,                  -- from Mongo _id
							    title TEXT,
							    description TEXT,
							    spot TEXT,
							    made_year INTEGER,
							    content_type TEXT,
							    "exclusiveBadges" JSONB,               -- now JSONB for better performance
							    created_at TIMESTAMPTZ DEFAULT NOW(),
							    updated_at TIMESTAMPTZ DEFAULT NOW()
							);
							""",
							"""
							   CREATE TABLE public.content_images (
							   	    content_id BIGINT REFERENCES public.contents(id),
							   	    image_id INT REFERENCES images(id),
							   	    PRIMARY KEY (content_id, image_id)
							   	);
							""",
							"""
							CREATE TABLE public.content_lookup_relations
							(
							    id BIGSERIAL PRIMARY KEY,
							    content_id BIGINT NOT NULL REFERENCES public.contents (id) ON DELETE CASCADE,
							    lookup_object_id BIGINT NOT NULL REFERENCES public.lookup_objects (id) ON DELETE CASCADE,
							    relation_type TEXT NOT NULL,     -- e.g. "genre", "badge", "category"
							    created_at TIMESTAMPTZ DEFAULT NOW()
							);
					"""
							

			));

			manager.recreateTablesFromScripts(tables);
		}
	}

	/**
	 * Drops and recreates tables with dependency detection from CREATE TABLE
	 * scripts
	 */
	public void recreateTablesFromScripts(Map<String, List<String>> tableSchemas) throws SQLException {
		connection.setAutoCommit(false);
		try {
			// 1. Detect dependencies from scripts
			Map<String, Set<String>> dependencies = detectDependencies(tableSchemas);

			// 2. Determine drop and create order
			List<String> dropOrder = sortTablesByDependencies(dependencies);
			List<String> createOrder = new ArrayList<>(dropOrder);
			Collections.reverse(createOrder);

			// 3. Drop tables safely
			for (String tableFullName : dropOrder) {
				System.out.println("Dropping table if exists: " + tableFullName);
				try (Statement stmt = connection.createStatement()) {
					stmt.executeUpdate("DROP TABLE IF EXISTS " + tableFullName + " CASCADE");
				}
			}

			// 4. Create tables in order
			for (String tableFullName : createOrder) {
				String schema = tableFullName.split("\\.")[0];
				// int idx =
				tableSchemas.get(schema).indexOf(tableFullName); // find matching script
				String createSQL = tableSchemas.get(schema).stream()
						.filter(s -> extractFullTableName(s, schema).equals(tableFullName)).findFirst()
						.orElseThrow(() -> new SQLException("Create script not found for " + tableFullName));

				System.out.println("Creating table: " + tableFullName);
				try (Statement stmt = connection.createStatement()) {
					stmt.executeUpdate(createSQL);
				}
			}

			// 5. Verify tables exist
			verifyTablesExist(createOrder);

			connection.commit();
			System.out.println("All tables dropped, created, and verified successfully!");
		} catch (SQLException e) {
			e.printStackTrace();
			connection.rollback();
			System.err.println("Error occurred. Transaction rolled back.");
			throw e;
		} finally {
			connection.setAutoCommit(true);
		}
	}

	/**
	 * Extract dependencies (foreign keys) directly from CREATE TABLE scripts
	 */
	private Map<String, Set<String>> detectDependencies(Map<String, List<String>> tableSchemas) {
		Map<String, Set<String>> dependencies = new HashMap<>();
		Pattern fkPattern = Pattern.compile("REFERENCES\\s+([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)",
				Pattern.CASE_INSENSITIVE);

		for (Map.Entry<String, List<String>> schemaEntry : tableSchemas.entrySet()) {
			String schema = schemaEntry.getKey();
			for (String createSQL : schemaEntry.getValue()) {
				String tableFullName = extractFullTableName(createSQL, schema);
				dependencies.putIfAbsent(tableFullName, new HashSet<>());

				Matcher matcher = fkPattern.matcher(createSQL);
				while (matcher.find()) {
					String depSchema = matcher.group(1);
					String depTable = matcher.group(2);
					dependencies.get(tableFullName).add(depSchema + "." + depTable);
				}
			}
		}
		return dependencies;
	}

	/**
	 * Topological sort to order tables based on dependencies
	 */
	private List<String> sortTablesByDependencies(Map<String, Set<String>> dependencies) throws SQLException {
		List<String> sorted = new ArrayList<>();
		Set<String> visited = new HashSet<>();
		Set<String> visiting = new HashSet<>();

		for (String table : dependencies.keySet()) {
			visit(table, dependencies, visited, visiting, sorted);
		}
		return sorted;
	}

	private void visit(String table, Map<String, Set<String>> dependencies, Set<String> visited, Set<String> visiting,
			List<String> sorted) throws SQLException {
		if (visited.contains(table))
			return;
		if (visiting.contains(table))
			throw new SQLException("Circular dependency detected at " + table);
		visiting.add(table);
		for (String dep : dependencies.getOrDefault(table, Collections.emptySet())) {
			visit(dep, dependencies, visited, visiting, sorted);
		}
		visiting.remove(table);
		visited.add(table);
		sorted.add(table);
	}

	/**
	 * Verify tables exist using pg_catalog.pg_tables (visible inside current
	 * transaction)
	 */
	private void verifyTablesExist(List<String> tableFullNames) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement("SELECT EXISTS ("
				+ "SELECT 1 FROM pg_catalog.pg_tables " + "WHERE schemaname = ? AND tablename = ?)")) {
			for (String fullName : tableFullNames) {
				String[] parts = fullName.split("\\.");
				String schema = parts[0];
				String table = parts[1];
				ps.setString(1, schema);
				ps.setString(2, table);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next() && !rs.getBoolean(1)) {
						throw new SQLException("Table not created: " + fullName);
					}
				}
			}
		}
	}

	/**
	 * Extract full table name including schema from CREATE TABLE statement
	 */
	private String extractFullTableName(String createSQL, String defaultSchema) {
		String upper = createSQL.toUpperCase();
		int idx = upper.indexOf("CREATE TABLE");
		String[] parts = createSQL.substring(idx + 12).trim().split("\\s+|\\(");
		String namePart = parts[0];
		if (!namePart.contains(".")) {
			namePart = defaultSchema + "." + namePart;
		}
		return namePart;
	}

}
