package com.tabii.flink;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.AvroParquetWriters;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.sink.filesystem.PartFileInfo;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.CheckpointRollingPolicy;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class KafkaToS3Parquet {

	static final Logger LOG = LoggerFactory.getLogger(KafkaToS3Parquet.class);

	public static void init() {


		// Get the Logback LoggerContext
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		// Optionally, set log level for Flink packages
		// Set log level programmatically for your class
		ch.qos.logback.classic.Logger appLogger = loggerContext.getLogger(KafkaToS3Parquet.class);
		appLogger.setLevel(Level.DEBUG); // Change to DEBUG, INFO, WARN, ERROR, etc.

		// Optionally, set log level for Flink packages
		ch.qos.logback.classic.Logger flinkLogger = loggerContext.getLogger("org.apache.flink");
		flinkLogger.setLevel(Level.ERROR); // Reduce Flink noise

		// Optionally, set log level for Flink packages
		ch.qos.logback.classic.Logger kafkaLogger = loggerContext.getLogger("org.apache.kafka.clients");
		kafkaLogger.setLevel(Level.ERROR); // Reduce Flink noise

		// Optionally, set log level for Hadoop packages
		ch.qos.logback.classic.Logger hadoopLogger = loggerContext.getLogger("org.apache.hadoop");
		hadoopLogger.setLevel(Level.ERROR); // Reduce Flink noise
		
		
		// Test the logging levels
		LOG.debug("Debug: Starting Flink job configuration");
		LOG.info("Info: Starting Flink job in local environment");
		LOG.warn("Warn: This is a warning message");
		LOG.error("Error: This is an error message (just for testing)");
	}

	private static String s3Bucket = "s3a://int-avatar-test/flink-parquet/";

	public static void main(String[] args) throws Exception {

		init();

		// KafkaToS3.print();

		KafkaToS3Parquet.sinkToS3();
	}

	public static void sinkToS3() throws Exception {

		StreamExecutionEnvironment see = StreamExecutionEnvironment.getExecutionEnvironment();
		see.setRuntimeMode(RuntimeExecutionMode.STREAMING);
		see.enableCheckpointing(60000);
		see.setParallelism(1);

		Configuration configs3 = new Configuration();
		configs3.set(CheckpointingOptions.CHECKPOINT_STORAGE, "filesystem");
		configs3.set(CheckpointingOptions.CHECKPOINTS_DIRECTORY, "s3://int-avatar-test/flink/checkpoint/");
		see.configure(configs3);

		KafkaSource<String> source = KafkaSource.<String>builder()
				.setBootstrapServers("127.0.0.1:9092")
				.setTopics("test-flink-stream")
				.setGroupId("flink_streamer")
				.setStartingOffsets(OffsetsInitializer.latest())
				.setValueOnlyDeserializer(new SimpleStringSchema())
				.build();

		// Define a data stream from Kafka
        DataStream<String> messageStream = see.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");
        
        // Transform the string data into a Tuple2<String, Long>
        DataStream<Tuple2<String, Long>> processedStream = messageStream
            .process(new ProcessFunction<String, Tuple2<String, Long>>() {
                private static final long serialVersionUID = 1L;

				@Override
                public void processElement(String value, Context ctx, Collector<Tuple2<String, Long>> out) {
                    Tuple2<String, Long> tuple = Tuple2.of(value, System.currentTimeMillis());
                    LOG.info("Processed message: {}", tuple);
                    out.collect(tuple);
                }
            });

     // Define Avro schema for Parquet
        String avroSchema = "{"
            + "\"type\":\"record\","
            + "\"name\":\"Message\","
            + "\"fields\":["
            + "{\"name\":\"content\",\"type\":\"string\"},"
            + "{\"name\":\"timestamp\",\"type\":\"long\"}"
            + "]}";
        Schema schema = new Schema.Parser().parse(avroSchema);
        
     // Configure FileSink for Parquet with CheckpointRollingPolicy
        final FileSink<GenericRecord> sink = FileSink
            .forBulkFormat(
                new Path(s3Bucket),
                AvroParquetWriters.forGenericRecord(schema)
            )
            .withBucketAssigner(new DateTimeBucketAssigner<>("yyyy-MM-dd--HH"))
            .withRollingPolicy(
                new CheckpointRollingPolicy<GenericRecord, String>() {
                    private static final long serialVersionUID = 1L;

					@Override
                    public boolean shouldRollOnCheckpoint(PartFileInfo<String> partFileInfo) {
						LOG.info("Rolling on checkpoint for part file: {}", partFileInfo.getBucketId());
                        return true;
                    }

                    @Override
                    public boolean shouldRollOnEvent(PartFileInfo<String> partFileInfo, GenericRecord event) {
                        // Optional: Add additional rolling conditions based on event
                        return false;
                    }

                    @Override
                    public boolean shouldRollOnProcessingTime(PartFileInfo<String> partFileInfo, long currentTime) {
                    	boolean shouldRoll = currentTime - partFileInfo.getCreationTime() >= 15 * 60 * 1000;
                        if (shouldRoll) {
                            LOG.info("Rolling on processing time for part file: {}", partFileInfo.getBucketId());
                        }
                        return shouldRoll;
                    }
                }
            )
            .build();
        
        // Transform Tuple2 to GenericRecord using MapFunction
        processedStream.map(new TupleToRecordMapper(schema)).sinkTo(sink);

		// messageStream.print();
        LOG.info("Starting Flink job execution");
		see.execute();
	}
	
    // MapFunction to convert Tuple2 to GenericRecord
    private static class TupleToRecordMapper implements MapFunction<Tuple2<String, Long>, GenericRecord> {
        private static final long serialVersionUID = 1L;
		private final Schema schema;

        public TupleToRecordMapper(Schema schema) {
            this.schema = schema;
        }

        @Override
        public GenericRecord map(Tuple2<String, Long> tuple) throws Exception {
            GenericRecord record = new GenericData.Record(schema);
            record.put("content", tuple.f0);
            record.put("timestamp", tuple.f1);
            return record;
        }
    }

}

