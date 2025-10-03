package com.tabii.data.transformers.pgToRedis;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.json.JSONArray;

import redis.clients.jedis.Jedis;

public class ImagesToRedisExporter {

    private static final Logger logger = Logger.getLogger(ImagesToRedisExporter.class.getName());

    public static void main(String[] args) {
        String pgUrl = "jdbc:postgresql://localhost:5432/yourdb";
        String pgUser = "youruser";
        String pgPassword = "yourpassword";

        String redisHost = "localhost";
        int redisPort = 6379;

        try (Connection conn = DriverManager.getConnection(pgUrl, pgUser, pgPassword);
             Jedis jedis = new Jedis(redisHost, redisPort)) {

            // 1. Query lookup_objects
            String sql = "SELECT id, fields, genre FROM lookup_objects WHERE fields IS NOT NULL OR genre IS NOT NULL";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    int objectId = rs.getInt("id");
                    String fieldsJson = rs.getString("fields");
                    String genreJson = rs.getString("genre");

                    Set<Integer> imageIds = new HashSet<>();

                    // extract from genre
                    if (genreJson != null) {
                        try {
                            JSONObject genreObj = new JSONObject(genreJson);
                            if (genreObj.has("id")) {
                                imageIds.add(genreObj.getInt("id"));
                            }
                        } catch (Exception e) {
                            logger.warning("Invalid genre JSON in object " + objectId);
                        }
                    }

                    // extract from fields
                    if (fieldsJson != null) {
                        try {
                            JSONObject fieldsObj = new JSONObject(fieldsJson);
                            String[] keys = {
                                    "main_image",
                                    "main_image_with_logo",
                                    "cover_image",
                                    "mobile_cover_image",
                                    "vertical_image",
                                    "vertical_image_with_logo",
                                    "show_logo",
                                    "promo_image"
                            };

                            for (String key : keys) {
                                if (fieldsObj.has(key)) {
                                    Object val = fieldsObj.get(key);
                                    if (val instanceof JSONObject && ((JSONObject) val).has("id")) {
                                        imageIds.add(((JSONObject) val).getInt("id"));
                                    } else if (val instanceof JSONArray) {
                                        JSONArray arr = (JSONArray) val;
                                        for (int i = 0; i < arr.length(); i++) {
                                            JSONObject img = arr.optJSONObject(i);
                                            if (img != null && img.has("id")) {
                                                imageIds.add(img.getInt("id"));
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.warning("Invalid fields JSON in object " + objectId);
                        }
                    }

                    // 2. Validate images against images table
                    for (Integer imgId : imageIds) {
                        if (existsInImagesTable(conn, imgId)) {
                            String redisKey = "image:" + imgId;
                            jedis.set(redisKey, imgId.toString());
                            logger.info("Stored in Redis: " + redisKey);
                        } else {
                            logger.warning("Image not found in images table: id=" + imgId + " (objectId=" + objectId + ")");
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean existsInImagesTable(Connection conn, int imageId) throws SQLException {
        String sql = "SELECT 1 FROM images WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, imageId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
