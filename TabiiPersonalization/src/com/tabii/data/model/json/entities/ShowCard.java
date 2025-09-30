package com.tabii.data.model.entities;

import java.util.List;

import com.tabii.data.model.entities.sub.Badge;
import com.tabii.data.model.entities.sub.ExclusiveBadge;
import com.tabii.data.model.entities.sub.Image;
import com.tabii.data.model.entities.sub.Media;

// Class representing the root JSON object
public class ShowCard {
	private List<Badge> badges;
	private String contentType;
	private String description;
	private List<ExclusiveBadge> exclusiveBadges;
	private boolean favorite;
	private List<GenreCard> genres;
	private int id;
	private List<Image> images;
	private int madeYear;
	private String spot;
	private String title;

	// Sadece ContinueWatching Row'da istenmiş anlamadım...
	private String ageRestriction;
	private int currentPosition;
	private int duration;
	private int episodeNumber;
	private int seasonNumber;
	
	// Sadece LiveStreamRow'da istenmiş anlamadım...
	private String loginStatus;
	private List<Media> media;
	private boolean seekable;
	private String slug;
	private int kantarId;
	
	// isteğe bağlı alanlar
	private Long targetId;
	
	// Getters and setters
	public List<Badge> getBadges() {
		return badges;
	}

	public void setBadges(List<Badge> badges) {
		this.badges = badges;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<ExclusiveBadge> getExclusiveBadges() {
		return exclusiveBadges;
	}

	public void setExclusiveBadges(List<ExclusiveBadge> exclusiveBadges) {
		this.exclusiveBadges = exclusiveBadges;
	}

	public boolean isFavorite() {
		return favorite;
	}

	public void setFavorite(boolean favorite) {
		this.favorite = favorite;
	}

	public List<GenreCard> getGenres() {
		return genres;
	}

	public void setGenres(List<GenreCard> genres) {
		this.genres = genres;
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

	public int getMadeYear() {
		return madeYear;
	}

	public void setMadeYear(int madeYear) {
		this.madeYear = madeYear;
	}

	public String getSpot() {
		return spot;
	}

	public void setSpot(String spot) {
		this.spot = spot;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAgeRestriction() {
		return ageRestriction;
	}

	public void setAgeRestriction(String ageRestriction) {
		this.ageRestriction = ageRestriction;
	}


	public int getCurrentPosition() {
		return currentPosition;
	}

	public void setCurrentPosition(int currentPosition) {
		this.currentPosition = currentPosition;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public int getEpisodeNumber() {
		return episodeNumber;
	}

	public void setEpisodeNumber(int episodeNumber) {
		this.episodeNumber = episodeNumber;
	}

	public int getSeasonNumber() {
		return seasonNumber;
	}

	public void setSeasonNumber(int seasonNumber) {
		this.seasonNumber = seasonNumber;
	}
	

	public String getLoginStatus() {
		return loginStatus;
	}

	public void setLoginStatus(String loginStatus) {
		this.loginStatus = loginStatus;
	}

	public List<Media> getMedia() {
		return media;
	}

	public void setMedia(List<Media> media) {
		this.media = media;
	}

	@Override
	public String toString() {
		return "ShowCard [badges=" + badges + ", contentType=" + contentType + ", description=" + description
				+ ", exclusiveBadges=" + exclusiveBadges + ", favorite=" + favorite + ", genres=" + genres + ", id="
				+ id + ", images=" + images + ", madeYear=" + madeYear + ", spot=" + spot + ", title=" + title
				+ ", ageRestriction=" + ageRestriction + ", currentPosition=" + currentPosition + ", duration="
				+ duration + ", episodeNumber=" + episodeNumber + ", seasonNumber=" + seasonNumber + ", loginStatus="
				+ loginStatus + ", media=" + media + ", seekable=" + seekable + ", slug=" + slug + "]";
	}

	public boolean isSeekable() {
		return seekable;
	}

	public void setSeekable(boolean seekable) {
		this.seekable = seekable;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public int getKantarId() {
		return kantarId;
	}

	public void setKantarId(int kantarId) {
		this.kantarId = kantarId;
	}

	public Long getTargetId() {
		return targetId;
	}

	public void setTargetId(Long targetId) {
		this.targetId = targetId;
	}


}
