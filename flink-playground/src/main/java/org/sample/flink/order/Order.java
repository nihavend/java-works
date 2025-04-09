package org.sample.flink.order;

//Json even that needs to be deserialised
public class Order {
	public String orderId;
	public String userId;
	public String eventTime;
	public Double priceAmount;
	public String userName;
}