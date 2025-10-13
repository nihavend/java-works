package old;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;
import com.tabii.utils.RedisProperties;
import org.json.JSONArray;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LookupObjectsToRedisExporter3 {

    public static void main(String[] args) {
        PgProperties pgProperties = CommonUtils.getPgConnectionProps();
        RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();

        try (Connection pgConn = DriverManager.getConnection(
                pgProperties.getDbUrl(),
                pgProperties.getDbUser(),
                pgProperties.getDbPassword());
             JedisPool jedisPool = new JedisPool(redisProperties.getHost(), redisProperties.getPort());
             Jedis jedis = jedisPool.getResource()) {

            System.out.println("✅ Connected to PostgreSQL and Redis.");

            String sql = "SELECT id, type, fields, title FROM lookup_objects";
            try (Statement stmt = pgConn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    long id = rs.getLong("id");
                    String type = rs.getString("type");
                    String fieldsJson = rs.getString("fields");
                    String title = rs.getString("title");

                    if (type == null) continue;
                    type = type.trim().toLowerCase();

                    JSONObject redisValue = new JSONObject();

                    switch (type) {
                        case "exclusive-badge":
                            redisValue.put("exclusiveBadges", buildExclusiveBadge(fieldsJson, title));
                            break;

                        case "badges":
                            redisValue.put("badges", buildBadges(fieldsJson, id, pgConn));
                            break;

                        case "genre":
                            redisValue.put("genres", buildGenres(fieldsJson, id, pgConn, title));
                            break;

                        default:
                            continue; // ignore other types
                    }

                    String redisKey = type + ":" + id;
                    jedis.set(redisKey, redisValue.toString());
                    System.out.printf("→ Wrote Redis key: %s = %s%n", redisKey, redisValue);
                }
            }

            System.out.println("✅ Export completed successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JSONArray buildExclusiveBadge(String fieldsJson, String title) {
        JSONArray arr = new JSONArray();
        JSONObject obj = new JSONObject();
        obj.put("exclusiveBadgeType", title != null ? title.toLowerCase().replaceAll("\\s+", "") : "unknown");
        arr.put(obj);
        return arr;
    }

    private static JSONArray buildBadges(String fieldsJson, long id, Connection pgConn) {
        JSONArray arr = new JSONArray();
        try {
            JSONObject fields = new JSONObject(fieldsJson != null ? fieldsJson : "{}");

            JSONObject layout = fields.optJSONObject("layout");
            JSONObject bannerLocation = fields.optJSONObject("banner_location");
            JSONObject showLocation = fields.optJSONObject("show_card_location");
            JSONObject badgeImage = fields.optJSONObject("badge_image");
            JSONObject displayTitle = fields.optJSONObject("display_title");

            JSONObject badgeObj = new JSONObject();
            badgeObj.put("id", id);
            badgeObj.put("type", layout != null ? layout.optString("key", "") : "");
            badgeObj.put("bannerLocation", bannerLocation != null ? bannerLocation.optString("key", "") : "");
            badgeObj.put("showLocation", showLocation != null ? showLocation.optString("key", "") : "");
            badgeObj.put("title", displayTitle != null ? displayTitle.optString("text", "") : "");

            List<Integer> imageIds = extractImageIds(fields, pgConn);
            badgeObj.put("images", new JSONArray(imageIds));

            arr.put(badgeObj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

    private static JSONArray buildGenres(String fieldsJson, long id, Connection pgConn, String title) {
        JSONArray arr = new JSONArray();
        try {
            JSONObject fields = new JSONObject(fieldsJson != null ? fieldsJson : "{}");

            JSONObject genreObj = new JSONObject();
            genreObj.put("contentType", "genre");
            genreObj.put("id", id);
            genreObj.put("title", title != null ? title : "");

            List<Integer> imageIds = extractImageIds(fields, pgConn);
            genreObj.put("images", new JSONArray(imageIds));

            arr.put(genreObj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

    private static List<Integer> extractImageIds(JSONObject fields, Connection pgConn) {
        List<Integer> ids = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"fileName\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(fields.toString());

        while (matcher.find()) {
            String fileName = matcher.group(1);
            Integer imageId = findImageIdByFilename(pgConn, fileName);
            if (imageId != null) ids.add(imageId);
        }
        return ids;
    }

    private static Integer findImageIdByFilename(Connection conn, String filename) {
        String sql = "SELECT id FROM images WHERE filename = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filename);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
