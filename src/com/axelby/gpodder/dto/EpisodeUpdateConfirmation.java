package com.axelby.gpodder.dto;

import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EpisodeUpdateConfirmation {
	private int _timestamp;
	private HashMap<String, String> _sanitizations = new HashMap<String, String>();

	private EpisodeUpdateConfirmation() {
	}

	public static EpisodeUpdateConfirmation readJson(JsonReader reader) throws IOException {
		EpisodeUpdateConfirmation confirmation = new EpisodeUpdateConfirmation();

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("timestamp")) {
				confirmation._timestamp = reader.nextInt();
			} else if (name.equals("update_urls")) {
				reader.beginArray();
				while (reader.hasNext()) {
					reader.beginArray();
					String old = reader.nextString();
					String better = reader.nextString();
					reader.endArray();
					confirmation._sanitizations.put(old, better);
				}
			}
		}

		return confirmation;
	}

	public int getTimestamp() {
		return _timestamp;
	}

	public Map<String, String> getSanitizations() {
		return _sanitizations;
	}
}
