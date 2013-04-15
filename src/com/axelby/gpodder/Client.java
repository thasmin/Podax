package com.axelby.gpodder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.gson.stream.JsonWriter;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;
import android.util.Log;

public class Client {

	private static class Config {
		public String mygpo = "https://gpodder.net/";
		public String mygpo_feedservice = "https://mygpo-feedservice.appspot.com/";
		public long update_timeout = 604800L;
	}
	
	private static Config _config;
	private static Calendar _configRefresh = null;

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
	
	private static String readStream(InputStream stream) {
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

	private Context _context;
	private String _username;
	private String _password;
	private String _sessionId;

	public Client(Context context, String username, String password) {
		_context = context;
		_username = username;
		_password = password;
	}

	private void writePost(HttpsURLConnection conn, String toPost)
			throws IOException {
		conn.setDoOutput(true);
		OutputStream output = null;
		try {
		     output = conn.getOutputStream();
		     output.write(toPost.getBytes());
		} finally {
		     if (output != null) try { output.close(); } catch (IOException logOrIgnore) {}
		}
	}

	public HttpsURLConnection createConnection(URL url) throws IOException {
		HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();

		if (_sessionId == null) {
			// basic authentication
			String auth = _username + ":" + _password;
			String encoded = new String(Base64.encode(auth.getBytes(), Base64.NO_WRAP));
			conn.setRequestProperty("Authorization", "basic " + encoded);
		} else {
			conn.setRequestProperty("Cookie", "sessionid=" + _sessionId);
		}
		conn.setRequestProperty("User-Agent", "podax/6.0");

		return conn;
	}

	public boolean authenticate() {
		verifyCurrentConfig();

		URL url;
		HttpsURLConnection conn = null;
		try {
			url = new URL(_config.mygpo + "api/2/auth/" + _username + "/login.json");
			conn = createConnection(url);
			conn.setRequestMethod("POST");

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200)
				return false;

			for (String val : conn.getHeaderFields().get("Set-Cookie")) {
				String[] data = val.split(";")[0].split("=");
				if (data[0].equals("sessionid"))
					_sessionId = data[1];
			}

			return true;
		} catch (IOException e) {
			Log.e("Podax", "io exception while authenticating to gpodder", e);
			return false;
		} catch (Exception e) {
			Log.e("Podax", "exception while authenticating to gpodder", e);
			return false;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	public String getDeviceName() {
		verifyCurrentConfig();

		URL url;
		HttpsURLConnection conn = null;
		try {
			url = new URL(_config.mygpo + "api/2/devices/" + _username + ".json");
			conn = createConnection(url);

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200)
				return null;

			String results = readStream(conn.getInputStream());
			if (results == null)
				return null;
			JSONArray devices = (JSONArray)new JSONTokener(results).nextValue();
			for (int i = 0; i < devices.length(); ++i) {
				JSONObject device = devices.getJSONObject(i);
				if (device.getString("id").equals("podax"))
					return device.getString("caption");
			}
			return null;
		} catch (IOException e) {
			Log.e("Podax", "io exception while getting device name", e);
			return null;
		} catch (Exception e) {
			Log.e("Podax", "exception while getting device name", e);
			return null;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	public boolean setDeviceName(String deviceName) {
		verifyCurrentConfig();

		URL url;
		HttpsURLConnection conn = null;
		try {
			url = new URL(_config.mygpo + "api/2/devices/" + _username + "/podax.json");
			conn = createConnection(url);
			HashMap<String, String> data = new HashMap<String, String>();
			data.put("caption", deviceName);
			data.put("type", "mobile");
			JSONObject json = new JSONObject(data);
			writePost(conn, json.toString());

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200)
				return false;
			return true;
		} catch (IOException e) {
			Log.e("Podax", "io exception while getting device name", e);
			return false;
		} catch (Exception e) {
			Log.e("Podax", "exception while getting device name", e);
			return false;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	public class Changes {
		public Vector<String> added = new Vector<String>();
		public Vector<String> removed = new Vector<String>();
		public int timestamp = 0;
		public boolean isValid() { return timestamp != 0; }
		public boolean isEmpty() { return added.size() > 0 || removed.size() > 0; }
	}

	public Changes getSubscriptionChanges(int lastCheck) {
		verifyCurrentConfig();

		URL url;
		HttpsURLConnection conn = null;
		Changes changes = new Changes();
		try {
			url = new URL(_config.mygpo + "api/2/subscriptions/" + _username + "/podax.json?since=" + String.valueOf(lastCheck));
			conn = createConnection(url);

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200)
				return null;

			String results = readStream(conn.getInputStream());
			if (results == null)
				return null;
			JSONObject json = (JSONObject) new JSONTokener(results).nextValue();

			changes.timestamp = json.getInt("timestamp");
			JSONArray added = json.getJSONArray("add");
			if (added != null)
				for (int i = 0; i < added.length(); ++i)
					changes.added.add(added.getString(i));
			JSONArray removed = json.getJSONArray("remove");
			if (removed != null)
				for (int i = 0; i < removed.length(); ++i)
					changes.removed.add(removed.getString(i));
			
			return changes;
		} catch (IOException e) {
			Log.e("Podax", "io exception while getting device name", e);
			return null;
		} catch (Exception e) {
			Log.e("Podax", "exception while getting device name", e);
			return null;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	public void syncDiffs() {
		verifyCurrentConfig();

		SQLiteDatabase db = new DBAdapter(_context).getWritableDatabase();
		HttpsURLConnection conn = null;
		try {
			Vector<String> toAdd = new Vector<String>();
			Cursor c = db.rawQuery("SELECT url FROM pending_add", null);
			while (c.moveToNext())
				toAdd.add(c.getString(0));
			c.close();

			Vector<String> toRemove = new Vector<String>();
			c = db.rawQuery("SELECT url FROM pending_remove", null);
			while (c.moveToNext())
				toRemove.add(c.getString(0));
			c.close();

			if (toAdd.size() == 0 && toRemove.size() == 0) {
				db.close();
				return;
			}

			URL url = new URL(_config.mygpo + "api/2/subscriptions/" + _username + "/podax.json");
			conn = createConnection(url);

			conn.setDoOutput(true);
			OutputStreamWriter streamWriter = new OutputStreamWriter(conn.getOutputStream());
			JsonWriter writer = new JsonWriter(streamWriter);
			writer.beginObject();
			writeStrings(writer, "add", toAdd);
			writeStrings(writer, "remove", toRemove);
			writer.endObject();
			writer.close();

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200)
				return;

			// clear out the pending tables
			db.execSQL("DELETE FROM pending_add");
			db.execSQL("DELETE FROM pending_remove");
		} catch (MalformedURLException e) {
			Log.e("Podax", "error while syncing gpodder diffs", e);
		} catch (IOException e) {
			Log.e("Podax", "error while syncing gpodder diffs", e);
		} catch (Exception e) {
			Log.e("Podax", "error while syncing gpodder diffs", e);
		} finally {
			if (db != null)
				db.close();
			if (conn != null)
				conn.disconnect();
		}
	}

	public void writeStrings(JsonWriter writer, String key, Vector<String> values) throws IOException {
		writer.name(key);
		writer.beginArray();
		for (String s : values)
			writer.value(s);
		writer.endArray();
	}
}
