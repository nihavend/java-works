package com.tabii.data.transformers.mongoToPg;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// MongoToPostgresContentExporter.java
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.MongoProperties;
import com.tabii.utils.PgProperties;

/**
 * Migrates documents from MongoDB "shows" collection to PostgreSQL "contents" table and related tables.
 * Java 17+, MongoDB Driver 4.11+, PostgreSQL JDBC.
 */
public class MongoToPostgresContentExporter {
    private static final Logger log = LoggerFactory.getLogger(MongoToPostgresContentExporter.class);

    // Allowed tables for insertRelation() to avoid unsafe SQL building
    private static final Set<String> ALLOWED_RELATION_TABLES = Set.of("content_images", "content_lookup_relations");

    public static void main(String[] args) {
        MongoProperties mongoProperties = CommonUtils.getMongoConnectionProps();
        String mongoUri = mongoProperties.getMongoUri();
        String mongoDb = mongoProperties.getMongoDb();
        String mongoCollection = "shows";

        PgProperties pgProperties = CommonUtils.getPgConnectionProps();
        String pgUrl = pgProperties.getDbUrl();
        String pgUser = pgProperties.getDbUser();
        String pgPassword = pgProperties.getDbPassword();

        log.info("Starting migration from MongoDB {}.{} -> Postgres {}", mongoDb, mongoCollection, pgUrl);

        try (MongoClient mongoClient = MongoClients.create(mongoUri);
             Connection pgConn = DriverManager.getConnection(pgUrl, pgUser, pgPassword)) {

            pgConn.setAutoCommit(false); // we will commit per-document to allow partial progress while ensuring consistency per document

            MongoDatabase database = mongoClient.getDatabase(mongoDb);
            MongoCollection<Document> collection = database.getCollection(mongoCollection);

            FindIterable<Document> docs = collection.find();

            int processed = 0;
            for (Document doc : docs) {
                processed++;
                try {
                    processShowDocument(pgConn, doc);
                    pgConn.commit();
                } catch (Exception e) {
                    log.warn("Error processing document (will continue). _id={} error={}", safeId(doc), e.toString());
                    try {
                        pgConn.rollback();
                    } catch (SQLException se) {
                        log.error("Error rolling back transaction: {}", se.toString());
                    }
                }
            }

            log.info("Migration finished. Documents processed: {}", processed);

        } catch (Exception e) {
            log.error("Fatal error during migration: ", e);
        }
    }

    private static String safeId(Document doc) {
        ObjectId oid = (doc == null) ? null : doc.getObjectId("_id");
        return (oid != null) ? oid.toHexString() : String.valueOf(doc != null ? doc.get("_id") : "null");
    }

    private static void processShowDocument(Connection pgConn, Document doc) {
        // Extract top-level fields
        String mongoId = extractMongoIdAsString(doc);
        String title = getFieldValue(doc, "title", String.class);
        String description = getFieldValue(doc, "description", String.class);
        String contentType = getFieldValue(doc, "contentType", String.class);

        Document fields = getFieldValue(doc, "fields", Document.class);
        String spot = (fields != null) ? getFieldValue(fields, "spot", String.class) : null;
        Integer madeYear = null;
        if (fields != null) {
            Long maybeMadeYear = getLongFromDocument(fields, "made_year");
            if (maybeMadeYear != null) {
                // if within int range, store as Integer
                if (maybeMadeYear >= Integer.MIN_VALUE && maybeMadeYear <= Integer.MAX_VALUE) {
                    madeYear = maybeMadeYear.intValue();
                } else {
                    // keep null or log; storing as integer column in schema - log overflow
                    log.warn("made_year out of integer range for mongo_id={} value={}", mongoId, maybeMadeYear);
                }
            }
        }

        // Transform exclusive badges
        Object exclusiveBadgeObj = (fields != null) ? fields.get("exclusive_badge") : null;
        String exclusiveBadgesJson = transformExclusiveBadges(exclusiveBadgeObj); // returns JSON array string like "[]"

        try {
            long contentId = upsertContent(pgConn, mongoId, title, description, spot, madeYear, contentType, exclusiveBadgesJson);

            // Handle images - traverse document and collect any nodes which are image-type documents
            List<ImageRef> imageRefs = new ArrayList<>();
            if (fields != null) {
                flattenCollectImages(fields, "fields", imageRefs);
            }
            // Insert content_images relation for each found image mapped by filename -> images.id
            for (ImageRef ir : imageRefs) {
                Integer imageId = findImageIdByFilename(pgConn, ir.filename);
                if (imageId == null) {
                    log.warn("Missing image mapping. mongo_id={}, jsonPath={}, filename={}", mongoId, ir.path, ir.filename);
                } else {
                    insertRelation(pgConn, "content_images", "content_id", "image_id", contentId, imageId.longValue());
                }
            }

            // Handle lookup relations arrays: genre, badges, exclusive_badge, parental-guide, age-restriction, category
            if (fields != null) {
                handleLookupArrayField(pgConn, contentId, fields, "genre", "genre");
                handleLookupArrayField(pgConn, contentId, fields, "badges", "badges");
                handleLookupArrayField(pgConn, contentId, fields, "exclusive_badge", "badges"); // exclusive badge mapping to lookup_objects type 'badges' assumed
                handleLookupArrayField(pgConn, contentId, fields, "parental-guide", "parental-guide");
                handleLookupArrayField(pgConn, contentId, fields, "age-restriction", "age-restriction");
                handleLookupArrayField(pgConn, contentId, fields, "category", "category");
            }

            // Optionally other fields to sync can be added here
            log.info("Processed content mongo_id={} as contents.id={}", mongoId, contentId);

        } catch (SQLException e) {
            throw new RuntimeException("SQL error while processing doc _id=" + mongoId + " : " + e.getMessage(), e);
        }
    }

    private static String extractMongoIdAsString(Document doc) {
        if (doc == null) return null;
        Object idObj = doc.get("_id");
        if (idObj instanceof ObjectId) {
            return ((ObjectId) idObj).toHexString();
        } else {
            return String.valueOf(idObj);
        }
    }

    /**
     * Upsert content: if a contents row with same mongo_id exists returns existing id.
     * Otherwise inserts and returns generated id.
     */
    private static long upsertContent(Connection conn, String mongoId, String title, String description,
                                      String spot, Integer madeYear, String contentType, String exclusiveBadgesJson)
            throws SQLException {

        // 1) check existing
        String selectSql = "SELECT id FROM contents WHERE mongo_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, mongoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }

        // 2) insert new
        String insertSql = "INSERT INTO contents (mongo_id, title, description, spot, made_year, content_type, exclusive_badges, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, now(), now()) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, mongoId);
            setNullableString(ps, 2, title);
            setNullableString(ps, 3, description);
            setNullableString(ps, 4, spot);
            if (madeYear != null) {
                ps.setInt(5, madeYear);
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            setNullableString(ps, 6, contentType);
            if (exclusiveBadgesJson != null) {
                ps.setString(7, exclusiveBadgesJson);
            } else {
                ps.setString(7, "[]");
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                } else {
                    throw new SQLException("INSERT returned no id for content mongo_id=" + mongoId);
                }
            }
        }
    }

    private static void setNullableString(PreparedStatement ps, int paramIndex, String value) throws SQLException {
        if (value == null) ps.setNull(paramIndex, Types.VARCHAR);
        else ps.setString(paramIndex, value);
    }

    /**
     * Find image id by filename in images table.
     */
    private static Integer findImageIdByFilename(Connection conn, String filename) {
        if (filename == null) return null;
        String sql = "SELECT id FROM images WHERE filename = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filename);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            log.warn("Error querying images by filename '{}': {}", filename, e.toString());
        }
        return null;
    }

    /**
     * Handle a lookup array field under `fields` document.
     * fieldName is key inside `fields` (e.g., "badges"), expectedType is the lookup_objects.type value if known.
     * If expectedType is null, matches by typeid only.
     */
    private static void handleLookupArrayField(Connection conn, long contentId, Document fields, String fieldName, String expectedType) {
        Object arrObj = fields.get(fieldName);
        if (arrObj == null) return;
        if (!(arrObj instanceof List)) {
            log.warn("Expected array for fields.{} but got {}. Path=fields.{}", fieldName, arrObj == null ? "null" : arrObj.getClass().getSimpleName(), fieldName);
            return;
        }
        @SuppressWarnings("unchecked")
        List<Object> arr = (List<Object>) arrObj;
        int idx = 0;
        for (Object item : arr) {
            idx++;
            try {
                Long lookupTypeId = extractContentIdNumberLong(item);
                if (lookupTypeId == null) {
                    log.warn("Missing contentId.$numberLong in lookup array fields.{}[{}]. FullValue={} ", fieldName, idx - 1, item);
                    continue;
                }

                Long lookupId = findLookupObjectId(conn, lookupTypeId, expectedType);
                if (lookupId == null) {
                    log.warn("Missing lookup_objects record for typeid={} (fields.{}[{}])", lookupTypeId, fieldName, idx - 1);
                } else {
                    insertRelation(conn, "content_lookup_relations", "content_id", "lookup_object_id", contentId, lookupId);
                }
            } catch (Exception e) {
                log.warn("Error processing lookup array fields.{}[{}] : {}", fieldName, idx - 1, e.toString());
            }
        }
    }

    /**
     * Query lookup_objects by typeid (and optionally type).
     * Returns lookup_objects.id or null if not found.
     */
    private static Long findLookupObjectId(Connection conn, Long typeid, String expectedType) {
        String sql;
        if (expectedType != null) {
            sql = "SELECT id FROM lookup_objects WHERE typeid = ? AND type = ? LIMIT 1";
        } else {
            sql = "SELECT id FROM lookup_objects WHERE typeid = ? LIMIT 1";
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, typeid);
            if (expectedType != null) ps.setString(2, expectedType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (SQLException e) {
            log.warn("Error querying lookup_objects for typeid={} type={}: {}", typeid, expectedType, e.toString());
        }
        return null;
    }

    /**
     * Insert relation into one of the allowed relation tables.
     */
    private static void insertRelation(Connection conn, String table, String col1, String col2, Long id1, Long id2) {
        if (!ALLOWED_RELATION_TABLES.contains(table)) {
            throw new IllegalArgumentException("Table not allowed for insertRelation: " + table);
        }
        // Simple check for allowed column names
        if (!(("content_images".equals(table) && "content_id".equals(col1) && "image_id".equals(col2)) ||
                ("content_lookup_relations".equals(table) && "content_id".equals(col1) && "lookup_object_id".equals(col2)))) {
            throw new IllegalArgumentException("Columns not allowed for table: " + table + " " + col1 + "," + col2);
        }

        String sql;
        if ("content_images".equals(table)) {
            // add ON CONFLICT DO NOTHING to prevent duplicate pk errors
            sql = "INSERT INTO content_images (content_id, image_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        } else {
            sql = "INSERT INTO content_lookup_relations (content_id, lookup_object_id, relation_type, created_at) VALUES (?, ?, ?, now())";
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id1);
            ps.setLong(2, id2);
            if ("content_lookup_relations".equals(table)) {
                // use empty relation_type - caller may want to customize; default to 'related'
                ps.setString(3, "related");
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warn("Error inserting relation into {} for ids {} and {}: {}", table, id1, id2, e.toString());
        }
    }

    /**
     * Extracts nested contentId.$numberLong or contentId.number.$numberLong or direct number-like values.
     */
    private static Long extractContentIdNumberLong(Object item) {
        if (item == null) return null;
        if (item instanceof Number) {
            return ((Number) item).longValue();
        } else if (item instanceof Document) {
            Document d = (Document) item;
            // common patterns: contentId -> { "$numberLong": "123" } or contentId -> { "number": { "$numberLong": "123" } } or item has contentId document inside
            if (d.containsKey("contentId")) {
                Object cid = d.get("contentId");
                if (cid instanceof Document) {
                    Long v = getLongFromDocument((Document) cid, null);
                    if (v != null) return v;
                } else if (cid instanceof Number) {
                    return ((Number) cid).longValue();
                }
            }
            // or the document itself may be like {"$numberLong":"123"} or {"number": {"$numberLong":"123"}}
            Long v = getLongFromDocument(d, null);
            if (v != null) return v;
            // maybe "id" field
            if (d.containsKey("id")) {
                Object idVal = d.get("id");
                if (idVal instanceof Number) return ((Number) idVal).longValue();
                if (idVal instanceof Document) {
                    Long v2 = getLongFromDocument((Document) idVal, null);
                    if (v2 != null) return v2;
                }
            }
        } else if (item instanceof String) {
            try {
                return Long.parseLong((String) item);
            } catch (NumberFormatException ignore) {
            }
        }
        return null;
    }

    /**
     * Safely returns a typed value from a Document. If the document value is a Document
     * and caller asked for a scalar type (String/Integer/Long), this tries to parse
     * Extended JSON patterns like { "number": { "$numberLong": "2022" } } or { "$numberLong": "2022" }.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(Document doc, String key, Class<T> type) {
        if (doc == null) return null;
        Object val = doc.get(key);
        if (val == null) return null;

        // If requested Document
        if (Document.class.equals(type)) {
            if (val instanceof Document) return (T) val;
            // if value is Map-like, convert
            if (val instanceof Map) return (T) new Document((Map<String, Object>) val);
            return null;
        }

        if (String.class.equals(type)) {
            if (val instanceof String) return (T) val;
            if (val instanceof Number) return (T) String.valueOf(val);
            if (val instanceof Document) {
                // try to get nested string
                Document vdoc = (Document) val;
                // common property "value" or "text"
                if (vdoc.containsKey("value")) return (T) String.valueOf(vdoc.get("value"));
                if (vdoc.containsKey("text")) return (T) String.valueOf(vdoc.get("text"));
                // fallback to json
                return (T) vdoc.toJson();
            }
            return (T) String.valueOf(val);
        }

        if (Integer.class.equals(type) || Long.class.equals(type)) {
            if (val instanceof Number) {
                Number n = (Number) val;
                if (Integer.class.equals(type)) return (T) Integer.valueOf(n.intValue());
                else return (T) Long.valueOf(n.longValue());
            } else if (val instanceof Document) {
                Long l = getLongFromDocument((Document) val, null);
                if (l == null) return null;
                if (Integer.class.equals(type)) return (T) Integer.valueOf(l.intValue());
                else return (T) Long.valueOf(l);
            } else if (val instanceof String) {
                try {
                    if (Integer.class.equals(type)) return (T) Integer.valueOf(Integer.parseInt((String) val));
                    else return (T) Long.valueOf(Long.parseLong((String) val));
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        }

        // Fallback - try casting
        try {
            return (T) val;
        } catch (ClassCastException e) {
            log.warn("Type mismatch for key={} expecting={} actual={}", key, type.getSimpleName(), val.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Extracts a long from a Document that uses Extended JSON patterns:
     * - {"$numberLong": "2022"}
     * - {"number": {"$numberLong": "2022"}}
     * - direct numeric values
     *
     * If keyName != null, tries doc.get(keyName) first and then extracts.
     */
    private static Long getLongFromDocument(Document doc, String keyName) {
        if (doc == null) return null;
        Document target = doc;
        if (keyName != null) {
            Object o = doc.get(keyName);
            if (o == null) return null;
            if (o instanceof Document) target = (Document) o;
            else if (o instanceof Number) return ((Number) o).longValue();
            else if (o instanceof String) {
                try {
                    return Long.parseLong((String) o);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            } else {
                return null;
            }
        }
        // Try direct patterns on target
        if (target.containsKey("$numberLong")) {
            Object nl = target.get("$numberLong");
            if (nl instanceof String) {
                try {
                    return Long.parseLong((String) nl);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            } else if (nl instanceof Number) {
                return ((Number) nl).longValue();
            }
        }
        if (target.containsKey("number")) {
            Object numberNode = target.get("number");
            if (numberNode instanceof Document) {
                Document numDoc = (Document) numberNode;
                if (numDoc.containsKey("$numberLong")) {
                    Object nl = numDoc.get("$numberLong");
                    if (nl instanceof String) {
                        try {
                            return Long.parseLong((String) nl);
                        } catch (NumberFormatException ignored) {
                        }
                    } else if (nl instanceof Number) {
                        return ((Number) nl).longValue();
                    }
                }
            } else if (numberNode instanceof Number) {
                return ((Number) numberNode).longValue();
            } else if (numberNode instanceof String) {
                try {
                    return Long.parseLong((String) numberNode);
                } catch (NumberFormatException ignore) {
                }
            }
        }
        // If doc has direct numeric content (rare)
        for (Map.Entry<String, Object> e : doc.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Number) return ((Number) v).longValue();
        }
        return null;
    }

    /**
     * Transform exclusive_badge structure to a JSON array string suitable for exclusive_badges jsonb column.
     * Input might be List<Document> or Document or null.
     * Returns JSON string, e.g. '[{"exclusiveBadgeType":"originals"}]' or '[]'
     */
    private static String transformExclusiveBadges(Object exclusiveBadgeObj) {
        try {
            if (exclusiveBadgeObj == null) return "[]";
            List<Map<String, Object>> out = new ArrayList<>();
            if (exclusiveBadgeObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> lst = (List<Object>) exclusiveBadgeObj;
                for (Object it : lst) {
                    if (it instanceof Document) {
                        Document d = (Document) it;
                        // prefer exclusiveBadgeType or title
                        Object t = d.get("exclusiveBadgeType");
                        if (t == null) t = d.get("title");
                        Map<String, Object> kv = new HashMap<>();
                        kv.put("exclusiveBadgeType", t != null ? String.valueOf(t) : null);
                        out.add(kv);
                    } else if (it instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) it;
                        Object t = m.get("exclusiveBadgeType");
                        if (t == null) t = m.get("title");
                        Map<String, Object> kv = new HashMap<>();
                        kv.put("exclusiveBadgeType", t != null ? String.valueOf(t) : null);
                        out.add(kv);
                    }
                }
            } else if (exclusiveBadgeObj instanceof Document) {
                Document d = (Document) exclusiveBadgeObj;
                Object t = d.get("exclusiveBadgeType");
                if (t == null) t = d.get("title");
                Map<String, Object> kv = new HashMap<>();
                kv.put("exclusiveBadgeType", t != null ? String.valueOf(t) : null);
                out.add(kv);
            } else {
                // unknown form - return empty array
                return "[]";
            }
            // Convert to JSON string - use a small manual conversion to avoid adding extra deps
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (Map<String, Object> m : out) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{");
                boolean innerFirst = true;
                for (Map.Entry<String, Object> e : m.entrySet()) {
                    if (!innerFirst) sb.append(",");
                    innerFirst = false;
                    sb.append("\"").append(escapeJson(e.getKey())).append("\":");
                    Object v = e.getValue();
                    if (v == null) sb.append("null");
                    else {
                        sb.append("\"").append(escapeJson(String.valueOf(v))).append("\"");
                    }
                }
                sb.append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            log.warn("Error transforming exclusive_badge structure: {}", e.toString());
            return "[]";
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Recursively traverses a Document, collects any nested objects that represent images.
     * The heuristic: a Document with field "type" == "image" and a "filename" field.
     */
    private static void flattenCollectImages(Document doc, String pathPrefix, List<ImageRef> out) {
        if (doc == null) return;
        for (Map.Entry<String, Object> e : doc.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            String currentPath = pathPrefix + "." + key;
            if (val instanceof Document) {
                Document sub = (Document) val;
                Object typeVal = sub.get("type");
                if ("image".equals(typeVal) || "Image".equals(typeVal)) {
                    Object filenameObj = sub.get("filename");
                    String filename = filenameObj != null ? String.valueOf(filenameObj) : null;
                    out.add(new ImageRef(currentPath, filename));
                }
                // Sometimes an 'image' node might be inside an array or nested deeper
                flattenCollectImages(sub, currentPath, out);
            } else if (val instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> lst = (List<Object>) val;
                for (int i = 0; i < lst.size(); i++) {
                    Object item = lst.get(i);
                    String itemPath = currentPath + "[" + i + "]";
                    if (item instanceof Document) {
                        Document sub = (Document) item;
                        Object typeVal = sub.get("type");
                        if ("image".equals(typeVal) || "Image".equals(typeVal)) {
                            Object filenameObj = sub.get("filename");
                            String filename = filenameObj != null ? String.valueOf(filenameObj) : null;
                            out.add(new ImageRef(itemPath, filename));
                        }
                        flattenCollectImages(sub, itemPath, out);
                    }
                }
            }
        }
    }

    /**
     * Simple container for found images with their JSON path and filename.
     */
    private static class ImageRef {
        final String path;
        final String filename;

        ImageRef(String path, String filename) {
            this.path = path;
            this.filename = filename;
        }
    }
}
