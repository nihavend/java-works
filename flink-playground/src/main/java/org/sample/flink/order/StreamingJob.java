package org.sample.flink.order;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

public class StreamingJob {
	// private SourceFunction<Long> source;
	// private SinkFunction<Long> sink;

	public StreamingJob(SourceFunction<Long> source, SinkFunction<Long> sink) {
		// this.source = source;
		// this.sink = sink;
	}

	public StreamingJob() {
	}

	public void execute() throws Exception {
		// StreamExecutionEnvironment env =
		// StreamExecutionEnvironment.getExecutionEnvironment();
		Configuration conf = new Configuration();
		StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conf);

		KafkaSource<Order> kafkaSource = KafkaSource.<Order>builder()
				.setBootstrapServers("localhost:9092")
				.setTopics("flinktopic")
				.setGroupId("flinkgroup")
				.setStartingOffsets(OffsetsInitializer.latest())
				.setValueOnlyDeserializer(new KafkaOrderSchema()).build();

		DataStream<Order> stream = env.fromSource(
				kafkaSource
				,WatermarkStrategy
				.forBoundedOutOfOrderness(Duration.ofMinutes(1)), "Kafka Source").setParallelism(1);

		SerializableTimestampAssigner<Order> sz = new SerializableTimestampAssigner<Order>() {
			private static final long serialVersionUID = 1L;

			@Override
			public long extractTimestamp(Order order, long l) {
				try {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
					Date date = sdf.parse(order.eventTime);
					return date.getTime();
				} catch (ParseException e) {
					return 0;
				}
			}
		};
		
		WatermarkStrategy<Order> watermarkStrategy = WatermarkStrategy
				.<Order>forBoundedOutOfOrderness(Duration.ofMillis(100)).withTimestampAssigner(sz)
				.withIdleness(Duration.ofSeconds(10));

		DataStream<Order> watermarkDataStream = stream.assignTimestampsAndWatermarks(watermarkStrategy);
		
		
		@SuppressWarnings("deprecation")
		DataStream<OrderAgg> groupedData = watermarkDataStream.keyBy("userId")
				.window(TumblingProcessingTimeWindows.of(Time.milliseconds(2500), Time.milliseconds(500)))
				// .sum("priceAmount");
				.apply(new Avg());

		
		
		
		KafkaSink<OrderAgg> sink = KafkaSink.<OrderAgg>builder().setBootstrapServers("localhost:9092")
				.setRecordSerializer(new KafkaOrderSinkSchema("flinkout"))
				.setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE).build();

		groupedData.sinkTo(sink);

		env.execute();
	}

	public static void main(String[] args) throws Exception {
		StreamingJob job = new StreamingJob();
		job.execute();

	}
}