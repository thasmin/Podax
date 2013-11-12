package com.axelby.gpodder.dto;

import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.util.Vector;

public class Changes {
	private Vector<String> _added = new Vector<String>();
	private Vector<String> _removed = new Vector<String>();
	private int _timestamp = 0;

	private Changes() {
	}

	public static Changes fromJson(JsonReader reader) throws IOException {
		Changes changes = new Changes();

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("timestamp")) {
				changes._timestamp = reader.nextInt();
			} else if (name.equals("add")) {
				reader.beginArray();
				while (reader.hasNext())
					changes._added.add(reader.nextString());
				reader.endArray();
			} else if (name.equals("remove")) {
				reader.beginArray();
				while (reader.hasNext())
					changes._removed.add(reader.nextString());
				reader.endArray();
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
