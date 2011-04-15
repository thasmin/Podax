package com.axelby.podax;

import org.json.JSONObject;

public class PodcastSearcher {

	public JSONObject search(String query) {
		String url = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/wa/wsSearch?country=us&media=podcast&term="+query;
		return Downloader.downloadJSON(url);
	}
}
