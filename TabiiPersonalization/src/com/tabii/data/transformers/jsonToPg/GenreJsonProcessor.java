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

public class GenreJsonProcessor {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java GenreJsonProcessor <path-to-json-file>");
            return;
        }

        PgProperties pgProperties = CommonUtils.getPgConnectionProps();
        
        String jsonFilePath = args[0];

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(jsonFilePath));

            try (Connection conn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUrl(), pgProperties.getDbPassword())) {
                findAndInsertGenres(root, conn);
            }

            System.out.println("✅ Done: Genres processed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void findAndInsertGenres(JsonNode node, Connection conn) throws SQLException {
        if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode child = node.get(fieldName);
                if ("genres".equals(fieldName) && child.isArray()) {
                    insertGenres((ArrayNode) child, conn);
                } else {
                    findAndInsertGenres(child, conn); // Recurse
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                findAndInsertGenres(item, conn); // Recurse
            }
        }
    }

    private static void insertGenres(ArrayNode genresArray, Connection conn) throws SQLException {
        String findImageIdSQL = "SELECT id FROM images WHERE filename = ?";
        String insertGenreSQL = """
            INSERT INTO genres (id, title, image_id)
            VALUES (?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """;

        try (
            PreparedStatement findImageStmt = conn.prepareStatement(findImageIdSQL);
            PreparedStatement insertGenreStmt = conn.prepareStatement(insertGenreSQL)
        ) {
            for (JsonNode genre : genresArray) {
                long genreId = genre.has("id") ? genre.get("id").asLong() : 0;
                String title = genre.has("title") ? genre.get("title").asText() : null;

                Integer imageId = null;

                // Get the first image (if any)
                if (genre.has("images") && genre.get("images").isArray()) {
                    ArrayNode images = (ArrayNode) genre.get("images");
                    if (!images.isEmpty()) {
                        JsonNode image = images.get(0); // Use the first image only
                        String filename = image.has("name") ? image.get("name").asText() : null;

                        if (filename != null) {
                            findImageStmt.setString(1, filename);
                            ResultSet rs = findImageStmt.executeQuery();
                            if (rs.next()) {
                                imageId = rs.getInt("id");
                                System.out.printf("✅ Genre %d linked to image '%s' (id=%d)%n", genreId, filename, imageId);
                            } else {
                                System.out.printf("⚠️ No matching image for genre %d with filename '%s'%n", genreId, filename);
                            }
                            rs.close();
                        }
                    }
                }

                // Insert the genre
                insertGenreStmt.setLong(1, genreId);
                insertGenreStmt.setString(2, title);

                if (imageId != null) {
                    insertGenreStmt.setInt(3, imageId);
                } else {
                    insertGenreStmt.setNull(3, Types.INTEGER);
                }

                insertGenreStmt.executeUpdate();
            }
        }
    }
}
