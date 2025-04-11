package com.tabii.ref_impl.flink.model.message;

import java.io.Serializable;
import java.util.List;

public class LogWrapper implements Serializable {
	private static final long serialVersionUID = 1L;
	private List<ResourceLog> resourceLogs;

	public LogWrapper() {
	}

	public LogWrapper(List<ResourceLog> resourceLogs) {
		this.resourceLogs = resourceLogs;
	}

	public List<ResourceLog> getResourceLogs() {
		return resourceLogs;
	}

	public void setResourceLogs(List<ResourceLog> resourceLogs) {
		this.resourceLogs = resourceLogs;
	}
}














