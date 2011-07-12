package com.axelby.podax;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;

public class Podcast {
	private int id = -1;
	private Subscription subscription;
	private String title;
	private String link;
	private Date pubDate;
	private String description;
	private String mediaUrl;
	private Integer fileSize;
	private Integer queuePosition;
	private int lastPosition = 0;
	private int duration = 0;
	
	static private SimpleDateFormat rssDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
	
	public Podcast(Subscription subscription) {
		super();
		this.subscription = subscription;
	}
	
	public Podcast(int id, Subscription subscription, String title,
			String link, Date pubDate, String description, String mediaUrl,
			Integer fileSize, Integer queuePosition, int lastPosition, int duration) {
		super();
		this.id = id;
		this.subscription = subscription;
		this.title = title;
		this.link = link;
		this.pubDate = pubDate;
		this.description = description;
		this.mediaUrl = mediaUrl;
		this.fileSize = fileSize;
		this.queuePosition = queuePosition;
		this.lastPosition = lastPosition;
		this.duration = duration;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
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
	public void setPubDate(Date pubDate) {
		this.pubDate = pubDate;
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
	public Integer getFileSize() {
		return fileSize;
	}
	public void setFileSize(Integer fileSize) {
		this.fileSize = fileSize;
	}
	public Integer getQueuePosition() {
		return queuePosition;
	}
	public void setQueuePosition(Integer queuePosition) {
		this.queuePosition = queuePosition;
	}
	public int getLastPosition() {
		return lastPosition;
	}
	public void setLastPosition(int lastPosition) {
		this.lastPosition = lastPosition;
	}
	public int getDuration() {
		return duration;
	}
	public void setDuration(int duration) {
		this.duration = duration;
	}

	public static String getStoragePath() {
		String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		String podaxDir = externalPath + "/Android/data/com.axelby.podax/files/";
		File podaxFile = new File(podaxDir);
		if (!podaxFile.exists())
			podaxFile.mkdirs();
		return podaxDir;
	}

	public String getFilename() {
		// find extension (probably .mp3)
		String extension = ""; 
		int i = this.mediaUrl.lastIndexOf('.');
		if (i > 0) {
		    extension = this.mediaUrl.substring(i+1);
		}
		
		return getStoragePath() + Integer.toString(this.id) + "." + extension;
	}

	public boolean isDownloaded() {
		if (this.fileSize == null)
			return false;
		File file = new File(getFilename());
		return file.exists() && file.length() == this.fileSize && this.fileSize != 0;
	}
	
	public boolean needsDownload() {
		// make sure there's a file associated
		if (getMediaUrl() == null || getMediaUrl().length() == 0)
			return false;
		if (isDownloaded())
			return false;
		return true;
	}
}
