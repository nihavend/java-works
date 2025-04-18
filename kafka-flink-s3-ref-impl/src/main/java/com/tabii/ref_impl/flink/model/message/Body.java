package com.tabii.ref_impl.flink.model.message;

import java.io.Serializable;

public class Body implements Serializable {
	private static final long serialVersionUID = 1L;
	private String stringValue;

	public Body() {
	}

	public Body(String stringValue) {
		this.stringValue = stringValue;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}
}