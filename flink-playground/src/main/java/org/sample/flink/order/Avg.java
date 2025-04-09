package org.sample.flink.order;


import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

//Window function to calculate average 
public class Avg implements WindowFunction<Order, OrderAgg, Tuple, TimeWindow> {

	private static final long serialVersionUID = 1L;

	@Override
	public void apply(Tuple key, TimeWindow window, Iterable<Order> values, Collector<OrderAgg> out) {
		Double sum = 0.0;
		int count = 0;
		String userName = null;
		for (Order value : values) {
			sum += value.priceAmount;
			userName = value.userName;
			count++;
		}

		OrderAgg result = new OrderAgg();
		result.userName = userName;
		result.userId = key.getField(0).toString(); // may be there is a better way to get this value.Need to figure
													// out:)
		result.eventTime = window.getEnd();
		result.avgPriceAmount = (sum / count);
		out.collect(result);
	}



}