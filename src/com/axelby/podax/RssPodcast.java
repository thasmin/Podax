package com.axelby.podax;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RssPodcast implements Cloneable {
	private Subscription subscription;
	private String title;
	private String link;
	private Date pubDate;
	private String description;
	private String mediaUrl;
	
	static private SimpleDateFormat rssDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
	
	public RssPodcast(Subscription sub) {
		this.subscription = sub;
	}

	public Subscription getSubscription() {
		return subscription;
	}

	public void setSubscription(Subscription subscription) {
		this.subscription = subscription;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public Date getPubDate() {
		return pubDate;
	}

	public void setPubDate(String rssDate) {
		if (rssDate == null) {
			this.pubDate = null;
			return;
		}
		try {
			this.pubDate = rssDateFormat.parse(rssDate);
		} catch (ParseException e) {
			e.printStackTrace();
			this.pubDate = null;
		}
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getMediaUrl() {
		return mediaUrl;
	}

	public void setMediaUrl(String mediaUrl) {
		this.mediaUrl = mediaUrl;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
