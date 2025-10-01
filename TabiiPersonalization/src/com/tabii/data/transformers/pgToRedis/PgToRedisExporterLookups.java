package com.tabii.data.transformers.pgToRedis;

import org.json.JSONObject;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;
import com.tabii.utils.RedisProperties;

import redis.clients.jedis.Jedis;

import java.sql.*;

public class PgToRedisExporterLookups {



    public static void main(String[] args) {
    	
    	// PostgreSQL connection settings
    	PgProperties pgProperties = CommonUtils.getPgConnectionProps();
    	
    	 // Redis connection settings
    	RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();
    	
        try (
                Connection pgConn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(), pgProperties.getDbPassword());
                Jedis jedis = new Jedis(redisProperties.getHost(), redisProperties.getPort())
        ) {
            String sql = "SELECT id, type, typeid, audit, body, fields, isactive, metadata, path, " +
                    "paths, published, site, title, viewcount, audit_user_id " +
                    "FROM lookup_objects";

            try (PreparedStatement ps = pgConn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    int typeid = rs.getInt("typeid");
                    String type = rs.getString("type");

                    // Redis key = type:id (unchanged)
                    String redisKey = type + ":" + typeid;
                    System.out.println("Redis Key : " + redisKey);
                    // JSON value (now includes typeid too)
                    JSONObject value = new JSONObject();
                    value.put("id", id);
                    value.put("type", type);
                    value.put("typeid", rs.getLong("typeid"));  // new column
                    value.put("audit", safeJson(rs.getString("audit")));
                    value.put("body", safeJson(rs.getString("body")));
                    value.put("fields", safeJson(rs.getString("fields")));
                    value.put("isactive", rs.getBoolean("isactive"));
                    value.put("metadata", safeJson(rs.getString("metadata")));
                    value.put("path", rs.getString("path"));
                    value.put("paths", safeJson(rs.getString("paths")));
                    value.put("published", safeJson(rs.getString("published")));
                    value.put("site", rs.getString("site"));
                    value.put("title", rs.getString("title"));
                    value.put("viewcount", rs.getLong("viewcount"));
                    value.put("audit_user_id", rs.getLong("audit_user_id"));

                    jedis.set(redisKey, value.toString());
                    System.out.println("Stored in Redis: " + redisKey);
                }
            }

            System.out.println("✅ Export completed successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object safeJson(String json) {
        if (json == null) return JSONObject.NULL;
        try {
            return new JSONObject(json);
        } catch (Exception e) {
            return json;
        }
    }
}
