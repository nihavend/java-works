package com.tabii.utils;

public class MongoProperties {
	public MongoProperties(String pgUrl, String pgUser, String pgPass) {
		super();
		this.pgUrl = pgUrl;
		this.pgUser = pgUser;
		this.pgPass = pgPass;
	}

	String pgUrl;
	String pgUser;
	String pgPass;

	public String getPgUrl() {
		return pgUrl;
	}

	public String getPgUser() {
		return pgUser;
	}

	public String getPgPass() {
		return pgPass;
	}

}
