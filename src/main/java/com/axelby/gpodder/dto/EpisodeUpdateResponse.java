package com.axelby.gpodder.dto;

import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

public class EpisodeUpdateResponse {
	private Date timestamp;
	private final ArrayList<EpisodeUpdate> updates = new ArrayList<>();

	private EpisodeUpdateResponse() {
	}

	public static EpisodeUpdateResponse readJson(JsonReader reader) throws IOException, ParseException {
		EpisodeUpdateResponse response = new EpisodeUpdateResponse();

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("timestamp"))
				response.timestamp = new Date(reader.nextLong());
			else if (name.equals("actions")) {
				reader.beginArray();
				while (reader.hasNext())
					response.updates.add(EpisodeUpdate.readJson(reader));
				reader.endArray();
			}
		}
		reader.endObject();

		return response;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public Iterable<EpisodeUpdate> getUpdates() {
		return updates;
	}
}
