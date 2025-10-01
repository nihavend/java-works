package com.tabii.utils;

public class MongoProperties {

	public MongoProperties(String mongoUri, String mongoDb) {
		super();
		this.mongoUri = mongoUri;
		this.mongoDb = mongoDb;
	}

	String mongoUri;
	String mongoDb;

	public String getMongoUri() {
		return mongoUri;
	}

	public String getMongoDb() {
		return mongoDb;
	}

}
