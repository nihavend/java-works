package com.tabii.data.model;

public class CwRow extends Row {

	public CwRow() {
		super();
	}

	private String title;

	// Getters & Setters
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	
	@Override
	public String toString() {
		return "CwRow [title=" + title + ", getContents()=" + getContents() + ", getId()=" + getId() + ", getRowType()="
				+ getRowType() + ", getClass()=" + getClass() + ", hashCode()=" + hashCode() + ", toString()="
				+ super.toString() + "]";
	}
}
