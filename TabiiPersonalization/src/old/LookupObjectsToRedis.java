package old;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.JSONArray;
import org.json.JSONObject;

import redis.clients.jedis.Jedis;

public class LookupObjectsToRedis {

    private Connection pgConnection;
    private Jedis redis;

    public LookupObjectsToRedis(Connection pgConnection, Jedis redis) {
        this.pgConnection = pgConnection;
        this.redis = redis;
    }

    public void migrateLookupObjects() throws SQLException {
        String lookupQuery = "SELECT * FROM lookup_objects WHERE isactive = true";
        try (Statement stmt = pgConnection.createStatement();
             ResultSet rs = stmt.executeQuery(lookupQuery)) {

            while (rs.next()) {
                String type = rs.getString("type");
                long typeId = rs.getLong("typeid");
                JSONObject redisValue = null;

                switch (type) {
                    case "exclusive-badge":
                        redisValue = transformExclusiveBadge(rs);
                        break;
                    case "badges":
                        redisValue = transformBadges(rs);
                        break;
                    case "genre":
                        redisValue = transformGenres(rs);
                        break;
                    default:
                        continue; // skip unknown types
                }

                if (redisValue != null) {
                    String redisKey = type + ":" + typeId;
                    redis.set(redisKey, redisValue.toString());
                    System.out.println("Written to Redis key: " + redisKey);
                }
            }
        }
    }

    private JSONObject transformExclusiveBadge(ResultSet rs) throws SQLException {
        // Example: {"exclusiveBadges":[{"exclusiveBadgeType":"originals"}]}
        JSONObject obj = new JSONObject();
        JSONArray arr = new JSONArray();
        JSONObject badge = new JSONObject();
        badge.put("exclusiveBadgeType", "originals");
        arr.put(badge);
        obj.put("exclusiveBadges", arr);
        return obj;
    }

    private JSONObject transformBadges(ResultSet rs) throws SQLException {
        JSONObject obj = new JSONObject();
        JSONArray badgesArr = new JSONArray();

        JSONArray fieldsBadges = new JSONArray(rs.getString("fields")); // Assuming JSON array in "fields"
        for (int i = 0; i < fieldsBadges.length(); i++) {
            JSONObject b = fieldsBadges.getJSONObject(i);
            JSONObject badge = new JSONObject();
            badge.put("bannerLocation", b.optString("bannerLocation", ""));
            badge.put("id", b.getLong("id"));
            badge.put("showLocation", b.optString("showLocation", ""));
            badge.put("title", b.optString("title", ""));
            badge.put("type", b.optString("type", ""));

            JSONArray imagesArr = new JSONArray();
            JSONArray originalImages = b.getJSONArray("images");
            for (int j = 0; j < originalImages.length(); j++) {
                JSONObject image = originalImages.getJSONObject(j);
                Integer imageId = getImageIdByName(image.getString("name"));
                if (imageId != null) {
                    imagesArr.put(imageId);
                }
            }
            badge.put("images", imagesArr);
            badgesArr.put(badge);
        }

        obj.put("badges", badgesArr);
        return obj;
    }

    private JSONObject transformGenres(ResultSet rs) throws SQLException {
        JSONObject obj = new JSONObject();
        JSONArray genresArr = new JSONArray();

        JSONArray fieldsGenres = new JSONArray(rs.getString("fields")); // Assuming JSON array in "fields"
        for (int i = 0; i < fieldsGenres.length(); i++) {
            JSONObject g = fieldsGenres.getJSONObject(i);
            JSONObject genre = new JSONObject();
            genre.put("contentType", "genre");
            genre.put("id", g.getLong("id"));
            genre.put("title", g.getString("title"));

            JSONArray imagesArr = new JSONArray();
            JSONArray originalImages = g.getJSONArray("images");
            for (int j = 0; j < originalImages.length(); j++) {
                JSONObject image = originalImages.getJSONObject(j);
                Integer imageId = getImageIdByName(image.getString("name"));
                if (imageId != null) {
                    imagesArr.put(imageId);
                }
            }
            genre.put("images", imagesArr);
            genresArr.put(genre);
        }

        obj.put("genres", genresArr);
        return obj;
    }

    private Integer getImageIdByName(String filename) throws SQLException {
        String query = "SELECT id FROM images WHERE filename = ?";
        try (PreparedStatement ps = pgConnection.prepareStatement(query)) {
            ps.setString(1, filename);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        Connection pgConn = DriverManager.getConnection(
            "jdbc:postgresql://localhost:5432/yourdb", "user", "password");
        Jedis redis = new Jedis("localhost", 6379);

        LookupObjectsToRedis exporter = new LookupObjectsToRedis(pgConn, redis);
        exporter.migrateLookupObjects();

        pgConn.close();
        redis.close();
    }
}
