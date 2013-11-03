package com.axelby.gpodder;

import android.net.Uri;

public class ToplistPodcast {
	private Uri website;
	private String description;
	private String title;
	private String url;
	private String logoUrl;

	@Override
	public String toString() {
		return getTitle();
	}

	public Uri getWebsite() {
		return website;
	}
	public void setWebsite(Uri website) {
		this.website = website;
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}

	public String getLogoUrl() {
		return logoUrl;
	}
	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}
}
