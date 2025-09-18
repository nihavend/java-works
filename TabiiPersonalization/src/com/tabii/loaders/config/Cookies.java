package com.tabii.loaders.config;

public class Cookies {

	private Cookie advertising_cookies;
	private Cookie cookie_preferences;
	private Cookie essential_cookies;

	// getters & setters
	public Cookie getAdvertising_cookies() {
		return advertising_cookies;
	}

	public void setAdvertising_cookies(Cookie advertising_cookies) {
		this.advertising_cookies = advertising_cookies;
	}

	public Cookie getCookie_preferences() {
		return cookie_preferences;
	}

	public void setCookie_preferences(Cookie cookie_preferences) {
		this.cookie_preferences = cookie_preferences;
	}

	public Cookie getEssential_cookies() {
		return essential_cookies;
	}

	public void setEssential_cookies(Cookie essential_cookies) {
		this.essential_cookies = essential_cookies;
	}
}