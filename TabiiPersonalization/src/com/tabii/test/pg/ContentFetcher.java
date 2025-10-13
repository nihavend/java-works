package com.tabii.test.pg;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.PgProperties;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContentFetcher {

	public static void main(String[] args) {
		PgProperties pgProperties = CommonUtils.getPgConnectionProps();

		String contentQuery = "SELECT id, made_year, spot, title, exclusive_badges, description, content_type FROM contents";

		try (Connection conn = DriverManager.getConnection(pgProperties.getDbUrl(), pgProperties.getDbUser(),
				pgProperties.getDbPassword());
				Statement stmt = conn.createStatement();
				ResultSet contentRs = stmt.executeQuery(contentQuery)) {

			while (contentRs.next()) {
				long contentId = contentRs.getLong("id");
				int madeYear = contentRs.getInt("made_year");
				String spot = contentRs.getString("spot");
				String title = contentRs.getString("title");
				String exclusiveBadges = contentRs.getString("exclusive_badges");
				String description = contentRs.getString("description");
				String contentType = contentRs.getString("content_type");

				// --- Fetch related images (id + filename) ---
				List<String> imageObjects = new ArrayList<>();
				String imageQuery = """
						SELECT i.id, i.filename
						FROM content_images ci
						JOIN images i ON ci.image_id = i.id
						WHERE ci.content_id = ?
						""";
				try (PreparedStatement ps = conn.prepareStatement(imageQuery)) {
					ps.setLong(1, contentId);
					ResultSet imageRs = ps.executeQuery();
					while (imageRs.next()) {
						long imageId = imageRs.getLong("id");
						String filename = imageRs.getString("filename");
						imageObjects.add("{\"id\": " + imageId + ", \"filename\": \"" + filename + "\"}");
					}
				}

				// --- Fetch lookup relations (badges + genres) ---
				List<Long> badgeIds = new ArrayList<>();
				List<Long> genreIds = new ArrayList<>();
				String lookupQuery = """
						SELECT clr.lookup_object_id, lo.type
						FROM content_lookup_relations clr
						JOIN lookup_objects lo ON clr.lookup_object_id = lo.id
						WHERE clr.content_id = ?
						""";
				try (PreparedStatement ps = conn.prepareStatement(lookupQuery)) {
					ps.setLong(1, contentId);
					ResultSet lookupRs = ps.executeQuery();
					while (lookupRs.next()) {
						long lookupId = lookupRs.getLong("lookup_object_id");
						String type = lookupRs.getString("type");
						if ("badges".equalsIgnoreCase(type)) {
							badgeIds.add(lookupId);
						} else if ("genre".equalsIgnoreCase(type)) {
							genreIds.add(lookupId);
						}
					}
				}

				// --- Print output ---
				System.out.println("{");
				System.out.println("  \"id\": " + contentId + ",");
				System.out.println("  \"images\": " + imageObjects + ",");
				System.out.println("  \"made_year\": " + madeYear + ",");
				System.out.println("  \"spot\": \"" + (spot != null ? spot.replace("\"", "\\\"") : "") + "\",");
				System.out.println("  \"title\": \"" + (title != null ? title.replace("\"", "\\\"") : "") + "\",");
				System.out.println("  \"exclusive_badges\": "
						+ (exclusiveBadges != null ? "\"" + exclusiveBadges.replace("\"", "\\\"") + "\"" : null) + ",");
				System.out.println("  \"description\": \""
						+ (description != null ? description.replace("\"", "\\\"") : "") + "\",");
				System.out.println("  \"content_type\": \""
						+ (contentType != null ? contentType.replace("\"", "\\\"") : "") + "\",");
				System.out.println("  \"badges\": " + badgeIds + ",");
				System.out.println("  \"genres\": " + genreIds + ",");
				System.out.println("  \"favorite\": false");
				System.out.println("}");
				System.out.println("------------------------------------------------");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
