package com.tabii.data.model.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tabii.data.model.entities.sub.Download;
import com.tabii.data.model.entities.sub.Player;

public class ProfileCard {

	@JsonProperty("PK")
	private String PK;
	
	@JsonProperty("SK")
	private String SK;
	
	private String avatar;
	private String countryCode;
	private String createdAt;
	private boolean current;
	private boolean defaultProfile;
	private Download download;
	private boolean kids;
	private String language;
	private String maturityLevel;

	private String modifiedAt;
	private String name;

	private Player player;

	public String getPK() {
		return PK;
	}

	public void setPK(String pK) {
		PK = pK;
	}

	public String getSK() {
		return SK;
	}

	public void setSK(String sK) {
		SK = sK;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public boolean isCurrent() {
		return current;
	}

	public void setCurrent(boolean current) {
		this.current = current;
	}

	public boolean isDefaultProfile() {
		return defaultProfile;
	}

	public void setDefaultProfile(boolean defaultProfile) {
		this.defaultProfile = defaultProfile;
	}

	public Download getDownload() {
		return download;
	}

	public void setDownload(Download download) {
		this.download = download;
	}

	public boolean isKids() {
		return kids;
	}

	public void setKids(boolean kids) {
		this.kids = kids;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getMaturityLevel() {
		return maturityLevel;
	}

	public void setMaturityLevel(String maturityLevel) {
		this.maturityLevel = maturityLevel;
	}

	public String getModifiedAt() {
		return modifiedAt;
	}

	public void setModifiedAt(String modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}
}
