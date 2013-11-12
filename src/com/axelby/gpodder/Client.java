package com.axelby.gpodder;

import android.content.Context;
import android.database.Cursor;
import android.util.Base64;
import android.util.Log;

import com.axelby.gpodder.dto.Changes;
import com.axelby.gpodder.dto.EpisodeUpdate;
import com.axelby.gpodder.dto.EpisodeUpdateConfirmation;
import com.axelby.podax.GPodderProvider;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;

public class Client extends NoAuthClient {
	private String _username;
	private String _password;
	private String _sessionId;

	public Client(Context context, String username, String password) {
		super(context);
		_username = username;
		_password = password;
	}

	private void writePost(HttpsURLConnection conn, String toPost) throws IOException {
		conn.setDoOutput(true);
		OutputStream output = null;
		try {
		     output = conn.getOutputStream();
		     output.write(toPost.getBytes());
		} finally {
		     if (output != null) try { output.close(); } catch (IOException ignored) {}
		}
	}

	protected HttpsURLConnection createConnection(URL url) throws IOException {
		HttpsURLConnection conn = super.createConnection(url);

		if (_sessionId == null) {
			// basic authentication
			String auth = _username + ":" + _password;
			String encoded = new String(Base64.encode(auth.getBytes(), Base64.NO_WRAP));
			conn.setRequestProperty("Authorization", "basic " + encoded);
		} else {
			conn.setRequestProperty("Cookie", "sessionid=" + _sessionId);
		}

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

	public DeviceConfiguration getDeviceConfiguration(String deviceId) {
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
				if (device.getString("id").equals(deviceId))
					return new DeviceConfiguration(device.getString("caption"), device.getString("type"));
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

	public boolean setDeviceConfiguration(String deviceId, DeviceConfiguration configuration) {
		verifyCurrentConfig();

		URL url;
		HttpsURLConnection conn = null;
		try {
			url = new URL(_config.mygpo + "api/2/devices/" + _username + "/" + deviceId + ".json");
			conn = createConnection(url);
			HashMap<String, String> data = new HashMap<String, String>();
			data.put("caption", configuration.getCaption());
			data.put("type", configuration.getType());
			JSONObject json = new JSONObject(data);
			writePost(conn, json.toString());

			conn.connect();
			int code = conn.getResponseCode();
			return code == 200;
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

	public Changes getSubscriptionChanges(String deviceId, int lastCheck) {
		verifyCurrentConfig();

		URL url;
		HttpsURLConnection conn = null;
		try {
			url = new URL(_config.mygpo + "api/2/subscriptions/" + _username + "/" + deviceId + ".json?since=" + String.valueOf(lastCheck));
			conn = createConnection(url);

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200)
				return null;

			JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
			return Changes.fromJson(reader);
		} catch (IOException e) {
			Log.e("Podax", "io exception while getting subscription changes", e);
			return null;
		} catch (Exception e) {
			Log.e("Podax", "exception while getting subscription changes", e);
			return null;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	public void syncDiffs(String deviceId) {
		verifyCurrentConfig();

		HttpsURLConnection conn = null;
		try {
			Vector<String> toAdd = new Vector<String>();
			Cursor c = _context.getContentResolver().query(GPodderProvider.TO_ADD_URI, new String[] { "url" }, null, null, null);
			if (c != null) {
				while (c.moveToNext())
					toAdd.add(c.getString(0));
				c.close();
			}

			Vector<String> toRemove = new Vector<String>();
			c = _context.getContentResolver().query(GPodderProvider.TO_REMOVE_URI, new String[]{"url"}, null, null, null);
			if (c != null) {
				while (c.moveToNext())
					toRemove.add(c.getString(0));
				c.close();
			}

			if (toAdd.size() == 0 && toRemove.size() == 0)
				return;

			URL url = new URL(_config.mygpo + "api/2/subscriptions/" + _username + "/" + deviceId + ".json");
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
			_context.getContentResolver().delete(GPodderProvider.URI, null, null);
		} catch (MalformedURLException e) {
			Log.e("Podax", "error while syncing gpodder diffs", e);
		} catch (IOException e) {
			Log.e("Podax", "error while syncing gpodder diffs", e);
		} catch (Exception e) {
			Log.e("Podax", "error while syncing gpodder diffs", e);
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	private void writeStrings(JsonWriter writer, String key, Vector<String> values) throws IOException {
		writer.name(key);
		writer.beginArray();
		for (String s : values)
			writer.value(s);
		writer.endArray();
	}

	public EpisodeUpdateConfirmation updateEpisodes(EpisodeUpdate[] updates) {
		verifyCurrentConfig();

		HttpsURLConnection conn = null;
		try {
			URL url = new URL(_config.mygpo + "api/2/episodes/" + _username + ".json");
			conn = createConnection(url);

			conn.setDoOutput(true);
			OutputStreamWriter streamWriter = new OutputStreamWriter(conn.getOutputStream());
			JsonWriter writer = new JsonWriter(streamWriter);
			writer.beginArray();
			for (EpisodeUpdate update : updates)
				update.writeJson(writer);
			writer.endArray();
			writer.close();

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200)
				return null;

			JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
			EpisodeUpdateConfirmation result = EpisodeUpdateConfirmation.readJson(reader);
			return result;
		} catch (MalformedURLException ignored) {
		} catch (IOException ignored) {
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}
}
