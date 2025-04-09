package org.sample.flink.tabii.model;

import lombok.Data;
import java.util.List;

@Data
public class LogWrapperLombok {

	private List<ResourceLog> resourceLogs;

	@Data
	public class ResourceLog {
	    private Resource resource;
	    private List<ScopeLog> scopeLogs;
	    private String schemaUrl;
	}

	@Data
	public class Resource {
	    private List<Attribute> attributes;
	}

	@Data
	public class Attribute {
	    private String key;
	    private Value value;
	}

	@Data
	public class Value {
	    private String stringValue;
	    private String intValue;  // Using String since some numbers are too large for int/long
	    private ArrayValue arrayValue;
	}

	@Data
	public class ArrayValue {
	    private List<Value> values;
	}

	@Data
	public class ScopeLog {
	    private Scope scope;
	    private List<LogRecord> logRecords;
	}

	@Data
	public class Scope {
	    private String name;
	}

	@Data
	public class LogRecord {
	    private String timeUnixNano;
	    private String observedTimeUnixNano;
	    private Integer severityNumber;
	    private String severityText;
	    private Body body;
	    private List<Attribute> attributes;
	    private String traceId;
	    private String spanId;
	}

	@Data
	public class Body {
	    private String stringValue;
	}
}
