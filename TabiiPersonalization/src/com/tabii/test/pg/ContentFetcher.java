package com.tabii.test;
import java.sql.*;
import java.util.*;

public class ContentFetcher {

    public static void main(String[] args) {
        String jdbcUrl = "jdbc:mysql://localhost:3306/your_database";
        String dbUser = "your_user";
        String dbPass = "your_password";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {
            String contentQuery = "SELECT id, madeYear, spot, title, favorite, exclusiveBadges, description, contentType FROM contents";
            Statement stmt = conn.createStatement();
            ResultSet contentRs = stmt.executeQuery(contentQuery);

            while (contentRs.next()) {
                int contentId = contentRs.getInt("id");

                // Get image IDs
                List<Integer> imageIds = new ArrayList<>();
                PreparedStatement imgStmt = conn.prepareStatement("SELECT id FROM content_images WHERE content_id = ?");
                imgStmt.setInt(1, contentId);
                ResultSet imgRs = imgStmt.executeQuery();
                while (imgRs.next()) {
                    imageIds.add(imgRs.getInt("id"));
                }

                // Get badge and genre IDs
                List<Integer> badgeIds = new ArrayList<>();
                List<Integer> genreIds = new ArrayList<>();
                PreparedStatement relStmt = conn.prepareStatement(
                        "SELECT lor.lookup_object_id, lo.type FROM content_lookup_relations lor " +
                        "JOIN lookup_objects lo ON lor.lookup_object_id = lo.id WHERE lor.content_id = ?"
                );
                relStmt.setInt(1, contentId);
                ResultSet relRs = relStmt.executeQuery();
                while (relRs.next()) {
                    int lookupId = relRs.getInt("lookup_object_id");
                    String type = relRs.getString("type");
                    if ("badge".equalsIgnoreCase(type)) {
                        badgeIds.add(lookupId);
                    } else if ("genre".equalsIgnoreCase(type)) {
                        genreIds.add(lookupId);
                    }
                }

                // Print the content data
                System.out.println("Content ID: " + contentId);
                System.out.println("  Images: " + imageIds);
                System.out.println("  Made Year: " + contentRs.getInt("madeYear"));
                System.out.println("  Spot: " + contentRs.getString("spot"));
                System.out.println("  Title: " + contentRs.getString("title"));
                System.out.println("  Favorite: " + contentRs.getBoolean("favorite"));
                System.out.println("  Exclusive Badges: " + contentRs.getString("exclusiveBadges"));
                System.out.println("  Description: " + contentRs.getString("description"));
                System.out.println("  Content Type: " + contentRs.getString("contentType"));
                System.out.println("  Badges: " + badgeIds);
                System.out.println("  Genres: " + genreIds);
                System.out.println("--------------------------------------------------");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
