package com.tabii.data.transformers.pgToRedis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;
import com.tabii.utils.RedisProperties;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class LookupObjectsToRedisExporter {

    public static void main(String[] args) {
        PgProperties pgProperties = CommonUtils.getPgConnectionProps();
        RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();

        try (Connection pgConn = DriverManager.getConnection(
                pgProperties.getDbUrl(), pgProperties.getDbUser(), pgProperties.getDbPassword());
             JedisPool jedisPool = new JedisPool(redisProperties.getHost(), redisProperties.getPort());
             Jedis jedis = jedisPool.getResource()) {

            exportLookupObjects(pgConn, jedis);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exportLookupObjects(Connection pgConn, Jedis jedis) throws SQLException {
        String sql = "SELECT id, type, fields FROM lookup_objects";
        try (Statement stmt = pgConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                long id = rs.getLong("id");
                String type = rs.getString("type");
                String fieldsJson = rs.getString("fields");

                if (fieldsJson == null || fieldsJson.isEmpty()) continue;

                JSONObject fields = new JSONObject(fieldsJson);
                String redisKey = type + ":" + id;
                JSONObject redisValue = new JSONObject();

                switch (type) {
                    case "exclusive-badge":
                        redisValue = buildExclusiveBadgeValue(fields);
                        break;
                    case "badges":
                        redisValue = buildBadgesValue(pgConn, id, fields);
                        break;
                    case "genre":
                        redisValue = buildGenreValue(pgConn, id, fields);
                        break;
                    default:
                        // Other types ignored
                        continue;
                }

                jedis.set(redisKey, redisValue.toString());
                System.out.println("✅ Saved to Redis -> " + redisKey + " = " + redisValue);
            }
        }
    }

    private static JSONObject buildExclusiveBadgeValue(JSONObject fields) {
        JSONObject badge = new JSONObject();
        String badgeType = extractExclusiveBadgeType(fields);
        badge.put("exclusiveBadgeType", badgeType);

        JSONArray arr = new JSONArray();
        arr.put(badge);

        JSONObject result = new JSONObject();
        result.put("exclusiveBadges", arr);
        return result;
    }

    private static String extractExclusiveBadgeType(JSONObject fields) {
        // Example heuristic: extract from layout.key if exists
        if (fields.has("layout")) {
            JSONObject layout = fields.optJSONObject("layout");
            if (layout != null && layout.has("key")) {
                return layout.getString("key").toLowerCase();
            }
        }
        return "originals";
    }

    private static JSONObject buildBadgesValue(Connection pgConn, long id, JSONObject fields) throws SQLException {
        JSONObject obj = new JSONObject();

        obj.put("bannerLocation", extractString(fields, "banner_location", "key"));
        obj.put("id", id);
        obj.put("showLocation", extractString(fields, "show_card_location", "key"));
        obj.put("title", extractString(fields, "display_title", "text"));
        obj.put("type", extractString(fields, "layout", "key"));

        JSONArray imageIds = findImageIdsFromFields(pgConn, fields);
        obj.put("images", imageIds);

        return obj;
    }

    private static JSONObject buildGenreValue(Connection pgConn, long id, JSONObject fields) throws SQLException {
        JSONObject obj = new JSONObject();
        obj.put("contentType", "genre");
        obj.put("id", id);
        obj.put("title", extractString(fields, "display_title", "text"));

        JSONArray imageIds = findImageIdsFromFields(pgConn, fields);
        obj.put("images", imageIds);

        return obj;
    }

    private static JSONArray findImageIdsFromFields(Connection pgConn, JSONObject fields) throws SQLException {
        List<String> fileNames = new ArrayList<>();
        findFileNamesRecursive(fields, fileNames);

        JSONArray imageIds = new JSONArray();
        for (String fileName : fileNames) {
            Long imageId = getImageIdByFilename(pgConn, fileName);
            if (imageId != null) {
                imageIds.put(imageId);
            }
        }
        return imageIds;
    }

    private static void findFileNamesRecursive(Object node, List<String> fileNames) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            if (obj.optString("type").equals("image") && obj.has("fileName")) {
                fileNames.add(obj.getString("fileName"));
            }
            for (String key : obj.keySet()) {
                findFileNamesRecursive(obj.get(key), fileNames);
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.length(); i++) {
                findFileNamesRecursive(arr.get(i), fileNames);
            }
        }
    }

    private static Long getImageIdByFilename(Connection pgConn, String fileName) throws SQLException {
        String sql = "SELECT id FROM images WHERE filename = ?";
        try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
            ps.setString(1, fileName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        return null;
    }

    private static String extractString(JSONObject fields, String nodeKey, String subKey) {
        if (fields.has(nodeKey)) {
            JSONObject node = fields.optJSONObject(nodeKey);
            if (node != null && node.has(subKey)) {
                return node.optString(subKey);
            }
        }
        return "";
    }
}
