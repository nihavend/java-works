package com.tabii.data.transformers.mongoToPg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.MongoProperties;
import com.tabii.utils.PgProperties;

public class MongoToPostgresImages {

    private static final List<String> IMAGE_KEYS = Arrays.asList(
            "main_image", "main_image_with_logo", "cover_image",
            "mobile_cover_image", "vertical_image", "vertical_image_with_logo",
            "show_logo", "promo_image"
    );

    public static void main(String[] args) {
        
        
		MongoProperties mongoProperties = CommonUtils.getMongoConnectionProps();
		String mongoCollection = "lookup"; // Mongo collection

		PgProperties pgProperties = CommonUtils.getPgConnectionProps();
        

        try (
                MongoClient mongoClient = MongoClients.create(mongoProperties.getMongoUri());
                Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
						pgProperties.getDbPassword())
        ) {
            MongoDatabase db = mongoClient.getDatabase("mydb");
            MongoCollection<Document> shows = db.getCollection("shows");

            // Prepare PG statements
            PreparedStatement insertImage = pgConn.prepareStatement(
                    "INSERT INTO images (image_type, filename, title) VALUES (?, ?, ?) " +
                            "ON CONFLICT (filename) DO UPDATE SET image_type = EXCLUDED.image_type, title = EXCLUDED.title RETURNING id"
            );

            PreparedStatement insertContentImage = pgConn.prepareStatement(
                    "INSERT INTO content_images (content_id, image_id) VALUES (?, ?) " +
                            "ON CONFLICT DO NOTHING"
            );

            for (Document show : shows.find()) {
                long contentId = show.getLong("_id");
                Document fields = show.get("fields", Document.class);

                if (fields == null) continue;

                for (String key : IMAGE_KEYS) {
                    if (fields.containsKey(key)) {
                        Document imageDoc = fields.get(key, Document.class);
                        if (imageDoc == null) continue;

                        String type = imageDoc.getString("type");
                        String filename = imageDoc.getString("fileName");
                        String title = imageDoc.getString("title");

                        if (filename == null) continue; // filename is required

                        // Insert into images
                        insertImage.setString(1, type);
                        insertImage.setString(2, filename);
                        insertImage.setString(3, title);
                        ResultSet rs = insertImage.executeQuery();
                        int imageId = -1;
                        if (rs.next()) {
                            imageId = rs.getInt(1);
                        }
                        rs.close();

                        if (imageId != -1) {
                            // Insert into relation table
                            insertContentImage.setLong(1, contentId);
                            insertContentImage.setInt(2, imageId);
                            insertContentImage.executeUpdate();
                        }
                    }
                }
            }

            System.out.println("✅ Export completed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
