package com.tabii.ref_impl.flink.model.message;

import java.io.Serializable;
import java.util.List;

public class Resource implements Serializable {
	private static final long serialVersionUID = 1L;
	private List<Attribute> attributes;

	public Resource() {
	}

	public Resource(List<Attribute> attributes) {
		this.attributes = attributes;
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}
}
