package com.tabii.data.model.json;

import com.tabii.data.model.json.entities.sub.MetaData;

public class ShowRow extends Row {

	public ShowRow() {
		super();
	}
	
	private MetaData metaData;
	private String title;

	// Getters & Setters
	public MetaData getMetaData() {
		return metaData;
	}

	public void setMetaData(MetaData metaData) {
		this.metaData = metaData;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	
	@Override
	public String toString() {
		return "ShowRow [metaData=" + metaData + ", title=" + title + ", getContents()=" + getContents() + ", getId()="
				+ getId() + ", getRowType()=" + getRowType() + ", getClass()=" + getClass() + ", hashCode()="
				+ hashCode() + ", toString()=" + super.toString() + "]";
	}
}
