package org.sample.flink.tabii.model.message;

import java.io.Serializable;
import java.util.List;

public class ArrayValue implements Serializable {
	private static final long serialVersionUID = 1L;
	private List<Value> values;

	public ArrayValue() {
	}

	public ArrayValue(List<Value> values) {
		this.values = values;
	}

	public List<Value> getValues() {
		return values;
	}

	public void setValues(List<Value> values) {
		this.values = values;
	}
}