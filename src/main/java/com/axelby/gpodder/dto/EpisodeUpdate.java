package com.axelby.gpodder.dto;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class EpisodeUpdate {
	private static final DateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

	static {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		ISO8601.setTimeZone(tz);
	}

	public String podcast;
	public String episode;
	public String device;
	public String action;
	public String timestamp;
	public Integer started;
	public Integer position;
	public Integer total;

	public EpisodeUpdate() { }

	public EpisodeUpdate(String podcast, String episode, String device, String action, Date timestamp, int position) {
		this.podcast = podcast;
		this.episode = episode;
		this.action = action;
		this.timestamp = ISO8601.format(timestamp);
		this.position = position;
		this.device = device;
	}

}
