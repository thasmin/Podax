package com.axelby.podax.podaxapp;

public class Network {
	public String imageUrl;
	public String name;
	public String link;
	public String shortcode;
	public Podcast[] podcasts;

	@Override
	public String toString() { return name; }
}
