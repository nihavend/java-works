package com.tabii.data.transformers.mongoToPg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.MongoProperties;
import com.tabii.utils.PgProperties;

public class MongoToPostgresShowImagesExporter {


    // Image keys to extract from Mongo
    private static final List<String> IMAGE_KEYS = Arrays.asList(
            "main_image", "main_image_with_logo", "cover_image",
            "mobile_cover_image", "vertical_image", "vertical_image_with_logo",
            "show_logo", "promo_image", "badge_image"
    );

    public static void main(String[] args) {
    	
    	PgProperties pgProperties = CommonUtils.getPgConnectionProps();
    	MongoProperties mongoProperties = CommonUtils.getMongoConnectionProps();
    	String mongoCollection = "shows"; // Mongo collection
    	
        try (MongoClient mongoClient = MongoClients.create(mongoProperties.getMongoUri());
				Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
						pgProperties.getDbPassword())) {

        	MongoDatabase mongoDb = mongoClient.getDatabase(mongoProperties.getMongoDb());
			MongoCollection<Document> collection = mongoDb.getCollection(mongoCollection);
			
            // Query all documents
            FindIterable<Document> docs = collection.find();

            String insertSQL = "INSERT INTO images (image_type, filename, title) VALUES (?, ?, ?) ON CONFLICT (filename) DO NOTHING";
            try (PreparedStatement ps = pgConn.prepareStatement(insertSQL)) {
                for (Document doc : docs) {
                    Document fields = doc.get("fields", Document.class);
                    if (fields == null) continue;

                    for (String key : IMAGE_KEYS) {
                        Document imageDoc = fields.get(key, Document.class);
                        if (imageDoc != null) {
                            String type = imageDoc.getString("type");
                            String fileName = imageDoc.getString("fileName");
                            String title = imageDoc.getString("title");

                            if (fileName != null) { // filename is unique, skip nulls
                                ps.setString(1, type);
                                ps.setString(2, fileName);
                                ps.setString(3, title);
                                ps.addBatch();
                            }
                        }
                    }
                }
                ps.executeBatch();
                System.out.println("Images exported successfully.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
