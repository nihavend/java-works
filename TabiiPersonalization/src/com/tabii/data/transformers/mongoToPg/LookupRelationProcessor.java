package com.tabii.data.transformers.mongoToPg;
import org.bson.Document;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class LookupRelationProcessor {

    private static final Logger LOGGER = Logger.getLogger(LookupRelationProcessor.class.getName());

    // List of expected fields
    private static final List<String> NODE_NAMES = Arrays.asList(
            "parental-guide", "age-restriction", "category", "exclusive-badge",
            "badge", "genre", "badges"
    );

    public static void processLookupRelations(Document show, Long contentId, Connection pgConn) throws SQLException {
        if (show == null || contentId == null) {
            throw new IllegalArgumentException("Show document or contentId cannot be null");
        }

        Document fields = show.get("fields", Document.class);
        if (fields == null) {
            throw new IllegalArgumentException("Show document has no 'fields' node");
        }

        for (String nodeName : NODE_NAMES) {
            Object node = fields.get(nodeName);
            if (node == null) {
                // Try underscore alternative
                node = fields.get(nodeName.replace("-", "_"));
            }

            if (node == null) {
                LOGGER.severe("Missing required node: " + nodeName + " in show " + contentId);
                throw new IllegalStateException("Fatal: Required lookup node missing: " + nodeName);
            }

            List<Document> nodeItems = new ArrayList<>();
            if (node instanceof List) {
                ((List<?>) node).forEach(item -> {
                    if (item instanceof Document) nodeItems.add((Document) item);
                    else LOGGER.warning("Unexpected non-Document item in node " + nodeName);
                });
            } else if (node instanceof Document) {
                nodeItems.add((Document) node);
            } else {
                LOGGER.warning("Unexpected type for node " + nodeName + ": " + node.getClass().getSimpleName());
                continue;
            }

            for (Document item : nodeItems) {
                Long lookupObjectId = null;
                Object idObj = item.get("contentId");
                if (idObj instanceof Integer) {
                    lookupObjectId = ((Integer) idObj).longValue();
                } else if (idObj instanceof Long) {
                    lookupObjectId = (Long) idObj;
                } else if (idObj instanceof Double) {
                    lookupObjectId = ((Double) idObj).longValue();
                }

                if (lookupObjectId == null) {
                    LOGGER.severe("Missing contentId in node " + nodeName);
                    throw new IllegalStateException("Fatal: Node contentId missing: " + nodeName);
                }

                // Check existence in lookup_objects
                if (!lookupObjectExists(lookupObjectId, nodeName, pgConn)) {
                    LOGGER.severe("Lookup object not found in table: type=" + nodeName + ", id=" + lookupObjectId);
                    throw new IllegalStateException("Fatal: Lookup object missing: " + nodeName + " - " + lookupObjectId);
                }

                // Insert relation
                insertContentLookupRelation(contentId, lookupObjectId, nodeName, pgConn);
            }
        }

        LOGGER.info("All lookup relations processed successfully for contentId: " + contentId);
    }

    private static boolean lookupObjectExists(Long lookupObjectId, String type, Connection pgConn) throws SQLException {
        String sql = "SELECT 1 FROM lookup_objects WHERE id = ? AND type = ?";
        try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
            ps.setLong(1, lookupObjectId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void insertContentLookupRelation(Long contentId, Long lookupObjectId, String relationType, Connection pgConn) throws SQLException {
        String sql = "INSERT INTO content_lookup_relations (content_id, lookup_object_id, relation_type) VALUES (?, ?, ?)";
        try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
            ps.setLong(1, contentId);
            ps.setLong(2, lookupObjectId);
            ps.setString(3, relationType);
            ps.executeUpdate();
        }
    }

    /** 
     * Verifies that all expected relations exist for a given show contentId 
     */
    public static void verifyLookupRelations(Long contentId, Connection pgConn) throws SQLException {
        String sql = "SELECT relation_type, lookup_object_id FROM content_lookup_relations WHERE content_id = ?";
        Map<String, List<Long>> relationsMap = new HashMap<>();

        try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
            ps.setLong(1, contentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("relation_type");
                    Long lookupId = rs.getLong("lookup_object_id");
                    relationsMap.computeIfAbsent(type, k -> new ArrayList<>()).add(lookupId);
                }
            }
        }

        for (String nodeName : NODE_NAMES) {
            if (!relationsMap.containsKey(nodeName) && !relationsMap.containsKey(nodeName.replace("-", "_"))) {
                LOGGER.severe("Missing relation for node: " + nodeName + " in content_lookup_relations for contentId " + contentId);
                throw new IllegalStateException("Verification failed: missing relation " + nodeName);
            }
        }

        LOGGER.info("All expected relations exist for contentId: " + contentId);
    }
}
