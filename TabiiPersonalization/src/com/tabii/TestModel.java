package com.tabii;

import java.util.ArrayList;

import com.tabii.data.model.Row;
import com.tabii.data.model.RowType;

public class TestModel {

	
	
	public static void main(String[] args) {
	
		
		ArrayList<Row> myHomePage = new ArrayList<Row>();
		
		
		Row row_1 = new Row(1, RowType.BANNER);
		Row row_2 = new Row(1, RowType.SHOW);
		Row row_3 = new Row(1, RowType.CONTINUE_WATCHING);
		Row row_4 = new Row(1, RowType.LIVE_STREAM);
		Row row_5 = new Row(1, RowType.SHOW);
		Row row_6 = new Row(1, RowType.SHOW);
		Row row_7 = new Row(1, RowType.SHOW);
		Row row_8 = new Row(1, RowType.SHOW);
		Row row_9 = new Row(1, RowType.SHOW);
		Row row_10 = new Row(1, RowType.LIVE_STREAM);
		Row row_11 = new Row(1, RowType.SHOW);
		Row row_12 = new Row(1, RowType.SHOW);
		Row row_13 = new Row(1, RowType.SPECIAL);
		Row row_14 = new Row(1, RowType.SHOW);
		Row row_15 = new Row(1, RowType.SHOW);
		Row row_16 = new Row(1, RowType.GENRE);
		
		
		myHomePage.add(row_1);
		myHomePage.add(row_2);
		myHomePage.add(row_3);
		myHomePage.add(row_4);
		myHomePage.add(row_5);
		myHomePage.add(row_6);
		myHomePage.add(row_7);
		myHomePage.add(row_8);
		myHomePage.add(row_9);
		myHomePage.add(row_10);
		myHomePage.add(row_11);
		myHomePage.add(row_12);
		myHomePage.add(row_13);
		myHomePage.add(row_14);
		myHomePage.add(row_15);
		myHomePage.add(row_16);
		
	}

	
}
