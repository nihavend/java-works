package com.tabii.loaders.row;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabii.data.model.ShowRow;
import com.tabii.data.model.entities.GenreCard;
import com.tabii.data.model.entities.ShowCard;

// Main class to load and parse JSON from a file
public class ShowRowJSONLoader {
	public static void main(String[] args) {
		// Path to the JSON file (adjust as needed)

		// String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/2.show_row.json";
		// String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/5.show_row.json";
		// String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/6.show_row.json";
		// String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/7.show_row.json";
		// String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/8.show_row.json";
		// String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/9.show_row.json";
		// String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/11.show_row.json";
		// String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/12.show_row.json";
		// String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/14.show_row.json";
		String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/15.show_row.json";
		
		// Create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			// Read JSON file and map to ContentList object
			ShowRow showRow = objectMapper.readValue(new File(filePath), ShowRow.class);

			// Print the deserialized object
			System.out.println("Loaded ContentList: " + showRow);

			// Example: Accessing specific fields
			System.out.println("\nRow Type: " + showRow.getRowType());
			System.out.println("ID: " + showRow.getId());
			System.out.println("Number of Contents: " + showRow.getContents().size());
			System.out.println("\nContent Titles and Genres:");
			for (ShowCard content : showRow.getContents()) {
				System.out.println(" - " + content.getTitle() + " (" + content.getContentType() + ")");
				if(content.getGenres() != null) {
					for (GenreCard genre : content.getGenres()) {
						System.out.println("   * Genre: " + genre.getTitle());
					}
				}
			}

		} catch (IOException e) {
			System.err.println("Error reading JSON file: " + e.getMessage());
			e.printStackTrace();
		}
	}
}