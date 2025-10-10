package com.tabii.data.transformers.pgToRedis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;
import com.tabii.utils.RedisProperties;
import org.postgresql.util.PSQLException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class LookupObjectsToRedisExporter {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        PgProperties pgProperties = CommonUtils.getPgConnectionProps();
        RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();

        try (Connection pgConn = DriverManager.getConnection(
                     pgProperties.getDbUrl(), pgProperties.getDbUser(), pgProperties.getDbPassword());
             JedisPool jedisPool = new JedisPool(redisProperties.getHost(), redisProperties.getPort());
             Jedis jedis = jedisPool.getResource()) {

            System.out.println("✅ Connected to PostgreSQL and Redis");

            // Cache images table for quick lookup
            Map<String, Integer> imageNameToId = loadImageNameIdMap(pgConn);

            // Query lookup_objects table
            String sql = "SELECT id, type, fields FROM lookup_objects WHERE isactive = true";
            try (PreparedStatement ps = pgConn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Long id = rs.getLong("id");
                    if(id == 149694) {
                    	System.out.println();
                    }
                    String type = rs.getString("type");
                    String fieldsJson = rs.getString("fields");

                    if (fieldsJson == null) continue;

                    JsonNode fields = mapper.readTree(fieldsJson);
                    ObjectNode redisValue = mapper.createObjectNode();

                    switch (type.toLowerCase()) {
                        case "exclusive-badge":
                            redisValue.setAll(transformExclusiveBadge(fields));
                            break;

                        case "badges":
                            redisValue.setAll(transformBadges(fields, imageNameToId));
                            break;

                        case "genre":
                            redisValue.setAll(transformGenres(fields, imageNameToId));
                            break;

                        default:
                            System.out.println("⚠️ Skipped unknown type: " + type);
                            continue;
                    }

                    String redisKey = type + ":" + id;
                    jedis.set(redisKey, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(redisValue));
                    System.out.println("✅ Wrote Redis key: " + redisKey);
                }
            }

            System.out.println("🎯 Export complete.");

        } catch (PSQLException e) {
            System.err.println("❌ PostgreSQL error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load image filename → id map
    private static Map<String, Integer> loadImageNameIdMap(Connection conn) throws SQLException {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT filename, id FROM images";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("filename"), rs.getInt("id"));
            }
        }
        System.out.println("📸 Loaded " + map.size() + " images into cache");
        return map;
    }

    // -------- Transformation Helpers -------- //

    private static ObjectNode transformExclusiveBadge(JsonNode fields) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode badges = mapper.createArrayNode();
        ObjectNode badge = mapper.createObjectNode();
        badge.put("exclusiveBadgeType", fields.path("exclusiveBadgeType").asText("originals"));
        badges.add(badge);
        result.set("exclusiveBadges", badges);
        return result;
    }

    private static ObjectNode transformBadges(JsonNode fields, Map<String, Integer> imageMap) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode badgesArr = mapper.createArrayNode();

        if (fields.isArray()) {
            for (JsonNode badgeNode : fields) {
                ObjectNode badge = mapper.createObjectNode();
                badge.put("bannerLocation", badgeNode.path("bannerLocation").asText());
                badge.put("id", badgeNode.path("id").asInt());
                badge.put("showLocation", badgeNode.path("showLocation").asText());
                badge.put("title", badgeNode.path("title").asText());
                badge.put("type", badgeNode.path("type").asText());

                // Replace images array with image IDs
                ArrayNode imageIds = mapper.createArrayNode();
                for (JsonNode imgNode : badgeNode.path("images")) {
                    String filename = imgNode.path("name").asText();
                    Integer id = imageMap.get(filename);
                    if (id != null) imageIds.add(id);
                }
                badge.set("images", imageIds);
                badgesArr.add(badge);
            }
        }

        result.set("badges", badgesArr);
        return result;
    }

    private static ObjectNode transformGenres(JsonNode fields, Map<String, Integer> imageMap) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode genresArr = mapper.createArrayNode();

        if (fields.isArray()) {
            for (JsonNode genreNode : fields) {
                ObjectNode genre = mapper.createObjectNode();
                genre.put("contentType", "genre");
                genre.put("id", genreNode.path("id").asInt());
                genre.put("title", genreNode.path("title").asText());

                ArrayNode imageIds = mapper.createArrayNode();
                for (JsonNode imgNode : genreNode.path("images")) {
                    String filename = imgNode.path("name").asText();
                    Integer id = imageMap.get(filename);
                    if (id != null) imageIds.add(id);
                }
                genre.set("images", imageIds);
                genresArr.add(genre);
            }
        }

        result.set("genres", genresArr);
        return result;
    }
}
