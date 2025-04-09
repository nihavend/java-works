package org.sample.flink.tabii.model.message;

import java.io.Serializable;
import java.util.List;

public class LogRecord implements Serializable {
	private static final long serialVersionUID = 1L;
	private String timeUnixNano;
	private String observedTimeUnixNano;
	private Integer severityNumber;
	private String severityText;
	private Body body;
	private List<Attribute> attributes;
	private String traceId;
	private String spanId;

	public LogRecord() {
	}

	public LogRecord(String timeUnixNano, String observedTimeUnixNano, Integer severityNumber, String severityText,
			Body body, List<Attribute> attributes, String traceId, String spanId) {
		this.timeUnixNano = timeUnixNano;
		this.observedTimeUnixNano = observedTimeUnixNano;
		this.severityNumber = severityNumber;
		this.severityText = severityText;
		this.body = body;
		this.attributes = attributes;
		this.traceId = traceId;
		this.spanId = spanId;
	}

	public String getTimeUnixNano() {
		return timeUnixNano;
	}

	public void setTimeUnixNano(String timeUnixNano) {
		this.timeUnixNano = timeUnixNano;
	}

	public String getObservedTimeUnixNano() {
		return observedTimeUnixNano;
	}

	public void setObservedTimeUnixNano(String observedTimeUnixNano) {
		this.observedTimeUnixNano = observedTimeUnixNano;
	}

	public Integer getSeverityNumber() {
		return severityNumber;
	}

	public void setSeverityNumber(Integer severityNumber) {
		this.severityNumber = severityNumber;
	}

	public String getSeverityText() {
		return severityText;
	}

	public void setSeverityText(String severityText) {
		this.severityText = severityText;
	}

	public Body getBody() {
		return body;
	}

	public void setBody(Body body) {
		this.body = body;
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public String getSpanId() {
		return spanId;
	}

	public void setSpanId(String spanId) {
		this.spanId = spanId;
	}
}