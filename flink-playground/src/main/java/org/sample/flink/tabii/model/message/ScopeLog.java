package org.sample.flink.tabii.model.message;

import java.io.Serializable;
import java.util.List;

public class ScopeLog implements Serializable {
	private static final long serialVersionUID = 1L;
	private Scope scope;
	private List<LogRecord> logRecords;

	public ScopeLog() {
	}

	public ScopeLog(Scope scope, List<LogRecord> logRecords) {
		this.scope = scope;
		this.logRecords = logRecords;
	}

	public Scope getScope() {
		return scope;
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}

	public List<LogRecord> getLogRecords() {
		return logRecords;
	}

	public void setLogRecords(List<LogRecord> logRecords) {
		this.logRecords = logRecords;
	}
}