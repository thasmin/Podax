package com.axelby.gpodder.dto;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class EpisodeUpdate {
	private static DateFormat ISO8601 = null;

	static {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		ISO8601.setTimeZone(tz);
	}

	private String podcast;
	private String episode;
	private String device;
	private String action;
	private Date timestamp;
	private Integer started;
	private Integer position;
	private Integer total;

	private EpisodeUpdate() {
	}

	public EpisodeUpdate(String podcast, String episode, String action, Date timestamp) {
		this.podcast = podcast;
		this.episode = episode;
		this.action = action;
		this.timestamp = timestamp;
	}

	public EpisodeUpdate(String podcast, String episode, String action, Date timestamp, int position) {
		this(podcast, episode, action, timestamp);
		this.position = position;
	}

	public EpisodeUpdate(String podcast, String episode, String action, Date timestamp, int started, int position, int total) {
		this(podcast, episode, action, timestamp, position);
		this.started = started;
		this.total = total;
	}

	public EpisodeUpdate(String podcast, String episode, String device, String action, Date timestamp) {
		this(podcast, episode, action, timestamp);
		this.device = device;
	}

	public EpisodeUpdate(String podcast, String episode, String device, String action, Date timestamp, int position) {
		this(podcast, episode, action, timestamp, position);
		this.device = device;
	}

	public EpisodeUpdate(String podcast, String episode, String device, String action, Date timestamp, int started, int position, int total) {
		this(podcast, episode, action, timestamp, started, position, total);
		this.device = device;
	}

	public static EpisodeUpdate readJson(JsonReader reader) throws IOException, ParseException {
		EpisodeUpdate update = new EpisodeUpdate();

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (reader.peek() == JsonToken.NULL)
				reader.skipValue();
			else if (name.equals("podcast"))
				update.podcast = reader.nextString();
			else if (name.equals("episode"))
				update.episode = reader.nextString();
			else if (name.equals("device"))
				update.device = reader.nextString();
			else if (name.equals("action"))
				update.action = reader.nextString();
			else if (name.equals("timestamp"))
				update.timestamp = ISO8601.parse(reader.nextString());
			else if (name.equals("started"))
				update.started = reader.nextInt();
			else if (name.equals("position"))
				update.position = reader.nextInt();
			else if (name.equals("total"))
				update.total = reader.nextInt();
			else
				reader.skipValue();
		}
		reader.endObject();

		return update;
	}

	public String getPodcast() {
		return podcast;
	}

	public String getEpisode() {
		return episode;
	}

	public String getDevice() {
		return device;
	}

	public String getAction() {
		return action;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public Integer getStarted() {
		return started;
	}

	public Integer getPosition() {
		return position;
	}

	public Integer getTotal() {
		return total;
	}

	public void writeJson(JsonWriter writer) throws IOException {
		writer.beginObject();
		writer.name("podcast").value(podcast);
		writer.name("episode").value(episode);
		if (device != null)
			writer.name("device").value(device);
		writer.name("action").value(action);
		writer.name("timestamp").value(ISO8601.format(timestamp));
		if (started != null)
			writer.name("started").value(started);
		if (position != null)
			writer.name("position").value(position);
		if (total != null)
			writer.name("total").value(total);
		writer.endObject();
	}
}
