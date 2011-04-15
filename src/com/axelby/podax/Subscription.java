package com.axelby.podax;

import java.util.Date;

public class Subscription {
	int id;
	String title;
	String url;
	Date lastUpdate;
	
	public Subscription(int id, String title, String url, Date lastUpdate) {
		super();
		this.id = id;
		this.title = title;
		this.url = url;
		this.lastUpdate = lastUpdate;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
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
	public Date getLastUpdate() {
		return lastUpdate;
	}
	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	
	public String getDisplayTitle() {
		return this.title != null ? this.title : this.url;
	}
}
