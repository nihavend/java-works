package com.tabii.data.transformers.mongoToPg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.MongoProperties;
import com.tabii.utils.PgProperties;

public class MongoToPostgresLookups {

	public static void main(String[] args) {
		migrate();
	}
	
	public static void migrate() {

		MongoProperties mongoProperties = CommonUtils.getMongoConnectionProps();
		String mongoCollection = "lookup"; // Mongo collection

		PgProperties pgProperties = CommonUtils.getPgConnectionProps();

		try (MongoClient mongoClient = MongoClients.create(mongoProperties.getMongoUri());
				Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
						pgProperties.getDbPassword())) {

			MongoDatabase mongoDb = mongoClient.getDatabase(mongoProperties.getMongoDb());
			MongoCollection<Document> collection = mongoDb.getCollection(mongoCollection);

			// Prepare insert statement
			String insertSQL = "INSERT INTO lookup_objects "
					+ "(id, type, audit, body, fields, isactive, metadata, path, paths, published, site, title, viewcount, audit_user_id) "
					+ "VALUES (?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?::jsonb, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?)";

			PreparedStatement stmt = pgConn.prepareStatement(insertSQL);

			// Read documents
			for (Document doc : collection.find()) {
				String type = doc.getString("type");
				
				Map<String, Object> flat = flattenDocument(doc);
				
				Object typeId =  flat.get("id");
				
				stmt.setObject(1, typeId, Types.BIGINT);
				stmt.setString(2, type);
				stmt.setString(3, asJson(flat.get("audit")));
				stmt.setString(4, asJson(flat.get("body")));
				stmt.setString(5, asJson(flat.get("fields")));
				stmt.setObject(6, flat.get("isActive"), Types.BOOLEAN);
				stmt.setString(7, asJson(flat.get("metadata")));
				stmt.setString(8, (String) flat.get("path"));
				stmt.setString(9, asJson(flat.get("paths")));
				stmt.setString(10, asJson(flat.get("published")));
				stmt.setString(11, (String) flat.get("site"));
				stmt.setString(12, (String) flat.get("title"));
				stmt.setObject(13, flat.get("viewCount"), Types.BIGINT);
				stmt.setObject(14, flat.get("audit_user_id"), Types.BIGINT);

				stmt.addBatch();
			}

			stmt.executeBatch();
			System.out.println("Data inserted successfully.");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Flatten the document: handle audit.user -> audit_user_id, other nested
	// objects stay as JSON
	private static Map<String, Object> flattenDocument(Document doc) {
		Map<String, Object> map = new HashMap<>();
		for (Map.Entry<String, Object> entry : doc.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if ("type".equals(key))
				continue;

			if ("audit".equals(key) && value instanceof Document) {
				Document auditDoc = (Document) value;
				// Extract audit_user_id if user exists
				if (auditDoc.containsKey("created")) {
					Document created = auditDoc.get("created", Document.class);
					if (created != null && created.containsKey("user")) {
						Document user = created.get("user", Document.class);
						if (user != null && user.containsKey("_id")) {
							map.put("audit_user_id", user.getLong("_id"));
						}
					}
				}
				map.put("audit", auditDoc); // store entire audit as JSON
			} else if (value instanceof Document || value instanceof List) {
				map.put(key, value); // JSON columns
			} else {
				map.put(key, value); // primitive fields
			}
		}
		return map;
	}

	private static String asJson(Object obj) {
		if (obj == null)
			return null;
		try {
			if (obj instanceof Document) {
				return ((Document) obj).toJson(JsonWriterSettings.builder().build());
			} else if (obj instanceof List) {
				ObjectMapper mapper = new ObjectMapper();
				return mapper.writeValueAsString(obj); // proper JSON
			} else {
				return obj.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
