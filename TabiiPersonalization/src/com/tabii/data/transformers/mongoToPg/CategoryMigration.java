package com.tabii.data.transformers.mongoToPg;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.sql.*;
import java.time.Instant;
import java.util.*;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.MongoProperties;
import com.tabii.utils.PgProperties;

public class CategoryMigration {

    public static void main(String[] args) {
        try {
            PgProperties pgProperties = CommonUtils.getPgConnectionProps();
            MongoProperties mongoProperties = CommonUtils.getMongoConnectionProps();
            String mongoCollection = "lookup";

            // Mongo Connection
            MongoClient mongoClient = MongoClients.create(mongoProperties.getMongoUri());
            MongoDatabase mongoDb = mongoClient.getDatabase(mongoProperties.getMongoDb());
            MongoCollection<Document> collection = mongoDb.getCollection(mongoCollection);

            // Postgres Connection
            Connection pgConn = DriverManager.getConnection(
                    pgProperties.getDbUrl(),
                    pgProperties.getDbUser(),
                    pgProperties.getDbPassword()
            );

            // Prepared statements
            PreparedStatement insertCategory = pgConn.prepareStatement(
                    "INSERT INTO categories " +
                            "(id, isActive, path, site, title, viewcount, " +
                            "published_date, published_status, published_revisionId, " +
                            "modified_user, modified_date, created_user, created_date) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON CONFLICT (id) DO UPDATE SET " +
                            "isActive = EXCLUDED.isActive, path = EXCLUDED.path, site = EXCLUDED.site, " +
                            "title = EXCLUDED.title, viewcount = EXCLUDED.viewcount, " +
                            "published_date = EXCLUDED.published_date, published_status = EXCLUDED.published_status, " +
                            "published_revisionId = EXCLUDED.published_revisionId, " +
                            "modified_user = EXCLUDED.modified_user, modified_date = EXCLUDED.modified_date, " +
                            "created_user = EXCLUDED.created_user, created_date = EXCLUDED.created_date"
            );

            PreparedStatement insertImage = pgConn.prepareStatement(
                    "INSERT INTO images (filename, url, width, height) " +
                            "VALUES (?, ?, ?, ?) " +
                            "ON CONFLICT (filename) DO UPDATE SET " +
                            "url = EXCLUDED.url, width = EXCLUDED.width, height = EXCLUDED.height " +
                            "RETURNING id"
            );

            PreparedStatement insertCategoryImage = pgConn.prepareStatement(
                    "INSERT INTO category_images (category_id, image_id) " +
                            "VALUES (?, ?) ON CONFLICT DO NOTHING"
            );

            // Query Mongo
            FindIterable<Document> docs = collection.find(Filters.eq("type", "category"));

            for (Document doc : docs) {
                try {
                    long id = getLong(doc, "id");
                    boolean isActive = doc.getBoolean("isActive", false);
                    String path = doc.getString("path");
                    String site = doc.getString("site");
                    String title = doc.getString("title");
                    long viewCount = getNestedLong(doc, "viewCount.$numberLong");

                    // Published
                    Document published = doc.get("published", Document.class);
                    Timestamp publishedDate = null;
                    String publishedStatus = null;
                    Long revisionId = null;
                    if (published != null) {
                        publishedDate = toTimestamp(published.getString("date"));
                        publishedStatus = published.get("status") != null ? published.get("status").toString() : null;
                        revisionId = getLongSafe(published, "revisionId.$numberLong");
                    }

                    // Audit
                    Document audit = doc.get("audit", Document.class);
                    Timestamp createdDate = null;
                    Timestamp modifiedDate = null;
                    int createdUser = 5;
                    int modifiedUser = 5;

                    if (audit != null) {
                        Document created = audit.get("created", Document.class);
                        if (created != null) {
                            createdDate = toTimestamp(created.getString("date"));
                            Document createdUserDoc = created.get("user", Document.class);
                            if (createdUserDoc != null) {
                                createdUser = getUserId(pgConn, createdUserDoc);
                            }
                        }
                        Document modified = audit.get("modified", Document.class);
                        if (modified != null) {
                            modifiedDate = toTimestamp(modified.getString("date"));
                            Document modifiedUserDoc = modified.get("user", Document.class);
                            if (modifiedUserDoc != null) {
                                modifiedUser = getUserId(pgConn, modifiedUserDoc);
                            }
                        }
                    }

                    // Insert into categories
                    insertCategory.setLong(1, id);
                    insertCategory.setBoolean(2, isActive);
                    insertCategory.setString(3, path);
                    insertCategory.setString(4, site);
                    insertCategory.setString(5, title);
                    insertCategory.setLong(6, viewCount);
                    insertCategory.setTimestamp(7, publishedDate);
                    insertCategory.setString(8, publishedStatus);
                    if (revisionId != null) insertCategory.setLong(9, revisionId); else insertCategory.setNull(9, Types.BIGINT);
                    insertCategory.setInt(10, modifiedUser);
                    insertCategory.setTimestamp(11, modifiedDate != null ? modifiedDate : new Timestamp(System.currentTimeMillis()));
                    insertCategory.setInt(12, createdUser);
                    insertCategory.setTimestamp(13, createdDate != null ? createdDate : new Timestamp(System.currentTimeMillis()));
                    insertCategory.executeUpdate();

                    // Images
                    List<Document> body = (List<Document>) doc.get("body", List.class);
                    if (body != null) {
                        for (Document b : body) {
                            if (b.containsKey("filename")) {
                                String filename = b.getString("filename");
                                String url = b.getString("url");
                                Integer width = b.getInteger("width", 0);
                                Integer height = b.getInteger("height", 0);

                                insertImage.setString(1, filename);
                                insertImage.setString(2, url);
                                insertImage.setInt(3, width);
                                insertImage.setInt(4, height);

                                ResultSet rs = insertImage.executeQuery();
                                int imageId = 0;
                                if (rs.next()) {
                                    imageId = rs.getInt(1);
                                }
                                rs.close();

                                insertCategoryImage.setLong(1, id);
                                insertCategoryImage.setInt(2, imageId);
                                insertCategoryImage.executeUpdate();
                            }
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Error migrating category: " + e.getMessage());
                }
            }

            pgConn.close();
            mongoClient.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static long getLong(Document doc, String field) {
        try {
            Object val = doc.get(field);
            if (val instanceof Document) {
                return Long.parseLong(((Document) val).getString("$numberLong"));
            } else if (val != null) {
                return Long.parseLong(val.toString());
            }
        } catch (Exception ignored) {}
        return 0L;
    }

    private static long getNestedLong(Document doc, String path) {
        try {
            String[] keys = path.split("\\.");
            Document current = doc;
            for (int i = 0; i < keys.length - 1; i++) {
                current = current.get(keys[i], Document.class);
                if (current == null) return 0L;
            }
            Object val = current.get(keys[keys.length - 1]);
            if (val != null) return Long.parseLong(val.toString());
        } catch (Exception ignored) {}
        return 0L;
    }

    private static Long getLongSafe(Document doc, String field) {
        try {
            Object val = doc.get(field);
            if (val != null) return Long.parseLong(val.toString());
        } catch (Exception ignored) {}
        return null;
    }

    private static Timestamp toTimestamp(String dateStr) {
        try {
            return Timestamp.from(Instant.parse(dateStr));
        } catch (Exception e) {
            return new Timestamp(System.currentTimeMillis());
        }
    }

    private static int getUserId(Connection conn, Document userDoc) {
        try {
            String email = userDoc.getString("email");
            String name = userDoc.getString("name");
            if (email == null) return 0;

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users (email, name) VALUES (?, ?) " +
                            "ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name RETURNING id"
            );
            stmt.setString(1, email);
            stmt.setString(2, name);
            ResultSet rs = stmt.executeQuery();
            int id = 0;
            if (rs.next()) id = rs.getInt(1);
            rs.close();
            return id;
        } catch (Exception e) {
            return 0;
        }
    }
}
