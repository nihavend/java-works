package org.sample.flink.tabii.model.message;

import java.io.Serializable;

public class Attribute implements Serializable {
	private static final long serialVersionUID = 1L;
	private String key;
	private Value value;

	public Attribute() {
	}

	public Attribute(String key, Value value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		this.value = value;
	}
}