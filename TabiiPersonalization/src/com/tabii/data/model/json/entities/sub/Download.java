package com.tabii.data.model.json.entities.sub;

public class Download {
	private boolean onlyWifi;
	private String quality;

	public boolean isOnlyWifi() {
		return onlyWifi;
	}

	public void setOnlyWifi(boolean onlyWifi) {
		this.onlyWifi = onlyWifi;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}
}
