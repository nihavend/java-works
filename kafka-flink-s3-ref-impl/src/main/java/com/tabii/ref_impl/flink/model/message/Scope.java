package com.tabii.ref_impl.flink.model.message;

import java.io.Serializable;

public class Scope implements Serializable {
	private static final long serialVersionUID = 1L;
	private String name;

	public Scope() {
	}

	public Scope(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
