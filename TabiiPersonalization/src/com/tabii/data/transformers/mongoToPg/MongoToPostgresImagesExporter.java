package com.tabii.data.transformers.mongoToPg;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

public class MongoToPostgresImagesExporter {

    private static final List<String> IMAGE_KEYS = Arrays.asList(
            "main_image", "main_image_with_logo", "cover_image",
            "mobile_cover_image", "vertical_image", "vertical_image_with_logo",
            "show_logo", "promo_image", "badge_image"
    );

    public static void main(String[] args) {
        String mongoUri = "mongodb://localhost:27017";
        String mongoDbName = "mydb";
        String pgUrl = "jdbc:postgresql://localhost:5432/mydb";
        String pgUser = "postgres";
        String pgPass = "postgres";

        try (MongoClient mongoClient = MongoClients.create(mongoUri);
             Connection pgConn = DriverManager.getConnection(pgUrl, pgUser, pgPass)) {

            MongoDatabase mongoDatabase = mongoClient.getDatabase(mongoDbName);
            MongoCollection<Document> shows = mongoDatabase.getCollection("shows");

            FindIterable<Document> docs = shows.find();
            int count = 0;

            for (Document doc : docs) {
                Document fields = doc.get("fields", Document.class);
                if (fields == null) continue;

                for (String key : IMAGE_KEYS) {
                    if (!fields.containsKey(key)) continue;

                    Object value = fields.get(key);
                    if (!(value instanceof Document)) {
                        System.out.println("⚠️ Skipping non-object field: " + key);
                        continue;
                    }

                    Document imageDoc = (Document) value;
                    String type = imageDoc.getString("type");
                    String fileName = imageDoc.getString("fileName");
                    String title = imageDoc.getString("title");

                    if (fileName == null || fileName.isEmpty()) {
                        System.out.println("⚠️ Missing filename for key " + key);
                        continue;
                    }

                    insertImage(pgConn, type, fileName, title);
                    count++;
                }
            }

            System.out.println("✅ Export completed. Total images inserted: " + count);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertImage(Connection conn, String imageType, String fileName, String title) {
        String sql = "INSERT INTO images (image_type, filename, title) VALUES (?, ?, ?) " +
                     "ON CONFLICT (filename) DO NOTHING";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, imageType);
            ps.setString(2, fileName);
            ps.setString(3, title);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Failed to insert image: " + fileName + " | " + e.getMessage());
        }
    }
}
