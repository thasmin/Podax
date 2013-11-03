package com.axelby.gpodder;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

public class NoAuthClient {

	protected static class Config {
		public String mygpo = "https://gpodder.net/";
		public String mygpo_feedservice = "https://mygpo-feedservice.appspot.com/";
		public long update_timeout = 604800L;
	}

	protected static Config _config;
	protected static Calendar _configRefresh = null;

	public static void verifyCurrentConfig() {
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

	protected static String readStream(InputStream stream) {
		Scanner scanner = new Scanner(stream, "UTF-8").useDelimiter("\\A");
		return scanner.hasNext() ? scanner.next() : null;
	}

	private static Config retrieveGPodderConfig() {
		Config config = new Config();

		HttpURLConnection conn = null;
		try {
			URL url = new URL("http://gpodder.net/clientconfig.json");
			conn = (HttpURLConnection)url.openConnection();
			conn.addRequestProperty("User-Agent", "podax/6.0");
			String results = readStream(conn.getInputStream());
			if (results == null)
				return config;
			JSONObject json = (JSONObject) new JSONTokener(results).nextValue();
			config.mygpo = json.getJSONObject("mygpo").getString("baseurl");
			config.mygpo_feedservice = json.getJSONObject("mygpo-feedservice").getString("baseurl");
			config.update_timeout = json.getLong("update_timeout");
		} catch (IOException e) {
			Log.e("Podax", "io exception while retrieving gpodder config", e);
		} catch (JSONException e) {
			Log.e("Podax", "json exception while retrieving gpodder config", e);
		} finally {
			if (conn != null)
				conn.disconnect();
		}

		return config;
	}

	protected HttpsURLConnection createConnection(URL url) throws IOException {
		HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
		conn.setRequestProperty("User-Agent", "podax/6.0");
		return conn;
	}

	protected Context _context;

	public NoAuthClient(Context context) {
		_context = context;
	}


	public List<ToplistPodcast> getPodcastToplist() {
		verifyCurrentConfig();

		URL url;
		HttpsURLConnection conn = null;
		try {
			url = new URL(_config.mygpo + "toplist/20.json");
			conn = createConnection(url);

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200)
				return null;

			String results = readStream(conn.getInputStream());
			if (results == null)
				return null;
			JSONArray toplistJson = (JSONArray)new JSONTokener(results).nextValue();
			ArrayList<ToplistPodcast> toplist = new ArrayList<ToplistPodcast>(toplistJson.length());
			for (int i = 0; i < toplistJson.length(); ++i) {
				JSONObject podcastJson = toplistJson.getJSONObject(i);
				ToplistPodcast podcast = new ToplistPodcast();
				for (Iterator<String> key = podcastJson.keys(); key.hasNext(); ) {
					String k = key.next();
					if (k.equals("website"))
						podcast.setWebsite(Uri.parse(podcastJson.getString(k)));
					else if (k.equals("description"))
						podcast.setDescription(podcastJson.getString(k));
					else if (k.equals("title"))
						podcast.setTitle(podcastJson.getString(k));
					else if (k.equals("url"))
						podcast.setUrl(podcastJson.getString(k));
					else if (k.equals("logo_url"))
						podcast.setLogoUrl(podcastJson.getString(k));
				}
				toplist.add(podcast);
			}
			return toplist;
		} catch (IOException e) {
			Log.e("Podax", "io exception while getting gpodder toplist", e);
			return null;
		} catch (Exception e) {
			Log.e("Podax", "exception while getting gpodder toplist", e);
			return null;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}
}
