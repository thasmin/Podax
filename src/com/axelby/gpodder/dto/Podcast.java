package com.axelby.gpodder.dto;

import android.net.Uri;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;

public class Podcast {
	private Uri website;
	private String description;
	private String title;
	private String url;
	private String logoUrl;
	private Integer positionLastWeek;
	private Integer subscribersLastWeek;
	private Integer subscribers;
	private String mygpoLink;
	private String scaledLogoUrl;

	private Podcast() {
	}

	@Override
	public String toString() {
		return getTitle();
	}

	public Uri getWebsite() {
		return website;
	}
	private void setWebsite(Uri website) {
		this.website = website;
	}

	public String getDescription() {
		return description;
	}
	private void setDescription(String description) {
		this.description = description;
	}

	public String getTitle() {
		return title;
	}
	private void setTitle(String title) {
		this.title = title;
	}

	public String getUrl() {
		return url;
	}
	private void setUrl(String url) {
		this.url = url;
	}

	public String getLogoUrl() {
		return logoUrl;
	}
	private void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}

	public Integer getPositionLastWeek() {
		return positionLastWeek;
	}
	private void setPositionLastWeek(int positionLastWeek) {
		this.positionLastWeek = positionLastWeek;
	}

	public Integer getSubscribersLastWeek() {
		return subscribersLastWeek;
	}
	private void setSubscribersLastWeek(int subscribersLastWeek) {
		this.subscribersLastWeek = subscribersLastWeek;
	}

	public Integer getSubscribers() {
		return subscribers;
	}
	private void setSubscribers(int subscribers) {
		this.subscribers = subscribers;
	}

	public String getMygpoLink() {
		return mygpoLink;
	}
	private void setMygpoLink(String mygpoLink) {
		this.mygpoLink = mygpoLink;
	}

	public String getScaledLogoUrl() {
		return scaledLogoUrl;
	}
	private void setScaledLogoUrl(String scaledLogoUrl) {
		this.scaledLogoUrl = scaledLogoUrl;
	}

	public static Podcast readJson(JsonReader reader) throws IOException {
		Podcast podcast = new Podcast();
		reader.beginObject();
		while (reader.hasNext()) {
			String k = reader.nextName();
			if (k.equals("website") && reader.peek() == JsonToken.STRING)
				podcast.setWebsite(Uri.parse(reader.nextString()));
			else if (k.equals("description") && reader.peek() == JsonToken.STRING)
				podcast.setDescription(reader.nextString());
			else if (k.equals("title") && reader.peek() == JsonToken.STRING)
				podcast.setTitle(reader.nextString());
			else if (k.equals("url") && reader.peek() == JsonToken.STRING)
				podcast.setUrl(reader.nextString());
			else if (k.equals("logo_url") && reader.peek() == JsonToken.STRING)
				podcast.setLogoUrl(reader.nextString());
			else if (k.equals("mygpo_link") && reader.peek() == JsonToken.STRING)
				podcast.setMygpoLink(reader.nextString());
			else if (k.equals("position_last_week") && reader.peek() == JsonToken.NUMBER)
				podcast.setPositionLastWeek(reader.nextInt());
			else if (k.equals("subscribers") && reader.peek() == JsonToken.NUMBER)
				podcast.setSubscribers(reader.nextInt());
			else if (k.equals("subscribers_last_week") && reader.peek() == JsonToken.NUMBER)
				podcast.setSubscribersLastWeek(reader.nextInt());
			else if (k.equals("scaled_logo_url") && reader.peek() == JsonToken.STRING)
				podcast.setScaledLogoUrl(reader.nextString());
			else
				reader.skipValue();
		}
		reader.endObject();
		return podcast;
	}
}
