package com.tabii.ref_impl.flink.model.message;

import java.io.Serializable;
import java.util.List;

public class ResourceLog implements Serializable {
	private static final long serialVersionUID = 1L;
	private Resource resource;
	private List<ScopeLog> scopeLogs;
	private String schemaUrl;

	public ResourceLog() {
	}

	public ResourceLog(Resource resource, List<ScopeLog> scopeLogs, String schemaUrl) {
		this.resource = resource;
		this.scopeLogs = scopeLogs;
		this.schemaUrl = schemaUrl;
	}

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public List<ScopeLog> getScopeLogs() {
		return scopeLogs;
	}

	public void setScopeLogs(List<ScopeLog> scopeLogs) {
		this.scopeLogs = scopeLogs;
	}

	public String getSchemaUrl() {
		return schemaUrl;
	}

	public void setSchemaUrl(String schemaUrl) {
		this.schemaUrl = schemaUrl;
	}
}