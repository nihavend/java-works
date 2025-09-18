package com.tabii.data.model.entities.sub;

import java.util.List;

//Class for Badge
public class Badge {
	private String bannerLocation;
	private int id;
	private List<Image> images;
	private String showLocation;
	private String title;
	private String type;

	// Getters and setters
	public String getBannerLocation() {
		return bannerLocation;
	}

	public void setBannerLocation(String bannerLocation) {
		this.bannerLocation = bannerLocation;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<Image> getImages() {
		return images;
	}

	public void setImages(List<Image> images) {
		this.images = images;
	}

	public String getShowLocation() {
		return showLocation;
	}

	public void setShowLocation(String showLocation) {
		this.showLocation = showLocation;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "Badge{" + "bannerLocation='" + bannerLocation + '\'' + ", id=" + id + ", images=" + images
				+ ", showLocation='" + showLocation + '\'' + ", title='" + title + '\'' + ", type='" + type + '\''
				+ '}';
	}
}