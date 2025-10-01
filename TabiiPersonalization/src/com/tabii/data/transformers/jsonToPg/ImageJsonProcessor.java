package com.tabii.data.transformers.jsonToPg;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;

public class ImageJsonProcessor {



    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ImageJsonProcessor <path-to-json-file>");
            return;
        }

	    PgProperties pgProperties = CommonUtils.getPgConnectionProps();
        
        String jsonFilePath = args[0];

        try {
            // Read the JSON file
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(jsonFilePath));

            // Connect to PostgreSQL
            try (Connection conn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(), pgProperties.getDbPassword())) {
                // Recursively find and insert images
                findAndInsertImages(root, conn);
            }

            System.out.println("✅ Done: Images inserted into PostgreSQL.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔁 Recursively search for "images" arrays and insert them
    private static void findAndInsertImages(JsonNode node, Connection conn) throws SQLException {
        if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode child = node.get(fieldName);
                if ("images".equals(fieldName) && child.isArray()) {
                    insertImages((ArrayNode) child, conn);
                } else {
                    findAndInsertImages(child, conn); // Recurse
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                findAndInsertImages(item, conn); // Recurse
            }
        }
    }

    // ➕ Insert image objects into the database
    private static void insertImages(ArrayNode imagesArray, Connection conn) throws SQLException {
        String checkSql = "SELECT 1 FROM images WHERE filename = ?";
        String insertSql = "INSERT INTO images (image_type, filename, title) VALUES (?, ?, ?)";

        try (
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            PreparedStatement insertStmt = conn.prepareStatement(insertSql)
        ) {
            for (JsonNode image : imagesArray) {
                String imageType = image.has("imageType") ? image.get("imageType").asText() : null;
                String filename = image.has("name") ? image.get("name").asText() : null;
                String title = image.has("title") ? image.get("title").asText() : null;

                if (filename == null) {
                    System.out.println("⚠️ Skipping image with missing filename");
                    continue;
                }

                // Check for duplicate
                checkStmt.setString(1, filename);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    System.out.printf("⚠️ Duplicate found: '%s' already exists in DB.%n", filename);
                } else {
                    // Insert new image
                    insertStmt.setString(1, imageType);
                    insertStmt.setString(2, filename);
                    insertStmt.setString(3, title);
                    insertStmt.executeUpdate();
                    System.out.printf("✅ Inserted: %s%n", filename);
                }

                rs.close();
            }
        }
    }

}
