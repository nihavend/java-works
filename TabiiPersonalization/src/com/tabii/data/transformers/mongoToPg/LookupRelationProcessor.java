package com.tabii.data.transformers.mongoToPg;

import org.bson.Document;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class LookupRelationProcessor {

    private static final Logger logger = Logger.getLogger(LookupRelationProcessor.class.getName());

    // List of node names to process
    private static final List<String> NODE_NAMES = Arrays.asList(
            "parental-guide", "age-restriction", "category", 
            "exclusive_badge", "badge", "genre", "badges"
    );

    /**
     * Process lookup relations for a given show document and insert into content_lookup_relations table
     */
    public static void processLookupRelations(Document show, Long contentId, Connection pgConn) throws SQLException {
        if (!show.containsKey("fields")) {
            logger.severe("Show document does not contain 'fields'");
            return;
        }
        
        Document fields = (Document) show.get("fields");

        for (String node : NODE_NAMES) {
            // Map exclusive_badge node to exclusive-badge in lookup_objects
            String lookupType = node.equals("exclusive_badge") ? "exclusive-badge" : node;

            Object nodeValue = fields.get(node);
            if (nodeValue == null) continue;

            if (nodeValue instanceof List) {
                List<Document> items = (List<Document>) nodeValue;
                for (Document item : items) {
                	Long lookupObjectId = item.getLong("contentId");
                    if(lookupObjectId==47) {
                    	System.out.println();
                    }
                	processNodeItem(item, contentId, lookupType, pgConn);
                }
            } else if (nodeValue instanceof Document) {
                processNodeItem((Document) nodeValue, contentId, lookupType, pgConn);
            } else {
                logger.warning("Unexpected type for node " + node);
            }
        }
    }

    private static void processNodeItem(Document item, Long contentId, String relationType, Connection pgConn) throws SQLException {
        Long lookupObjectId = item.getLong("contentId");
        if (lookupObjectId == null) {
            logger.severe("Node item missing contentId for relationType: " + relationType);
            System.exit(1);
        }
        // Verify lookup object exists
        if (!lookupObjectExists(lookupObjectId, relationType, pgConn)) {
            logger.warning("Content id of show=" + contentId + " Lookup id=" + lookupObjectId + ", type=" + relationType);
            // System.exit(1);
            return;
        }

        // Insert relation
        insertContentLookupRelation(contentId, lookupObjectId, relationType, pgConn);
    }

    private static boolean lookupObjectExists(Long id, String type, Connection pgConn) throws SQLException {
        String sql = "SELECT 1 FROM lookup_objects WHERE id = ? AND type = ?";
        try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void insertContentLookupRelation(Long contentId, Long lookupObjectId, String relationType, Connection pgConn) throws SQLException {
        String insertSql = "INSERT INTO content_lookup_relations(content_id, lookup_object_id, relation_type) VALUES (?, ?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = pgConn.prepareStatement(insertSql)) {
            ps.setLong(1, contentId);
            ps.setLong(2, lookupObjectId);
            ps.setString(3, relationType);
            ps.executeUpdate();
        }
    }

    /**
     * Test function to verify all lookup relations for a show
     */
    public static boolean verifyLookupRelations(Long contentId, Connection pgConn) throws SQLException {
        String sql = "SELECT clr.relation_type, clr.lookup_object_id, lo.id " +
                     "FROM content_lookup_relations clr " +
                     "JOIN lookup_objects lo ON clr.lookup_object_id = lo.id " +
                     "WHERE clr.content_id = ?";

        try (PreparedStatement ps = pgConn.prepareStatement(sql)) {
            ps.setLong(1, contentId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean allExist = true;
                while (rs.next()) {
                    Long lookupObjectId = rs.getLong("lookup_object_id");
                    String type = rs.getString("relation_type");
                    Long loId = rs.getLong("id");

                    if (!lookupObjectId.equals(loId)) {
                        logger.severe("Relation verification failed for contentId=" + contentId + ", lookupObjectId=" + lookupObjectId + ", type=" + type);
                        allExist = false;
                    }
                }
                return allExist;
            }
        }
    }
}
