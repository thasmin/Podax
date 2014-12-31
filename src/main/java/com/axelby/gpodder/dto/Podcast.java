package com.axelby.gpodder.dto;

import android.net.Uri;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;

public class Podcast {
	public String website;
	public String description;
	public String title;
	public String url;
	public String logo_url;
	public int position_last_leek;
	public int subscribers_last_week;
	public int subscribers;
	public String mygpo_link;
	public String scaled_logo_url;

	@Override
	public String toString() { return title; }
}
