package com.axelby.gpodder;

import android.content.Context;
import android.util.Log;

import com.axelby.gpodder.dto.Podcast;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class NoAuthClient {

	static Config _config;
	private static Calendar _configRefresh = null;
	final Context _context;

	private String _errorMessage;

	public NoAuthClient(Context context) {
		_context = context;
	}

	public String getErrorMessage() {
		return _errorMessage;
	}
	void setErrorMessage(String errorMessage) {
		_errorMessage = errorMessage;
	}
	void clearErrorMessage() {
		_errorMessage = null;
	}

	static void verifyCurrentConfig() {
		if (_configRefresh == null || _configRefresh.before(new GregorianCalendar())) {
			_config = retrieveGPodderConfig();

			// do NOT use basic auth over HTTP without SSL
			if (_config.mygpo.startsWith("http://"))
				_config.mygpo = "https://" + _config.mygpo.substring(7);
			if (_config.mygpo_feedservice.startsWith("http://"))
				_config.mygpo_feedservice = "https://" + _config.mygpo_feedservice.substring(7);

			_configRefresh = new GregorianCalendar();
			_configRefresh.add(Calendar.MILLISECOND, (int) _config.update_timeout);
		}
	}

	private static Config retrieveGPodderConfig() {
		Config config = new Config();

		HttpURLConnection conn = null;
		try {
			URL url = new URL("http://gpodder.net/clientconfig.json");
			conn = (HttpURLConnection) url.openConnection();
			conn.addRequestProperty("User-Agent", "podax/6.1");

			// this probably won't change -- use defaults if request fails
			if (conn.getResponseCode() != 200)
				return config;

			JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				switch (name) {
					case "mygpo":
						reader.beginObject();
						reader.nextName();
						config.mygpo = reader.nextString();
						reader.endObject();
						break;
					case "mygpo-feedservice":
						reader.beginObject();
						reader.nextName();
						config.mygpo_feedservice = reader.nextString();
						reader.endObject();
						break;
					case "update_timeout":
						config.update_timeout = reader.nextLong();
						break;
				}
			}
			reader.endObject();
			return config;
		} catch (IOException e) {
			Log.e("Podax", "io exception while retrieving gpodder config", e);
		} finally {
			if (conn != null)
				conn.disconnect();
		}

		return config;
	}

	HttpsURLConnection createConnection(URL url) throws IOException {
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setRequestProperty("User-Agent", "podax/6.0");
		return conn;
	}

	public List<Podcast> getPodcastToplist() {
		verifyCurrentConfig();
		clearErrorMessage();

		HttpsURLConnection conn = null;
		try {
			URL url = new URL(_config.mygpo + "toplist/20.json");
			conn = createConnection(url);

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200) {
				setErrorMessage(conn.getResponseMessage());
				return null;
			}

			ArrayList<Podcast> toplist = new ArrayList<>();
			JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
			reader.beginArray();
			while (reader.hasNext())
				toplist.add(Podcast.readJson(reader));
			reader.endArray();
			return toplist;
		} catch (IOException e) {
			Log.e("Podax", "io exception while getting gpodder toplist", e);
			setErrorMessage(e.getMessage());
			return null;
		} catch (Exception e) {
			Log.e("Podax", "exception while getting gpodder toplist", e);
			setErrorMessage(e.getMessage());
			return null;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	static class Config {
		public String mygpo = "https://gpodder.net/";
		public String mygpo_feedservice = "https://mygpo-feedservice.appspot.com/";
		public long update_timeout = 604800L;
	}
}
