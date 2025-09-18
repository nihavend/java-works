package com.tabii.loaders.row;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabii.data.model.Queue;
import com.tabii.data.model.Row;

// Main class to load and parse JSON from a file
public class QueueJSONLoader {
	public static void main(String[] args) {
		// Path to the JSON file (adjust as needed)
		String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/home_page.json";

		// Create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			// Read JSON file and map to ContentList object
			Queue queue = objectMapper.readValue(new File(filePath), Queue.class);

			// Print the deserialized object
			System.out.println("Loaded data: " + queue);
			int counter = 0;
			// Example: Accessing specific fields
			System.out.println("Number of Contents: " + queue.getData().size());
			System.out.println("\ndata row types:");

			for (Object datas : queue.getData()) {
				System.out.println("data index : " + counter++);
//				switch (((Row)datas).getRowType()) {
//				case "banner" -> System.out.println(((BannerRow)datas).getRowType());
//				case CwRow c -> System.out.println(c.getRowType());
//				case GenreRow g -> System.out.println(g.getRowType());
//				case LiveStreamRow l -> System.out.println(l.getRowType());
//				case ShowRow s -> System.out.println(s.getRowType());
//				case SpecialRow sp -> System.out.println(sp.getRowType());
//				case null -> System.out.println("It's null");
//				default -> System.out.println("Unknown type");
//				}
//				;

				System.out.println(" - " + ((Row) datas).getId() + " (" + ((Row) datas).getRowType() + ")");
//				if(content.getGenres() != null) {
//					for (GenreCard genre : content.getGenres()) {
//						System.out.println("   * Genre: " + genre.getTitle());
//					}
//				}
			}

		} catch (IOException e) {
			System.err.println("Error reading JSON file: " + e.getMessage());
			e.printStackTrace();
		}
	}
}