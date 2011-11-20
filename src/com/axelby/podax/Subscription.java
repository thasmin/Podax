package com.axelby.podax;

import java.io.File;
import java.util.Date;

import android.os.Environment;

public class Subscription {
	int id;
	String title;
	String url;
	Date lastModified;
	Date lastUpdate;
	String eTag;
	String thumbnail;
	
	public Subscription(int id, String title, String url, Date lastModified, 
			Date lastUpdate, String eTag, String thumbnail) {
		super();
		this.id = id;
		this.title = title;
		this.url = url;
		this.lastModified = lastModified;
		this.lastUpdate = lastUpdate;
		this.eTag = eTag;
		this.thumbnail = thumbnail;
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
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
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
	public String getETag() {
		return eTag;
	}
	public void setETag(String eTag) {
		this.eTag = eTag;
	}
	public String getThumbnail() {
		return thumbnail;
	}
	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	public String getThumbnailFilename() {
		String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		String podaxDir = externalPath + "/Android/data/com.axelby.podax/files/";
		File podaxFile = new File(podaxDir);
		if (!podaxFile.exists())
			podaxFile.mkdirs();
		return podaxDir + "/" + getId() + ".jpg";
	}
}
