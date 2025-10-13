package old;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
// MongoToPostgresContentExporter.java
import com.tabii.utils.CommonUtils;
import com.tabii.utils.MongoProperties;
import com.tabii.utils.PgProperties;

/**
 * Migrates documents from MongoDB collection "shows" to PostgreSQL contents and related tables.
 *
 * Requirements implemented:
 *  - Uses CommonUtils.getMongoConnectionProps() and CommonUtils.getPgConnectionProps()
 *  - Reads all documents from mongo "shows"
 *  - Extracts fields safely, handles $numberLong, transforms exclusive_badge to exclusive_badges JSON
 *  - Maps images via images.filename -> images.id and inserts content_images
 *  - Inserts lookup relations into content_lookup_relations using lookup_objects.typeid
 *  - Logs missing images / lookup relations / unexpected structures
 *  - Uses PreparedStatements and handles nulls
 */
public class MongoToPostgresContentExporter {
    private static final Logger logger = LoggerFactory.getLogger(MongoToPostgresContentExporter.class);
    private static final JsonWriterSettings JSON_SETTINGS = JsonWriterSettings.builder().build();

    // Lookup relation array names under fields
    private static final List<String> LOOKUP_ARRAYS = Arrays.asList(
            "genre", "badges", "exclusive_badge", "parental-guide", "age-restriction", "category"
    );

    public static void main(String[] args) {
        MongoProperties mongoProperties = CommonUtils.getMongoConnectionProps();
        String mongoUri = mongoProperties.getMongoUri();
        String mongoDb = mongoProperties.getMongoDb();
        String mongoCollection = "shows";

        PgProperties pgProperties = CommonUtils.getPgConnectionProps();
        String pgUrl = pgProperties.getDbUrl();
        String pgUser = pgProperties.getDbUser();
        String pgPassword = pgProperties.getDbPassword();

        logger.info("Starting migration from MongoDB [{}] collection [{}] to Postgres [{}]", mongoDb, mongoCollection, pgUrl);

        // Mongo client
        try (MongoClient mongoClient = createMongoClient(mongoUri);
             Connection pgConn = DriverManager.getConnection(pgUrl, pgUser, pgPassword)) {

            pgConn.setAutoCommit(true); // operations mostly individual; you can adapt for batch/transaction

            MongoDatabase db = mongoClient.getDatabase(mongoDb);
            MongoCollection<Document> collection = db.getCollection(mongoCollection);

            // Prepare frequently used statements
            try (PreparedStatement findImageIdStmt = pgConn.prepareStatement("SELECT id FROM images WHERE filename = ?");
                 PreparedStatement findLookupIdStmt = pgConn.prepareStatement("SELECT id FROM lookup_objects WHERE id = ?")
            ) {
                // iterate all documents
                try (MongoCursor<Document> cursor = collection.find().iterator()) {
                    int count = 0;
                    while (cursor.hasNext()) {
                        Document showDoc = cursor.next();
                        count++;
                        try {
                            processShowDocument(showDoc, pgConn, findImageIdStmt, findLookupIdStmt);
                        } catch (Exception e) {
                            logger.warn("Failed to process Mongo document at index {} (mongo _id: {}): {}", count, showDoc.get("_id"), e.getMessage(), e);
                            // continue processing next documents
                        }
                    }
                    logger.info("Processed {} documents from Mongo collection '{}'.", count, mongoCollection);
                }
            }
        } catch (Exception e) {
            logger.error("Fatal error during migration: {}", e.getMessage(), e);
        }
    }

    private static MongoClient createMongoClient(String uri) {
        ConnectionString cs = new ConnectionString(uri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(cs)
                .build();
        return MongoClients.create(settings);
    }

    private static void processShowDocument(Document showDoc, Connection pgConn, PreparedStatement findImageIdStmt, PreparedStatement findLookupIdStmt) throws SQLException {
        // Extract core fields
        long id = Long.parseLong(showDoc.get("_id").toString());
        String title = getFieldValueAsString(showDoc, "title");
        String description = getFieldValueAsString(showDoc, "long_description");
        String contentType = getFieldValueAsString(showDoc, "type");

        // fields subdocument
        Document fieldsDoc = null;
        Object fieldsObj = showDoc.get("fields");
        if (fieldsObj instanceof Document) {
            fieldsDoc = (Document) fieldsObj;
        } else if (fieldsObj == null) {
            fieldsDoc = new Document();
            logger.debug("No 'fields' object for mongo id {}. Using empty Document.", id);
        } else {
            // unexpected structure
            logger.warn("Unexpected 'fields' type for mongo id {}: {}, path 'fields'", id, fieldsObj == null ? "null" : fieldsObj.getClass().getName());
            // try to coerce
            fieldsDoc = new Document("value", fieldsObj);
        }

        // spot might be nested under fields.spot.text
        String spot = null;
        Object spotVal = fieldsDoc.get("spot");
        if (spotVal instanceof Document) {
            // try nested text
            spot = getFieldValueAsString((Document) spotVal, "text");
            if (spot == null) {
                // maybe plain string inside spot
                spot = ((Document) spotVal).toJson(JSON_SETTINGS);
            }
        } else if (spotVal instanceof String) {
            spot = (String) spotVal;
        }

        // made_year
        Integer madeYear = null;
        Long madeYearLong = getLongFromDocument(fieldsDoc, "made_year");
        if (madeYearLong != null) madeYear = madeYearLong.intValue();

        // exclusive_badge transform
        Object exclusiveBadgeObj = null;
        if (fieldsDoc.containsKey("exclusive_badge")) {
            exclusiveBadgeObj = fieldsDoc.get("exclusive_badge");
        } else if (fieldsDoc.containsKey("exclusiveBadge")) {
            exclusiveBadgeObj = fieldsDoc.get("exclusiveBadge");
        }
        String exclusiveBadgesJson = transformExclusiveBadges(exclusiveBadgeObj);

        // Insert into contents and get generated content_id
        Long contentId = insertContent(pgConn, id, title, description, spot, madeYear, contentType, exclusiveBadgesJson);

        if (contentId == null) {
            // Could not insert (maybe unique conflict), try to fetch existing id
            contentId = getContentIdByMongoId(pgConn, id);
            if (contentId == null) {
                logger.warn("Could not create or fetch content for id {}. Skipping further processing for this document.", id);
                return;
            } else {
                logger.info("Found existing content.id={}", contentId);
            }
        }

        // Images: find any nested documents with type="image" and filename
        List<PathDoc> imageDocs = new ArrayList<>();
        flattenWithPaths(fieldsDoc, "fields", imageDocs);

        for (PathDoc pd : imageDocs) {
            Document doc = pd.doc;
            Object typeVal = doc.get("type");
            if (typeVal != null && "image".equals(typeVal.toString())) {
                // try to get filename
                String filename = null;
                if (doc.containsKey("filename")) filename = getFieldValueAsString(doc, "filename");
                // sometimes nested file document uses "file" or "url" or "image" fields
                if (filename == null && doc.containsKey("file")) {
                    Object fileObj = doc.get("file");
                    if (fileObj instanceof Document) {
                        filename = getFieldValueAsString((Document) fileObj, "filename");
                    } else if (fileObj instanceof String) {
                        filename = (String) fileObj;
                    }
                }
                if (filename == null && doc.containsKey("url")) {
                    // fallback to URL filename part
                    String url = getFieldValueAsString(doc, "url");
                    if (url != null) {
                        filename = extractFilenameFromUrl(url);
                    }
                }

                if (filename == null) {
                    logger.warn("Image document missing filename at JSON path '{}', doc: {}", pd.path, doc.toJson(JSON_SETTINGS));
                    continue;
                }

                Integer imageId = findImageId(findImageIdStmt, filename);
                if (imageId != null) {
                    // insert content_images
                    insertContentImage(pgConn, contentId, imageId);
                } else {
                    logger.warn("Missing image mapping for filename '{}' referenced at path '{}'. Document: {}", filename, pd.path, doc.toJson(JSON_SETTINGS));
                }
            }
        }

        // Lookup relations
        for (String arrName : LOOKUP_ARRAYS) {
            Object arrObj = fieldsDoc.get(arrName);
            if (arrObj instanceof List) {
                List<?> arr = (List<?>) arrObj;
                int idx = 0;
                for (Object item : arr) {
                    String itemPath = String.format("fields.%s[%d]", arrName, idx);
                    try {
                        Long relId = extractContentIdFromLookupItem(item);
                        if (relId == null) {
                            logger.warn("Missing contentId.$numberLong in lookup item at {} for content mongoId {}. Item: {}", itemPath, id, item);
                        } else {
                            Long lookupObjectId = findLookupObjectId(findLookupIdStmt, relId);
                            if (lookupObjectId != null) {
                                insertContentLookupRelation(pgConn, contentId, lookupObjectId, arrName);
                            } else {
                                logger.warn("Lookup object not found for typeid={} referenced at {} for content mongoId {}.", relId, itemPath, id);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Unexpected structure in lookup array '{}' at {} for mongoId {}: {}. Item: {}", arrName, itemPath, id, e.getMessage(), item, e);
                    }
                    idx++;
                }
            } else if (fieldsDoc.containsKey(arrName) && arrObj != null) {
                // present but not a list → unexpected
                logger.warn("Lookup field '{}' for mongoId {} is not an array (type {}). Content: {}", arrName, id, arrObj.getClass().getName(), arrObj);
            }
        }

    }

    // ---------- DB helper functions ----------

    private static Long insertContent(Connection conn, long id, String title, String description, String spot,
                                      Integer madeYear, String contentType, String exclusiveBadgesJson) {
        String sql = "INSERT INTO contents (id, title, description, spot, made_year, content_type, exclusive_badges) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            setNullableString(ps, 2, title);
            setNullableString(ps, 3, description);
            setNullableString(ps, 4, spot);
            if (madeYear != null) ps.setInt(5, madeYear); else ps.setNull(5, Types.INTEGER);
            setNullableString(ps, 6, contentType);
            if (exclusiveBadgesJson != null) {
                ps.setString(7, exclusiveBadgesJson);
            } else {
                ps.setNull(7, Types.VARCHAR); // cast in SQL to jsonb will handle null
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long iid = rs.getLong(1);
                    logger.debug("Inserted contents (id={})", iid);
                    return iid;
                }
            }
        } catch (SQLException e) {
            // Could be unique violation (id); log and return null so caller can try to fetch existing.
            logger.warn("Failed to insert content id={} : {}. Will attempt to fetch existing record.", id, e.getMessage());
            return null;
        }
        return null;
    }

    private static Long getContentIdByMongoId(Connection conn, Long mongoId) {
        String sql = "SELECT id FROM contents WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, mongoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.warn("Error fetching content id for id {}: {}", mongoId, e.getMessage(), e);
        }
        return null;
    }

    private static void insertContentImage(Connection conn, Long contentId, Integer imageId) {
        String sql = "INSERT INTO content_images (content_id, image_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, contentId);
            ps.setInt(2, imageId);
            ps.executeUpdate();
            logger.debug("Inserted content_images (content_id={}, image_id={})", contentId, imageId);
        } catch (SQLException e) {
            logger.warn("Failed to insert content_images (content_id={}, image_id={}): {}", contentId, imageId, e.getMessage());
        }
    }

    private static void insertContentLookupRelation(Connection conn, Long contentId, Long lookupObjectId, String relationType) {
        String sql = "INSERT INTO content_lookup_relations (content_id, lookup_object_id, relation_type) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, contentId);
            ps.setLong(2, lookupObjectId);
            ps.setString(3, relationType);
            ps.executeUpdate();
            logger.debug("Inserted content_lookup_relation (content_id={}, lookup_object_id={}, relation_type={})", contentId, lookupObjectId, relationType);
        } catch (SQLException e) {
            logger.warn("Failed to insert content_lookup_relations for content_id={}, lookup_object_id={}: {}", contentId, lookupObjectId, e.getMessage());
        }
    }

    // ---------- Query helpers ----------

    private static Integer findImageId(PreparedStatement findImageIdStmt, String filename) {
        try {
            findImageIdStmt.setString(1, filename);
            try (ResultSet rs = findImageIdStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.warn("Error finding image id for filename '{}': {}", filename, e.getMessage());
        }
        return null;
    }

    private static Long findLookupObjectId(PreparedStatement findLookupIdStmt, Long typeid) {
        try {
            findLookupIdStmt.setLong(1, typeid);
            try (ResultSet rs = findLookupIdStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.warn("Error finding lookup id for typeid {}: {}", typeid, e.getMessage());
        }
        return null;
    }

    // ---------- Utilities for Mongo field parsing ----------

    /**
     * Safely extracts a String value for key from doc.
     * If value is a nested Document with a "text" field, uses that.
     */
    private static String getFieldValueAsString(Document doc, String key) {
        if (doc == null) return null;
        Object val = doc.get(key);
        if (val == null) return null;
        if (val instanceof String) return (String) val;
        if (val instanceof Number) return String.valueOf(val);
        if (val instanceof Document) {
            Document d = (Document) val;
            // common patterns: { "text": "..." } or nested structures
            if (d.containsKey("text")) {
                Object t = d.get("text");
                return t == null ? null : t.toString();
            }
            if (d.containsKey("value")) {
                Object t = d.get("value");
                return t == null ? null : t.toString();
            }
            // fallback: JSON string
            return d.toJson(JSON_SETTINGS);
        }
        return val.toString();
    }

    /**
     * Attempts to read a Long out of Document key supporting extended JSON like:
     *   "made_year": { "number": { "$numberLong": "2022" } }
     * Or: "made_year": { "$numberLong": "2022" } or "made_year": 2022
     */
    private static Long getLongFromDocument(Document doc, String key) {
        if (doc == null) return null;
        Object val = doc.get(key);
        if (val == null) return null;
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val instanceof String) {
            try { return Long.parseLong((String) val); } catch (NumberFormatException ignored) {}
        }
        if (val instanceof Document) {
            Document d = (Document) val;
            // check for nested { "number": { "$numberLong": "2022" } }
            if (d.containsKey("number") && d.get("number") instanceof Document) {
                Document numberDoc = (Document) d.get("number");
                Object nl = numberDoc.get("$numberLong");
                if (nl == null) nl = numberDoc.get("number"); // defensive
                if (nl instanceof String) {
                    try { return Long.parseLong((String) nl); } catch (NumberFormatException ignored) {}
                } else if (nl instanceof Number) {
                    return ((Number) nl).longValue();
                }
            }
            // check for $numberLong at top
            Object topNumberLong = d.get("$numberLong");
            if (topNumberLong instanceof String) {
                try { return Long.parseLong((String) topNumberLong); } catch (NumberFormatException ignored) {}
            }
            // maybe nested as Document { "value": 2022 }, etc
            if (d.containsKey("value")) {
                Object v = d.get("value");
                if (v instanceof Number) return ((Number) v).longValue();
                if (v instanceof String) {
                    try { return Long.parseLong((String) v); } catch (NumberFormatException ignored) {}
                }
            }
        }
        return null;
    }

    /**
     * Transforms Mongo exclusive_badge structures to a JSON array string for Postgres jsonb column.
     * Accepts various input shapes: null, List<Document>, Document, String.
     * Returns JSON array string like: [{"exclusiveBadgeType":"originals"}] or "[]"
     */
    private static String transformExclusiveBadges(Object exclusiveBadgeObj) {
        if (exclusiveBadgeObj == null) {
            return "[]";
        }
        List<Document> normalized = new ArrayList<>();
        if (exclusiveBadgeObj instanceof List) {
            for (Object o : (List<?>) exclusiveBadgeObj) {
                if (o instanceof Document) normalized.add((Document) o);
                else {
                    // try to coerce
                    Document d = new Document("value", o);
                    normalized.add(d);
                }
            }
        } else if (exclusiveBadgeObj instanceof Document) {
            normalized.add((Document) exclusiveBadgeObj);
        } else if (exclusiveBadgeObj instanceof String) {
            // maybe a simple string like "originals"
            Document d = new Document("exclusiveBadgeType", exclusiveBadgeObj);
            normalized.add(d);
        } else {
            // unknown shape: wrap to JSON
            Document d = new Document("value", exclusiveBadgeObj);
            normalized.add(d);
        }

        // attempt to simplify badge documents to { "exclusiveBadgeType": "xxx" } if possible
        List<String> jsons = new ArrayList<>();
        for (Document d : normalized) {
            if (d.containsKey("exclusiveBadgeType")) {
                jsons.add(new Document("exclusiveBadgeType", d.get("exclusiveBadgeType")).toJson(JSON_SETTINGS));
            } else if (d.containsKey("title")) {
                jsons.add(new Document("exclusiveBadgeType", d.get("title")).toJson(JSON_SETTINGS));
            } else if (d.containsKey("key")) {
                jsons.add(new Document("exclusiveBadgeType", d.get("key")).toJson(JSON_SETTINGS));
            } else {
                // generic fallback - convert whole doc
                jsons.add(d.toJson(JSON_SETTINGS));
            }
        }

        // join into JSON array
        String arr = jsons.stream().collect(Collectors.joining(", ", "[", "]"));
        return arr;
    }

    /**
     * Recursively traverses a Document and collects nested Documents into results with their JSON path.
     * This helps finding nested image objects and logging full path for missing references.
     */
    private static void flattenWithPaths(Document doc, String basePath, List<PathDoc> results) {
        if (doc == null) return;
        results.add(new PathDoc(basePath, doc));
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            String curPath = basePath + "." + key;
            if (val instanceof Document) {
                flattenWithPaths((Document) val, curPath, results);
            } else if (val instanceof List) {
                List<?> list = (List<?>) val;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    String listPath = curPath + "[" + i + "]";
                    if (item instanceof Document) {
                        flattenWithPaths((Document) item, listPath, results);
                    } else {
                        // primitive inside list -> can log if needed
                    }
                }
            }
        }
    }

    // Extract contentId.$numberLong from lookup array item if possible
    private static Long extractContentIdFromLookupItem(Object item) {
        if (item == null) return null;
        if (item instanceof Document) {
            Document d = (Document) item;
            // common pattern: item.contentId.$numberLong or item.contentId -> Document -> $numberLong
            Object contentIdObj = d.get("contentId");
            if (contentIdObj instanceof Document) {
                Document cid = (Document) contentIdObj;
                Long l = getLongFromDocument(cid, "$numberLong");
                if (l != null) return l;
                // maybe cid contains $numberLong as string
                Object n = cid.get("$numberLong");
                if (n instanceof String) {
                    try { return Long.parseLong((String) n); } catch (NumberFormatException ignored) {}
                }
                // fallback: maybe contentId is a Document { "$numberLong": "131644" }
                Long fromCid = getLongFromDocument(d, "contentId");
                if (fromCid != null) return fromCid;
                // maybe contentId is simple number
                if (cid.get("number") instanceof Document) {
                    Long l2 = getLongFromDocument(cid, "number");
                    if (l2 != null) return l2;
                }
            } else if (contentIdObj instanceof Number) {
                return ((Number) contentIdObj).longValue();
            } else if (contentIdObj instanceof String) {
                try { return Long.parseLong((String) contentIdObj); } catch (NumberFormatException ignored) {}
            }
            // Some arrays may store id directly as {"$numberLong":"..."} in the item root
            Long alt = getLongFromDocument(d, "$numberLong");
            if (alt != null) return alt;
            // maybe item has "id" or "typeid"
            Long alt2 = getLongFromDocument(d, "id");
            if (alt2 != null) return alt2;
            Long alt3 = getLongFromDocument(d, "typeid");
            if (alt3 != null) return alt3;
        } else if (item instanceof Number) {
            return ((Number) item).longValue();
        } else if (item instanceof String) {
            try { return Long.parseLong((String) item); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    // Small helper to set string or null to PreparedStatement
    private static void setNullableString(PreparedStatement ps, int paramIndex, String value) throws SQLException {
        if (value == null) ps.setNull(paramIndex, Types.VARCHAR);
        else ps.setString(paramIndex, value);
    }

    // Extract filename from URL if present
    private static String extractFilenameFromUrl(String url) {
        if (url == null) return null;
        int qIdx = url.indexOf('?');
        if (qIdx >= 0) url = url.substring(0, qIdx);
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) return url.substring(lastSlash + 1);
        return url;
    }

    // ---------- Small helper classes ----------

    private static class PathDoc {
        final String path;
        final Document doc;

        PathDoc(String path, Document doc) {
            this.path = path;
            this.doc = doc;
        }
    }
}
