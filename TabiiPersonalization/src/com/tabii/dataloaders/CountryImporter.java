package com.tabii.dataloaders;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.tabii.utils.CommonUtils;

public class CountryImporter {

	static class CountryData {
		String name;
		String alpha2;
		String alpha3;
		String numeric;
		String iso3166_2;

		public CountryData(String name, String alpha2, String alpha3, String numeric, String iso3166_2) {
			this.name = name;
			this.alpha2 = alpha2;
			this.alpha3 = alpha3;
			this.numeric = numeric;
			this.iso3166_2 = iso3166_2;
		}
	}

	public static void main(String[] args) {
		
	    Properties dbProps = CommonUtils.loadDbProperties();
	    String dbUrl = dbProps.getProperty("pg.db.url");
	    String dbUser = dbProps.getProperty("pg.db.user");
	    String dbPassword = dbProps.getProperty("pg.db.password");
		
	    String dataFilePath = dbProps.getProperty("pg.db.datafilepath");
		
		String continentCountryFile = dataFilePath + "countries-continents.csv";
		String isoDataFile = dataFilePath + "iso-country-codes.csv";

		Map<String, String> countryToContinent = loadCountryContinentMap(continentCountryFile);
		Map<String, CountryData> countryDetails = loadISOCountries(isoDataFile);

		List<String> missingCountries = new ArrayList<>();
		List<String> missingContinents = new ArrayList<>();
		List<String> skippedDuplicates = new ArrayList<>();

		if (countryToContinent.isEmpty() || countryDetails.isEmpty()) {
			System.out.println("CSV files not properly loaded.");
			return;
		}

		try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
			conn.setAutoCommit(false);

			Map<String, Integer> continentNameToId = fetchContinentIds(conn);

			for (Map.Entry<String, String> entry : countryToContinent.entrySet()) {
				String countryName = entry.getKey();
				String continentName = entry.getValue();

				CountryData data = countryDetails.get(countryName);
				if (data == null) {
					missingCountries.add(countryName);
					continue;
				}

				Integer continentId = continentNameToId.get(continentName);
				if (continentId == null) {
					missingContinents.add(continentName + " (for country: " + countryName + ")");
					continue;
				}

				// Check for duplicates
				if (countryExists(conn, data.name, data.alpha2)) {
					skippedDuplicates.add(data.name);
					continue;
				}

				insertCountry(conn, data, continentId);
			}

			conn.commit();

			System.out.println("✅ Import complete.");
			System.out.println("Inserted countries: " + (countryToContinent.size() - skippedDuplicates.size()
					- missingCountries.size() - missingContinents.size()));
			System.out.println("⚠️ Duplicates skipped: " + skippedDuplicates.size());
			System.out.println("❌ Missing countries: " + missingCountries.size());
			System.out.println("❌ Missing continents: " + missingContinents.size());

			logList("missing_countries.log", missingCountries);
			logList("missing_continents.log", missingContinents);
			logList("duplicate_countries_skipped.log", skippedDuplicates);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Map<String, String> loadCountryContinentMap(String file) {
		Map<String, String> map = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			boolean header = true;
			while ((line = br.readLine()) != null) {
				if (header) {
					header = false;
					continue;
				}
				String[] parts = line.split(",", -1);
				if (parts.length != 2)
					continue;
				String continent = parts[0].trim();
				String country = parts[1].trim();
				map.put(country, continent);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}

	private static Map<String, CountryData> loadISOCountries(String file) {
		Map<String, CountryData> map = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			boolean header = true;
			while ((line = br.readLine()) != null) {
				if (header) {
					header = false;
					continue;
				}
				String[] parts = line.split(",", -1);
				if (parts.length < 5)
					continue;

				String name = parts[0].trim();
				String alpha2 = parts[1].trim();
				String alpha3 = parts[2].trim();
				String numeric = parts[3].trim();
				String iso = parts[4].trim();

				map.put(name, new CountryData(name, alpha2, alpha3, numeric, iso));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}

	private static Map<String, Integer> fetchContinentIds(Connection conn) throws SQLException {
		Map<String, Integer> map = new HashMap<>();
		String query = "SELECT id, name FROM continents";
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
			while (rs.next()) {
				map.put(rs.getString("name").trim(), (Integer) rs.getObject("id"));
			}
		}
		return map;
	}

	private static boolean countryExists(Connection conn, String name, String alpha2) throws SQLException {
		String sql = "SELECT 1 FROM countries WHERE name = ? OR alpha_2_code = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, name);
			ps.setString(2, alpha2);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next(); // True if any row exists
			}
		}
	}

	private static void insertCountry(Connection conn, CountryData data, Integer continentId) throws SQLException {
		String sql = """
				    INSERT INTO countries (
				        name, alpha_2_code, alpha_3_code, numeric_code, iso_3166_2,
				        continent_id, modified_user, modified_date, created_user, created_date
				    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			String user = "system";
			Timestamp now = new Timestamp(System.currentTimeMillis());

			ps.setString(1, data.name);
			ps.setString(2, data.alpha2);
			ps.setString(3, data.alpha3);
			ps.setString(4, data.numeric);
			ps.setString(5, data.iso3166_2);
			ps.setObject(6, continentId);
			ps.setString(7, user);
			ps.setTimestamp(8, now);
			ps.setString(9, user);

			ps.executeUpdate();
		}
	}

	private static void logList(String fileName, List<String> lines) {
		if (lines.isEmpty())
			return;
		try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
			for (String line : lines) {
				writer.println(line);
			}
			System.out.println("Logged to: " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



}
