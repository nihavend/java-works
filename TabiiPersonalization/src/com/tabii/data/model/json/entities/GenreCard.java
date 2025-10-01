package com.tabii.data.model.json.entities;

import java.util.List;

import com.tabii.data.model.json.entities.sub.Image;

//Class for Genre
public class GenreCard {
	private String contentType;
	private int id;
	private List<Image> images;
	private String title;

	// Getters and setters
	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return "Genre{" + "contentType='" + contentType + '\'' + ", id=" + id + ", images=" + images + ", title='"
				+ title + '\'' + '}';
	}
}