package com.tabii.flink.awssample;

import java.util.Properties;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tabii.flink.KafkaToS3;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class StreamingJobS3 {

	private static final Logger LOG = LoggerFactory.getLogger(KafkaToS3.class);

	// Name of the local JSON resource with the application properties in the same
	// format as they are received from the Amazon Managed Service for Apache Flink
	// runtime
	// private static final String LOCAL_APPLICATION_PROPERTIES_RESOURCE = "flink-application-properties-dev.json";

	// Create ObjectMapper instance to serialise POJOs into JSONs
	// private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static void init() {

		final Logger LOG = LoggerFactory.getLogger(KafkaToS3.class);

		// Get the Logback LoggerContext
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		// Optionally, set log level for Flink packages
		// Set log level programmatically for your class
		ch.qos.logback.classic.Logger appLogger = loggerContext.getLogger(KafkaToS3.class);
		appLogger.setLevel(Level.ERROR); // Change to DEBUG, INFO, WARN, ERROR, etc.

		// Optionally, set log level for Flink packages
		ch.qos.logback.classic.Logger flinkLogger = loggerContext.getLogger("org.apache.flink");
		flinkLogger.setLevel(Level.ERROR); // Reduce Flink noise

		// Optionally, set log level for Flink packages
		ch.qos.logback.classic.Logger kafkaLogger = loggerContext.getLogger("org.apache.kafka.clients");
		kafkaLogger.setLevel(Level.ERROR); // Reduce Flink noise

		// Test the logging levels
		LOG.debug("Debug: Starting Flink job configuration");
		LOG.info("Info: Starting Flink job in local environment");
		LOG.warn("Warn: This is a warning message");
		LOG.error("Error: This is an error message (just for testing)");
	}

	private static boolean isLocal(StreamExecutionEnvironment env) {
		return env instanceof LocalStreamEnvironment;
	}

	/**
	 * Load application properties from Amazon Managed Service for Apache Flink
	 * runtime or from a local resource, when the environment is local
	 */
//    private static Map<String, Properties> loadApplicationProperties(StreamExecutionEnvironment env) throws IOException {
//        if (isLocal(env)) {
//            LOGGER.info("Loading application properties from '{}'", LOCAL_APPLICATION_PROPERTIES_RESOURCE);
//            return KinesisAnalyticsRuntime.getApplicationProperties(
//                    StreamingJob.class.getClassLoader()
//                            .getResource(LOCAL_APPLICATION_PROPERTIES_RESOURCE).getPath());
//        } else {
//            LOGGER.info("Loading application properties from Amazon Managed Service for Apache Flink");
//            return KinesisAnalyticsRuntime.getApplicationProperties();
//        }
//    }

	/**
	 *
	 * @param s3SinkPath: Path to which application will write data to
	 * @return FileSink
	 */
	private static FileSink<String> S3Sink(String s3SinkPath) {
		return FileSink.forRowFormat(new Path(s3SinkPath), new SimpleStringEncoder<String>("UTF-8")).build();
	}

//    private static DataGeneratorSource<StockPrice> getStockPriceDataGeneratorSource() {
//        long recordPerSecond = 100;
//        return new DataGeneratorSource<>(
//                new StockPriceGeneratorFunction(),
//                Long.MAX_VALUE,
//                RateLimiterStrategy.perSecond(recordPerSecond),
//                TypeInformation.of(StockPrice.class));
//    }

	public static void main(String[] args) throws Exception {

		init();
		
		// Set up the streaming execution environment
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.setRuntimeMode(RuntimeExecutionMode.STREAMING);

		// Local dev specific settings
		if (isLocal(env)) {
			// Checkpointing and parallelism are set by Amazon Managed Service for Apache
			// Flink when running on AWS
			env.enableCheckpointing(60000);
			env.setParallelism(2);
		}

		// Retrieve bucket properties
		// Properties bucketProperties = loadApplicationProperties(env).get("bucket");
		Properties bucketProperties = new Properties();
		bucketProperties.setProperty("name", "int-avatar-test");
		LOG.info("Using bucket properties " + bucketProperties);
		String s3Bucket = Preconditions.checkNotNull(bucketProperties.getProperty("name"),
				"Target S3 bucket name not defined");

		// Source
		// DataGeneratorSource<StockPrice> source = getStockPriceDataGeneratorSource();

		KafkaSource<String> source = KafkaSource.<String>builder().setBootstrapServers("127.0.0.1:9092")
				.setTopics("test-flink-stream").setGroupId("flink_streamer")
				.setStartingOffsets(OffsetsInitializer.latest()).setValueOnlyDeserializer(new SimpleStringSchema())
				.build();
		DataStream<String> messageStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");
		// DataStream from Source
		// DataStream<StockPrice> kinesis = env.fromSource(source,
		// WatermarkStrategy.noWatermarks(), "data-generator").setParallelism(1);

		// Sink
		FileSink<String> sink = S3Sink(String.format("s3a://%s/flink/", s3Bucket));

		// DataStream to Sink
		// kinesis.map(OBJECT_MAPPER::writeValueAsString).uid("object-to-string-map").sinkTo(sink).uid("s3-sink");

		messageStream.sinkTo(sink); //.uid("s3-sink");

		env.execute("Flink S3 Streaming Sink Job");
	}
}