package com.tabii.utils;

public class PgProperties {
	
    String dbUrl;
    String dbUser;
	String dbPassword;

	public PgProperties(String dbUrl, String dbUser, String dbPassword) {
		super();
		this.dbUrl = dbUrl;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
	}

	public String getDbUrl() {
		return dbUrl;
	}

	public String getDbUser() {
		return dbUser;
	}

	public String getDbPassword() {
		return dbPassword;
	}
}
