package com.tabii.data.model.json;

import com.tabii.data.model.json.entities.sub.MetaData;

public class LiveStreamRow extends Row {
	
	
    public LiveStreamRow() {
		super();
	}
	
	private MetaData metaData;
	private String title;
    private String description;  
    private Long targetId;


	// Getters and setters
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
	
    public Long getTargetId() {
		return targetId;
	}
	public void setTargetId(Long targetId) {
		this.targetId = targetId;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	
	@Override
	public String toString() {
		return "LiveStreamRow [metaData=" + metaData + ", title=" + title + ", description=" + description
				+ ", targetId=" + targetId + ", getContents()=" + getContents() + ", getId()=" + getId()
				+ ", getRowType()=" + getRowType() + ", getClass()=" + getClass() + ", hashCode()=" + hashCode()
				+ ", toString()=" + super.toString() + "]";
	}
}


