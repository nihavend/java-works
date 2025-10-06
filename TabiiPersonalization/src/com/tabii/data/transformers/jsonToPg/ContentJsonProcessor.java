package com.tabii.data.transformers.jsonToPg;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;

public class ContentJsonProcessor {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java ContentJsonProcessor <path-to-json-file>");
            return;
        }
        

        PgProperties pgProperties = CommonUtils.getPgConnectionProps();

        String jsonFilePath = args[0];

        try (Connection conn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(), pgProperties.getDbPassword())) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(jsonFilePath));
            findAndInsertContents(root, conn);
            System.out.println("✅ All contents processed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void findAndInsertContents(JsonNode node, Connection conn) throws SQLException {
        if (node.isObject()) {
            Iterator<String> fields = node.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                JsonNode child = node.get(field);
                if ("contents".equals(field) && child.isArray()) {
                    insertContents((ArrayNode) child, conn);
                } else {
                    findAndInsertContents(child, conn); // Recursive
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                findAndInsertContents(item, conn);
            }
        }
    }

    private static void insertContents(ArrayNode contents, Connection conn) throws SQLException {
        String insertContentSQL = """
            INSERT INTO contents (id, title, description, spot, made_year, content_type)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO NOTHING
        """;

        String selectImageIdSQL = "SELECT id FROM images WHERE filename = ?";
        String insertImageLinkSQL = """
            INSERT INTO content_images (content_id, image_id)
            VALUES (?, ?) ON CONFLICT DO NOTHING
        """;
        String insertGenreLinkSQL = """
            INSERT INTO content_genres (content_id, genre_id)
            VALUES (?, ?) ON CONFLICT DO NOTHING
        """;
        String insertBadgeLinkSQL = """
            INSERT INTO content_badges (content_id, badge_id)
            VALUES (?, ?) ON CONFLICT DO NOTHING
        """;

        try (
            PreparedStatement contentStmt = conn.prepareStatement(insertContentSQL);
            PreparedStatement imageStmt = conn.prepareStatement(selectImageIdSQL);
            PreparedStatement imageLinkStmt = conn.prepareStatement(insertImageLinkSQL);
            PreparedStatement genreLinkStmt = conn.prepareStatement(insertGenreLinkSQL);
            PreparedStatement badgeLinkStmt = conn.prepareStatement(insertBadgeLinkSQL)
        ) {
            for (JsonNode content : contents) {
                long contentId = content.get("id").asLong();
                String title = content.path("title").asText(null);
                String description = content.path("description").asText(null);
                String spot = content.path("spot").asText(null);
                int madeYear = content.path("madeYear").asInt(0);
                String contentType = content.path("contentType").asText(null);

                // Insert into contents table
                contentStmt.setLong(1, contentId);
                contentStmt.setString(2, title);
                contentStmt.setString(3, description);
                contentStmt.setString(4, spot);
                contentStmt.setInt(5, madeYear);
                contentStmt.setString(6, contentType);
                contentStmt.executeUpdate();

                // Process images
                if (content.has("images")) {
                    for (JsonNode image : content.get("images")) {
                        String filename = image.path("name").asText(null);
                        if (filename != null) {
                            imageStmt.setString(1, filename);
                            ResultSet rs = imageStmt.executeQuery();
                            if (rs.next()) {
                                int imageId = rs.getInt("id");
                                imageLinkStmt.setLong(1, contentId);
                                imageLinkStmt.setInt(2, imageId);
                                imageLinkStmt.executeUpdate();
                            } else {
                                System.out.printf("⚠️ Image not found for content %d: %s%n", contentId, filename);
                            }
                            rs.close();
                        }
                    }
                }

                // Process genres
                if (content.has("genres")) {
                    for (JsonNode genre : content.get("genres")) {
                        if (genre.has("id")) {
                            long genreId = genre.get("id").asLong();
                            genreLinkStmt.setLong(1, contentId);
                            genreLinkStmt.setLong(2, genreId);
                            genreLinkStmt.executeUpdate();
                        }
                    }
                }

                // Process badges
                if (content.has("badges")) {
                    Set<Long> badgeIds = new HashSet<>();
                    for (JsonNode badge : content.get("badges")) {
                        if (badge.has("id")) {
                            long badgeId = badge.get("id").asLong();
                            if (badgeIds.add(badgeId)) { // avoid duplicates
                                badgeLinkStmt.setLong(1, contentId);
                                badgeLinkStmt.setLong(2, badgeId);
                                badgeLinkStmt.executeUpdate();
                            }
                        }
                    }
                }

                System.out.printf("✅ Inserted content: %s (%d)%n", title, contentId);
            }
        }
    }
}
