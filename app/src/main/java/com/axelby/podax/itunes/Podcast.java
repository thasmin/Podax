package com.axelby.podax.itunes;

import org.joda.time.LocalDate;

public class Podcast {
	public long id;
	public LocalDate date;
	public long category;
	public int position;
	public String name;
	public String summary;
	public String imageUrl;
	public String idUrl;
	public long subscriptionId;

	@Override public String toString() { return name; }
}

