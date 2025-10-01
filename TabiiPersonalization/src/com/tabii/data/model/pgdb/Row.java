package com.tabii.data.model.pgdb;

public class Row {

	private int[] contents;
	private int id;
	private String rowType;

	public int[] getContents() {
		return contents;
	}

	public void setContents(int[] contents) {
		this.contents = contents;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getRowType() {
		return rowType;
	}

	public void setRowType(String rowType) {
		this.rowType = rowType;
	}
}
