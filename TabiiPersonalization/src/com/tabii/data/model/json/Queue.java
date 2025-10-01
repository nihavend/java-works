package com.tabii.data.model.json;

import java.util.List;

public class Queue {

	public Queue() {
		super();
	}

	private List<Row> data;

	// Getters and setters
	public List<Row> getData() {
		return data;
	}

	public void setData(List<Row> data) {
		this.data = data;
	}
}
