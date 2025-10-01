package com.tabii.data.transformers.jsonToPg;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;

public class BadgeJsonProcessor {



    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java BadgeJsonProcessor <path-to-json-file>");
            return;
        }
        
	    
	    PgProperties pgProperties = CommonUtils.getPgConnectionProps();

        String jsonFilePath = args[0];

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(jsonFilePath));

            try (Connection conn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUrl(), pgProperties.getDbPassword())) {
                findAndInsertBadges(root, conn);
            }

            System.out.println("✅ Done: Badges processed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void findAndInsertBadges(JsonNode node, Connection conn) throws SQLException {
        if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode child = node.get(fieldName);
                if ("badges".equals(fieldName) && child.isArray()) {
                    insertBadges((ArrayNode) child, conn);
                } else {
                    findAndInsertBadges(child, conn); // Recurse
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                findAndInsertBadges(item, conn); // Recurse
            }
        }
    }

    private static void insertBadges(ArrayNode badgesArray, Connection conn) throws SQLException {
        String findImageIdSQL = "SELECT id FROM images WHERE filename = ?";
        String insertBadgeSQL = """
            INSERT INTO badges (id, title, type, banner_location, show_location, image_id)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """;

        try (
            PreparedStatement findImageStmt = conn.prepareStatement(findImageIdSQL);
            PreparedStatement insertBadgeStmt = conn.prepareStatement(insertBadgeSQL)
        ) {
            for (JsonNode badge : badgesArray) {
                long badgeId = badge.has("id") ? badge.get("id").asLong() : 0;
                String title = badge.has("title") ? badge.get("title").asText() : null;
                String type = badge.has("type") ? badge.get("type").asText() : null;
                String bannerLocation = badge.has("bannerLocation") ? badge.get("bannerLocation").asText() : null;
                String showLocation = badge.has("showLocation") ? badge.get("showLocation").asText() : null;

                Integer imageId = null;

                // Handle first image (if available)
                if (badge.has("images") && badge.get("images").isArray()) {
                    ArrayNode images = (ArrayNode) badge.get("images");
                    if (!images.isEmpty()) {
                        JsonNode image = images.get(0);
                        String filename = image.has("name") ? image.get("name").asText() : null;

                        if (filename != null) {
                            findImageStmt.setString(1, filename);
                            ResultSet rs = findImageStmt.executeQuery();
                            if (rs.next()) {
                                imageId = rs.getInt("id");
                                System.out.printf("✅ Badge %d linked to image '%s' (id=%d)%n", badgeId, filename, imageId);
                            } else {
                                System.out.printf("⚠️ No image found for badge %d (filename: %s)%n", badgeId, filename);
                            }
                            rs.close();
                        }
                    }
                }

                // Insert badge
                insertBadgeStmt.setLong(1, badgeId);
                insertBadgeStmt.setString(2, title);
                insertBadgeStmt.setString(3, type);
                insertBadgeStmt.setString(4, bannerLocation);
                insertBadgeStmt.setString(5, showLocation);

                if (imageId != null) {
                    insertBadgeStmt.setInt(6, imageId);
                } else {
                    insertBadgeStmt.setNull(6, Types.INTEGER);
                }

                insertBadgeStmt.executeUpdate();
            }
        }
    }
}
