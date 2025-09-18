package com.tabii.data.model;

import java.util.ArrayList;

import com.tabii.data.model.entities.sub.Image;

public class SpecialRow extends Row {

	public SpecialRow() {
		super();
	}

	private String title;
    private Long targetId;
    private String description;
    
	private ArrayList<Image> images;

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

	public ArrayList<Image> getImages() {
		return images;
	}

	public void setImages(ArrayList<Image> images) {
		this.images = images;
	}

	@Override
	public String toString() {
		return "SpecialRow [title=" + title + ", targetId=" + targetId + ", description=" + description + ", images="
				+ images + ", getContents()=" + getContents() + ", getId()=" + getId() + ", getRowType()="
				+ getRowType() + ", getClass()=" + getClass() + ", hashCode()=" + hashCode() + ", toString()="
				+ super.toString() + "]";
	}

	
	
}
