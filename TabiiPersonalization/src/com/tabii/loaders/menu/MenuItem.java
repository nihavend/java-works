package com.tabii.loaders.menu;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MenuItem {
	// "default" JSON alanı Java anahtar sözcüğü olduğu için isDefault ile eşliyoruz
	@JsonProperty("default")
	private boolean isDefault;

	private List<MenuImage> images;
	private List<String> targetContentTypes;
	private String targetId;
	private String title;
	private String universalUrl;
	private String viewType;
	private List<String> targetCategoryIds;

	// Getters / Setters
	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean aDefault) {
		isDefault = aDefault;
	}

	public List<MenuImage> getImages() {
		return images;
	}

	public void setImages(List<MenuImage> images) {
		this.images = images;
	}

	public List<String> getTargetContentTypes() {
		return targetContentTypes;
	}

	public void setTargetContentTypes(List<String> targetContentTypes) {
		this.targetContentTypes = targetContentTypes;
	}

	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUniversalUrl() {
		return universalUrl;
	}

	public void setUniversalUrl(String universalUrl) {
		this.universalUrl = universalUrl;
	}

	public String getViewType() {
		return viewType;
	}

	public void setViewType(String viewType) {
		this.viewType = viewType;
	}

	public List<String> getTargetCategoryIds() {
		return targetCategoryIds;
	}

	public void setTargetCategoryIds(List<String> targetCategoryIds) {
		this.targetCategoryIds = targetCategoryIds;
	}
}