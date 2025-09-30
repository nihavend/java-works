package com.tabii.data.model.json.loader;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tabii.utils.CommonUtils;

public class JsonImageIdUpdater {

    public static void main(String[] args) {
 
        if (args.length != 1) {
            System.err.println("Usage: java JsonImageIdUpdater <json_file_path>");
            System.exit(1);
        }

	    Properties dbProps = CommonUtils.loadDbProperties();
	    String dbUrl = dbProps.getProperty("pg.db.url");
	    String dbUser = dbProps.getProperty("pg.db.user");
	    String dbPassword = dbProps.getProperty("pg.db.password");
        
        
        String jsonFilePath = args[0];
        try {
            // Read JSON file
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(new File(jsonFilePath));

            // Connect to PostgreSQL
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                // Process all image nodes recursively
                processNode(rootNode, conn);

                // Create output file path with _updated suffix
                String outputFilePath = jsonFilePath.replace(".json", "_updated.json");

                // Write updated JSON to new file
                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputFilePath), rootNode);
                System.out.println("Updated JSON written to: " + outputFilePath);

            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.err.println("Error processing JSON file: " + e.getMessage());
            e.printStackTrace();
        }
        
    }

    private static void processNode(JsonNode node, Connection conn) throws SQLException {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            // Check if this is an image node
            if (objectNode.has("contentType") && objectNode.get("contentType").asText().equals("image")
                    && objectNode.has("name")) {
                String imageName = objectNode.get("name").asText();
                int imageId = getImageIdFromDatabase(conn, imageName);
                if (imageId != -1) {
                    objectNode.put("id", imageId);
                }
            }
            // Recursively process all fields
            for (JsonNode child : objectNode) {
                processNode(child, conn);
            }
        } else if (node.isArray()) {
            // Process each element in the array
            for (JsonNode child : node) {
                processNode(child, conn);
            }
        }
    }

    private static int getImageIdFromDatabase(Connection conn, String imageName) throws SQLException {
        String sql = "SELECT id FROM images WHERE filename = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, imageName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return -1; // Return -1 if image not found
    }
}