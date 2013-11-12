package com.axelby.gpodder.dto;

import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class EpisodeUpdate {
	private final String podcast;
	private final String episode;
	private final String device;
	private final String action;
	private final Date timestamp;

	public EpisodeUpdate(String podcast, String episode, String device, String action, Date timestamp) {
		this.podcast = podcast;
		this.episode = episode;
		this.device = device;
		this.action = action;
		this.timestamp = timestamp;
	}

	public void writeJson(JsonWriter writer) throws IOException {
		writer.beginObject();
		writer.name("podcast").value(podcast);
		writer.name("episode").value(episode);
		writer.name("device").value(device);
		writer.name("action").value(action);

		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
		df.setTimeZone(tz);
		writer.name("timestamp").value(df.format(timestamp));
	}
}
