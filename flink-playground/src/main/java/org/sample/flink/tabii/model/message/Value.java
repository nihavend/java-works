package org.sample.flink.tabii.model.message;

import java.io.Serializable;

public class Value implements Serializable {
	private static final long serialVersionUID = 1L;
	private String stringValue;
	private String intValue; // Kept as String to handle large numbers
	private ArrayValue arrayValue;

	public Value() {
	}

	public Value(String stringValue, String intValue, ArrayValue arrayValue) {
		this.stringValue = stringValue;
		this.intValue = intValue;
		this.arrayValue = arrayValue;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	public String getIntValue() {
		return intValue;
	}

	public void setIntValue(String intValue) {
		this.intValue = intValue;
	}

	public ArrayValue getArrayValue() {
		return arrayValue;
	}

	public void setArrayValue(ArrayValue arrayValue) {
		this.arrayValue = arrayValue;
	}
}