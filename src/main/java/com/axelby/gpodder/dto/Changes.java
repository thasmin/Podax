package com.axelby.gpodder.dto;

import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.util.ArrayList;

public class Changes {
	private final ArrayList<String> _added = new ArrayList<>();
	private final ArrayList<String> _removed = new ArrayList<>();
	private int _timestamp = 0;

	private Changes() {
	}

	public static Changes fromJson(JsonReader reader) throws IOException {
		Changes changes = new Changes();

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			switch (name) {
				case "timestamp":
					changes._timestamp = reader.nextInt();
					break;
				case "add":
					reader.beginArray();
					while (reader.hasNext())
						changes._added.add(reader.nextString());
					reader.endArray();
					break;
				case "remove":
					reader.beginArray();
					while (reader.hasNext())
						changes._removed.add(reader.nextString());
					reader.endArray();
					break;
			}
		}
		reader.endObject();

		return changes;
	}

	public int getTimestamp() {
		return _timestamp;
	}

	public Iterable<String> getAddedUrls() {
		return _added;
	}

	public Iterable<String> getRemovedUrls() {
		return _removed;
	}
}
