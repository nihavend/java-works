package org.sample.flink.tabii.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

public class KafkaJsonProducer {
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final Random random = new Random();

	public static void main(String[] args) {
		// Kafka producer configuration
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(props);
		try {
			// Generate and send 10 random messages
			for (int i = 0; i < 20; i++) {
				ResourceLogsWrapper message = generateRandomMessage();
				String json = objectMapper.writeValueAsString(message);
				ProducerRecord<String, String> record = new ProducerRecord<String, String>("flinktopic", null, json);
				producer.send(record);
				System.out.println("Sent message: " + json);
				Thread.sleep(1000); // Wait 1 second between messages
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			producer.close();
		}
	}

	private static ResourceLogsWrapper generateRandomMessage() {
		// Generate random data
		List<Attribute> resourceAttributes = new ArrayList<Attribute>();
		resourceAttributes.add(new Attribute("commit", new Value(UUID.randomUUID().toString(), null, null)));
		resourceAttributes.add(new Attribute("deployment.environment", new Value(randomEnv(), null, null)));
		resourceAttributes.add(new Attribute("host.name", new Value(randomHost(), null, null)));
		resourceAttributes.add(new Attribute("process.command_args", new Value(null, null,
				new ArrayValue(generateList(new Value("/path/to/executable-" + random.nextInt(1000), null, null))))));
		resourceAttributes
				.add(new Attribute("process.executable.name", new Value("app-" + random.nextInt(100), null, null)));
		resourceAttributes.add(
				new Attribute("process.executable.path", new Value("/path/app-" + random.nextInt(1000), null, null)));
		resourceAttributes.add(new Attribute("process.owner", new Value("user-" + random.nextInt(100), null, null)));
		resourceAttributes
				.add(new Attribute("process.pid", new Value(null, String.valueOf(random.nextInt(20000)), null)));
		resourceAttributes.add(
				new Attribute("process.runtime.description", new Value("runtime v" + random.nextDouble(), null, null)));
		resourceAttributes.add(new Attribute("process.runtime.name", new Value("java", null, null)));
		resourceAttributes
				.add(new Attribute("process.runtime.version", new Value("1." + random.nextInt(20) + ".0", null, null)));
		resourceAttributes.add(new Attribute("service.instance.id", new Value(randomHost(), null, null)));
		resourceAttributes.add(new Attribute("service.name", new Value("service-" + random.nextInt(10), null, null)));
		resourceAttributes.add(new Attribute("service.version", new Value("v" + random.nextDouble(), null, null)));
		resourceAttributes.add(new Attribute("telemetry.sdk.language", new Value("java", null, null)));
		resourceAttributes.add(new Attribute("telemetry.sdk.name", new Value("opentelemetry", null, null)));
		resourceAttributes
				.add(new Attribute("telemetry.sdk.version", new Value("1." + random.nextInt(5) + ".0", null, null)));
		resourceAttributes.add(new Attribute("k8s.pod.ip",
				new Value("192.168." + random.nextInt(255) + "." + random.nextInt(255), null, null)));

		List<Attribute> logAttributes = new ArrayList<Attribute>();
		logAttributes.add(new Attribute("serviceName", new Value("service-" + random.nextInt(10), null, null)));
		logAttributes.add(new Attribute("hostname", new Value(randomHost(), null, null)));
		logAttributes.add(
				new Attribute("code.filepath", new Value("/app/code-" + random.nextInt(100) + ".java", null, null)));
		logAttributes.add(new Attribute("code.lineno", new Value(null, String.valueOf(random.nextInt(1000)), null)));
		logAttributes
				.add(new Attribute("code.function", new Value("com.example.Func" + random.nextInt(10), null, null)));
		logAttributes.add(new Attribute("language", new Value("en", null, null)));
		logAttributes.add(new Attribute("correlationId", new Value(UUID.randomUUID().toString(), null, null)));
		logAttributes.add(new Attribute("method", new Value(randomMethod(), null, null)));
		logAttributes.add(new Attribute("path", new Value("/api/v1/" + random.nextInt(100), null, null)));
		logAttributes.add(new Attribute("query", new Value("", null, null)));
		logAttributes.add(new Attribute("user-agent", new Value("Agent/" + random.nextInt(10), null, null)));
		logAttributes.add(new Attribute("request", new Value(randomRequest(), null, null)));
		logAttributes.add(new Attribute("idList",
				new Value(null, null, new ArrayValue(generateList(new Value("id-" + random.nextInt(1000), null, null),
						new Value("id-" + random.nextInt(1000), null, null))))));
		logAttributes.add(new Attribute("time", new Value(System.currentTimeMillis() + "Z", null, null)));

		LogRecord logRecord = new LogRecord(String.valueOf(System.nanoTime()),
				String.valueOf(System.nanoTime() + random.nextInt(10000)), 9, "info",
				new Body("Random log message " + random.nextInt(1000)), logAttributes, UUID.randomUUID().toString(),
				UUID.randomUUID().toString().substring(0, 8));

		ScopeLog scopeLog = new ScopeLog(new Scope("scope-" + random.nextInt(10)), generateList(logRecord));

		ResourceLog resourceLog = new ResourceLog(new Resource(resourceAttributes), generateList(scopeLog),
				"https://opentelemetry.io/schemas/1.26.0");

		return new ResourceLogsWrapper(generateList(resourceLog));
	}

	// Helper method to create Lists in Java 8 (replacing List.of)
	private static <T> List<T> generateList(T... elements) {
		List<T> list = new ArrayList<T>();
		for (T element : elements) {
			list.add(element);
		}
		return list;
	}

	private static String randomEnv() {
		String[] envs = { "dev", "test", "prod", "staging" };
		return envs[random.nextInt(envs.length)];
	}

	private static String randomHost() {
		return "host-" + random.nextInt(1000) + ".local";
	}

	private static String randomMethod() {
		String[] methods = { "GET", "POST", "PUT", "DELETE" };
		return methods[random.nextInt(methods.length)];
	}

	private static String randomRequest() {
		return randomMethod() + " /api/v1/test HTTP/1.1\r\nHost: localhost:" + (random.nextInt(10000) + 1000) + "\r\n";
	}
}

// Data classes
class ResourceLogsWrapper {
	private List<ResourceLog> resourceLogs;

	public ResourceLogsWrapper(List<ResourceLog> resourceLogs) {
		this.resourceLogs = resourceLogs;
	}

	public List<ResourceLog> getResourceLogs() {
		return resourceLogs;
	}

	public void setResourceLogs(List<ResourceLog> resourceLogs) {
		this.resourceLogs = resourceLogs;
	}
}

class ResourceLog {
	private Resource resource;
	private List<ScopeLog> scopeLogs;
	private String schemaUrl;

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

class Resource {
	private List<Attribute> attributes;

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

class Attribute {
	private String key;
	private Value value;

	public Attribute(String key, Value value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		this.value = value;
	}
}

class Value {
	private String stringValue;
	private String intValue;
	private ArrayValue arrayValue;

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

class ArrayValue {
	private List<Value> values;

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

class ScopeLog {
	private Scope scope;
	private List<LogRecord> logRecords;

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

class Scope {
	private String name;

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

class LogRecord {
	private String timeUnixNano;
	private String observedTimeUnixNano;
	private int severityNumber;
	private String severityText;
	private Body body;
	private List<Attribute> attributes;
	private String traceId;
	private String spanId;

	public LogRecord(String timeUnixNano, String observedTimeUnixNano, int severityNumber, String severityText,
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

	public int getSeverityNumber() {
		return severityNumber;
	}

	public void setSeverityNumber(int severityNumber) {
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

class Body {
	private String stringValue;

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