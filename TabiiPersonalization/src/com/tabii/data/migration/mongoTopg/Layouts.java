package com.tabii.data.migration.mongoTopg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.MongoProperties;
import com.tabii.utils.PgProperties;



public class Layouts {

    // Recursive method to find all "layout" nodes
    private static void findLayoutNodes(Document doc, List<Document> results) {
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if ("layout".equals(key) && value instanceof Document) {
                results.add((Document) value);
            }

            if (value instanceof Document) {
                findLayoutNodes((Document) value, results);
            } else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof Document) {
                        findLayoutNodes((Document) item, results);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Load connection properties from internal utils
            PgProperties pgProperties = CommonUtils.getPgConnectionProps();
            MongoProperties mongoProperties = CommonUtils.getMongoConnectionProps();
            String mongoCollection = "lookup";

            try (MongoClient mongoClient = MongoClients.create(mongoProperties.getMongoUri());
                 Connection pgConn = DriverManager.getConnection(
                         pgProperties.getDbUrl(),
                         pgProperties.getDbUser(),
                         pgProperties.getDbPassword())) {

                MongoDatabase db = mongoClient.getDatabase(mongoProperties.getMongoDb());
                MongoCollection<Document> collection = db.getCollection(mongoCollection);

                FindIterable<Document> docs = collection.find();

                String insertSQL = "INSERT INTO layouts (key, value, metadata) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = pgConn.prepareStatement(insertSQL)) {

                    for (Document doc : docs) {
                        List<Document> layouts = new ArrayList<>();
                        findLayoutNodes(doc, layouts);

                        for (Document layout : layouts) {
                            String key = layout.getString("key");
                            String value = layout.getString("value");

                            Object metadataObj = layout.get("metadata");
                            String metadata = (metadataObj != null) ? metadataObj.toString() : null;

                            // insert only supported columns
                            pstmt.setString(1, key);
                            pstmt.setString(2, value);
                            pstmt.setString(3, metadata);
                            pstmt.addBatch();

                            // log extra fields
                            for (String fieldName : layout.keySet()) {
                                if (!fieldName.equals("key") &&
                                    !fieldName.equals("value") &&
                                    !fieldName.equals("metadata")) {
                                    System.out.printf("⚠️ Extra attribute in layout skipped: %s = %s%n",
                                            fieldName, layout.get(fieldName));
                                }
                            }
                        }

                    }

                    pstmt.executeBatch();
                }

                System.out.println("Layouts inserted into Postgres successfully.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
