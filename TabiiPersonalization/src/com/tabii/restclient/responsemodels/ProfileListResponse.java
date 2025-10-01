package com.tabii.restclient.responsemodels;

import java.util.List;

import com.tabii.data.model.json.entities.ProfileCard;

// Class representing the root JSON object
public class ProfileListResponse {

	private int count;
	private List<ProfileCard> data;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public List<ProfileCard> getData() {
		return data;
	}

	public void setData(List<ProfileCard> data) {
		this.data = data;
	}

}
