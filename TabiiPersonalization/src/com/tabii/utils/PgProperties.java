package com.tabii.utils;

public class PgProperties {

	String dbUrl;
	String dbUser;
	String dbPassword;
	String datafilepath;

	public PgProperties(String dbUrl, String dbUser, String dbPassword) {
		super();
		this.dbUrl = dbUrl;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
	}

	public PgProperties(String dbUrl, String dbUser, String dbPassword, String datafilepath) {
		super();
		this.dbUrl = dbUrl;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
		this.datafilepath = datafilepath;
	}

	public String getDatafilepath() {
		return datafilepath;
	}

	public void setDatafilepath(String datafilepath) {
		this.datafilepath = datafilepath;
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
