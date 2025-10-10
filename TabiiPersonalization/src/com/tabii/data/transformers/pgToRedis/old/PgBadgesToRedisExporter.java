package com.tabii.data.transformers.pgToRedis;

import org.json.JSONArray;
import org.json.JSONObject;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;
import com.tabii.utils.RedisProperties;

import redis.clients.jedis.Jedis;

import java.sql.*;

public class PgBadgesToRedisExporter {

	
	public static void main(String[] args) {
		migrate();
	}
	
    public static void migrate() {
    	// PostgreSQL connection settings
    	PgProperties pgProperties = CommonUtils.getPgConnectionProps();
    	
    	 // Redis connection settings
    	RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();

        try (
        		Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(), pgProperties.getDbPassword());
        		Jedis jedis = new Jedis(redisProperties.getHost(), redisProperties.getPort())
        ) {
            String sql = "SELECT typeid, title, fields FROM lookup_objects WHERE type = 'badges'";
            PreparedStatement stmt = pgConn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                long typeId = rs.getLong("typeid");
                String title = rs.getString("title");
                String fieldsJson = rs.getString("fields");

                JSONObject badgeObj = new JSONObject();
                badgeObj.put("id", typeId);
                badgeObj.put("title", title);

                JSONArray imagesArr = new JSONArray();

                if (fieldsJson != null) {
                    JSONObject fieldsObj = new JSONObject(fieldsJson);

                    // type from layout.key
                    if (fieldsObj.has("layout")) {
                        JSONObject layoutObj = fieldsObj.optJSONObject("layout");
                        if (layoutObj != null) {
                            badgeObj.put("type", layoutObj.optString("key", null));
                        }
                    }

                    // showLocation mapping
                    if (fieldsObj.has("show_card_location")) {
                        JSONObject showLocObj = fieldsObj.optJSONObject("show_card_location");
                        if (showLocObj != null) {
                            badgeObj.put("showLocation",
                                    mapLocation(showLocObj.optString("key", "")));
                        }
                    }

                    // bannerLocation mapping
                    if (fieldsObj.has("banner_location")) {
                        JSONObject bannerLocObj = fieldsObj.optJSONObject("banner_location");
                        if (bannerLocObj != null) {
                            badgeObj.put("bannerLocation",
                                    mapLocation(bannerLocObj.optString("key", "")));
                        }
                    }

                    // badge_image lookup
                    if (fieldsObj.has("badge_image")) {
                        JSONObject badgeImgObj = fieldsObj.optJSONObject("badge_image");
                        if (badgeImgObj != null) {
                            String filename = badgeImgObj.optString("fileName", null);
                            if (filename != null) {
                                int imageId = findImageId(pgConn, filename);
                                if (imageId != -1) {
                                    imagesArr.put(imageId);
                                }
                            }
                        }
                    }
                }

                badgeObj.put("images", imagesArr);

                String redisKey = "badges:" + typeId;
                jedis.set(redisKey, badgeObj.toString());

                System.out.println("✅ Saved to Redis: " + redisKey + " -> " + badgeObj);
            }

            rs.close();
            stmt.close();
            System.out.println("🚀 Export completed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String mapLocation(String key) {
        switch (key) {
            case "upper_right_corner":
                return "rightTop";
            case "upper_left_corner":
                return "leftTop";
            case "lower_left_corner":
                return "leftBottom";
            case "on_top_of_the_logo":
                return "upLogo";
            case "under_the_logo":
                return "bottomLogo";
            case "do_not_show":
            default:
                return "invisible";
        }
    }

    private static int findImageId(Connection conn, String filename) throws SQLException {
        String sql = "SELECT id FROM images WHERE filename = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filename);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return -1;
    }
}
