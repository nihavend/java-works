package com.tabii.data.transformers.mongoToPg;

import static java.util.Map.entry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

public class MongoToPostgresShowLookupImagesExporter {

   
    public  static final Map<String, String> imageMap = Map.ofEntries(
    		entry("main_image", "main"),
    	    entry("main_image_with_logo", "mainWithLogo"),
    	    entry("cover_image", "cover"),
    	    entry("mobile_cover_image", "mobileCover"),
    	    entry("vertical_image", "vertical"),
    	    entry("vertical_image_with_logo", "verticalWithLogo"),
    	    entry("show_logo", "showLogo"),
    	    entry("promo_image", "promoterImage"),
    	    entry("badge_image", "background"),
    	    entry("background", "background")
    	    
    	);

    public static void main(String[] args) {
    	migrate();
    }
    
    public static void migrate() {
    	
    	getShowImages();
    	getLookupImages();
    }
    
    private static void getShowImages() {
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

//                    if(fields.toJson().toString().contains("26087_0-0-144-144.jpeg")) {
//                    	System.out.println("Bingo....");
//                    }
                    
                    for (String key : imageMap.keySet()) {
                        Document imageDoc = fields.get(key, Document.class);
                        if (imageDoc != null) {
                            // String type = imageDoc.getString("type");
                            String fileName = imageDoc.getString("fileName");

                            
                            String title = imageDoc.getString("title");

                            if (fileName != null) { // filename is unique, skip nulls
                                ps.setString(1, imageMap.get(key));
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
    
    private static void getLookupImages() {
    	PgProperties pgProperties = CommonUtils.getPgConnectionProps();
    	MongoProperties mongoProperties = CommonUtils.getMongoConnectionProps();
    	String mongoCollection = "lookup"; // Mongo collection
    	
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
                    
                    for (String key : imageMap.keySet()) {
                        Document imageDoc = fields.get(key, Document.class);
                        if (imageDoc != null) {
                            // String type = imageDoc.getString("type");
                            String fileName = imageDoc.getString("fileName");

                            
                            String title = imageDoc.getString("title");

                            if (fileName != null) { // filename is unique, skip nulls
                                ps.setString(1, imageMap.get(key));
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
