package com.tabii.data.transformers.mongoToPg;

import com.mongodb.client.*;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.MongoProperties;
import org.bson.Document;

import java.util.*;

public class MongoToRDBSchemaGenerator {

	public static void main(String[] args) {
		try {
			MongoProperties mongoProperties = CommonUtils.getMongoConnectionProps();
			String mongoCollection = "lookup";

			MongoClient mongoClient = MongoClients.create(mongoProperties.getMongoUri());
			MongoDatabase mongoDb = mongoClient.getDatabase(mongoProperties.getMongoDb());
			MongoCollection<Document> collection = mongoDb.getCollection(mongoCollection);

			List<Document> sampleDocs = collection.find().limit(1000).into(new ArrayList<>());

			Map<String, Map<String, String>> typeFieldMap = new LinkedHashMap<>();

			for (Document doc : sampleDocs) {
				String type = doc.getString("type");
				if (type == null)
					continue;

				Map<String, String> fields = typeFieldMap.computeIfAbsent(type, k -> new LinkedHashMap<>());
				flattenDocument("", doc, fields);
			}

			// Generate CREATE TABLE
			for (Map.Entry<String, Map<String, String>> typeEntry : typeFieldMap.entrySet()) {
				String typeName = typeEntry.getKey().replaceAll("\\s+", "_").toLowerCase();
				Map<String, String> fields = typeEntry.getValue();

				System.out.println("CREATE TABLE " + typeName + " (");
				List<String> columns = new ArrayList<>();
				columns.add("    id SERIAL PRIMARY KEY");
				for (Map.Entry<String, String> fieldEntry : fields.entrySet()) {
					columns.add("    " + fieldEntry.getKey() + " " + fieldEntry.getValue());
				}
				System.out.println(String.join(",\n", columns));
				System.out.println(");\n");
			}

			mongoClient.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void flattenDocument(String prefix, Document doc, Map<String, String> fields) {
	    for (Map.Entry<String, Object> entry : doc.entrySet()) {
	        String key = entry.getKey();
	        Object value = entry.getValue();
	        String columnName = prefix.isEmpty() ? key : prefix + "_" + key;

	        if ("type".equals(key)) continue;

	        // Handle user/image references
	        if ((prefix.equals("audit_created") || prefix.equals("audit_modified")) && key.equals("user")) {
	            fields.putIfAbsent("audit_user_id", "BIGINT");
	            continue; // skip all inner fields
	        }

	        if (value instanceof Document) {
	            // preserve as JSON if not a reference
	            fields.putIfAbsent(columnName, "JSON");
	        } else if (value instanceof List) {
	            // arrays as JSON
	            fields.putIfAbsent(columnName, "JSON");
	        } else {
	            fields.putIfAbsent(columnName, inferSqlType(value));
	        }
	    }
	}

	private static String inferSqlType(Object value) {
		if (value == null)
			return "TEXT";
		if (value instanceof Integer || value instanceof Short || value instanceof Byte)
			return "INT";
		if (value instanceof Long)
			return "BIGINT";
		if (value instanceof Double || value instanceof Float)
			return "DOUBLE";
		if (value instanceof Boolean)
			return "BOOLEAN";
		if (value instanceof Date)
			return "TIMESTAMP";
		return "TEXT";
	}
}
