package com.axelby.gpodder;

import android.content.Context;
import android.database.Cursor;
import android.util.Base64;
import android.util.Log;

import com.axelby.gpodder.dto.Changes;
import com.axelby.gpodder.dto.EpisodeUpdate;
import com.axelby.gpodder.dto.EpisodeUpdateConfirmation;
import com.axelby.gpodder.dto.EpisodeUpdateResponse;
import com.axelby.gpodder.dto.Podcast;
import com.axelby.podax.GPodderProvider;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

import javax.net.ssl.HttpsURLConnection;

class Client extends NoAuthClient {
	private final String _username;
	private final String _password;
	private String _sessionId;

	public Client(Context context, String username, String password) {
		super(context);
		_username = username;
		_password = password;
	}

	HttpsURLConnection createConnection(URL url) throws IOException {
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
		clearErrorMessage();

		URL url;
		HttpsURLConnection conn = null;
		try {
			url = new URL(_config.mygpo + "api/2/auth/" + _username + "/login.json");
			conn = createConnection(url);
			conn.setRequestMethod("POST");

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200) {
				setErrorMessage(conn.getResponseMessage());
				return false;
			}

			for (String val : conn.getHeaderFields().get("Set-Cookie")) {
				String[] data = val.split(";")[0].split("=");
				if (data[0].equals("sessionid"))
					_sessionId = data[1];
			}

			return true;
		} catch (IOException e) {
			Log.e("Podax", "io exception while authenticating to gpodder", e);
			setErrorMessage(e.getMessage());
			return false;
		} catch (Exception e) {
			Log.e("Podax", "exception while authenticating to gpodder", e);
			setErrorMessage(e.getMessage());
			return false;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	public DeviceConfiguration getDeviceConfiguration(String deviceId) {
		verifyCurrentConfig();
		clearErrorMessage();

		URL url;
		HttpsURLConnection conn = null;
		try {
			url = new URL(_config.mygpo + "api/2/devices/" + _username + ".json");
			conn = createConnection(url);

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200) {
				setErrorMessage(conn.getResponseMessage());
				return null;
			}

			JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
			reader.beginArray();
			while (reader.hasNext()) {
				reader.beginObject();
				String id = null, caption = null, type = null;
				while (reader.hasNext()) {
					String name = reader.nextName();
					switch (name) {
						case "id": id = reader.nextString(); break;
						case "caption": caption = reader.nextString(); break;
						case "type": type = reader.nextString(); break;
					}
				}
				reader.endObject();
				if (id != null && id.equals(deviceId)) {
					reader.close();
					return new DeviceConfiguration(caption, type);
				}
			}
			return null;
		} catch (IOException e) {
			Log.e("Podax", "io exception while getting device name", e);
			setErrorMessage(e.getMessage());
			return null;
		} catch (Exception e) {
			Log.e("Podax", "exception while getting device name", e);
			setErrorMessage(e.getMessage());
			return null;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	public void setDeviceConfiguration(String deviceId, DeviceConfiguration configuration) {
		verifyCurrentConfig();
		clearErrorMessage();

		HttpsURLConnection conn = null;
		try {
			URL url = new URL(_config.mygpo + "api/2/devices/" + _username + "/" + deviceId + ".json");
			conn = createConnection(url);

			conn.setDoOutput(true);
			OutputStreamWriter streamWriter = new OutputStreamWriter(conn.getOutputStream());
			JsonWriter writer = new JsonWriter(streamWriter);
			writer.beginObject();
			writer.name("caption").value(configuration.getCaption());
			writer.name("type").value(configuration.getType());
			writer.endObject();
			writer.close();

			StringWriter w = new StringWriter();
			writer = new JsonWriter(w);
			writer.beginObject();
			writer.name("caption").value(configuration.getCaption());
			writer.name("type").value(configuration.getType());
			writer.endObject();
			writer.close();

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200) {
				setErrorMessage(conn.getResponseMessage());
				return;
			}
			return;
		} catch (MalformedURLException e) {
			Log.e("Podax", "malformed url exception while setting device name", e);
			setErrorMessage(e.getMessage());
			return;
		} catch (IOException e) {
			Log.e("Podax", "io exception while setting device name", e);
			setErrorMessage(e.getMessage());
			return;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	public Changes getSubscriptionChanges(String deviceId, int lastCheck) {
		verifyCurrentConfig();
		clearErrorMessage();

		URL url;
		HttpsURLConnection conn = null;
		try {
			url = new URL(_config.mygpo + "api/2/subscriptions/" + _username + "/" + deviceId + ".json?since=" + String.valueOf(lastCheck));
			conn = createConnection(url);

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200) {
				setErrorMessage(conn.getResponseMessage());
				return null;
			}

			JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
			return Changes.fromJson(reader);
		} catch (IOException e) {
			Log.e("Podax", "io exception while getting subscription changes", e);
			setErrorMessage(e.getMessage());
			return null;
		} catch (Exception e) {
			Log.e("Podax", "exception while getting subscription changes", e);
			setErrorMessage(e.getMessage());
			return null;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	public void syncSubscriptionDiffs(String deviceId) {
		verifyCurrentConfig();
		clearErrorMessage();

		HttpsURLConnection conn = null;
		try {
			ArrayList<String> toAdd = new ArrayList<>();
			Cursor c = _context.getContentResolver().query(GPodderProvider.TO_ADD_URI, new String[]{"url"}, null, null, null);
			if (c != null) {
				while (c.moveToNext())
					toAdd.add(c.getString(0));
				c.close();
			}

			ArrayList<String> toRemove = new ArrayList<>();
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
			if (code != 200) {
				setErrorMessage(conn.getResponseMessage());
				return;
			}

			// clear out the pending tables
			_context.getContentResolver().delete(GPodderProvider.URI, null, null);
		} catch (Exception e) {
			Log.e("Podax", "error while syncing gpodder diffs", e);
			setErrorMessage(e.getMessage());
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	private void writeStrings(JsonWriter writer, String key, Collection<String> values) throws IOException {
		writer.name(key);
		writer.beginArray();
		for (String s : values)
			writer.value(s);
		writer.endArray();
	}

	public EpisodeUpdateConfirmation updateEpisodes(EpisodeUpdate[] updates) {
		verifyCurrentConfig();
		clearErrorMessage();

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
			if (code != 200) {
				setErrorMessage(conn.getResponseMessage());
				return null;
			}

			JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
			return EpisodeUpdateConfirmation.readJson(reader);
		} catch (MalformedURLException e) {
			Log.e("Podax", e.getMessage());
			setErrorMessage(e.getMessage());
			return null;
		} catch (IOException e) {
			Log.e("Podax", e.getMessage());
			setErrorMessage(e.getMessage());
			return null;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	public EpisodeUpdateResponse getEpisodeUpdates(Long since) {
		verifyCurrentConfig();
		clearErrorMessage();

		HttpsURLConnection conn = null;
		try {
			String querystring = "?aggregated=true";
			if (since != null)
				querystring += "&since=" + since;
			URL url = new URL(_config.mygpo + "api/2/episodes/" + _username + ".json" + querystring);
			conn = createConnection(url);

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200) {
				setErrorMessage(conn.getResponseMessage());
				return null;
			}

			JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
			return EpisodeUpdateResponse.readJson(reader);
		} catch (ParseException e) {
			Log.e("Podax", e.getMessage());
			setErrorMessage(e.getMessage());
			return null;
		} catch (MalformedURLException e) {
			Log.e("Podax", e.getMessage());
			setErrorMessage(e.getMessage());
			return null;
		} catch (IOException e) {
			Log.e("Podax", e.getMessage());
			setErrorMessage(e.getMessage());
			return null;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	public ArrayList<Podcast> getAllSubscriptions() {
		verifyCurrentConfig();
		clearErrorMessage();

		HttpsURLConnection conn = null;
		try {
			URL url = new URL(_config.mygpo + "subscriptions/" + _username + ".json");
			conn = createConnection(url);

			conn.connect();
			int code = conn.getResponseCode();
			if (code != 200) {
				setErrorMessage(conn.getResponseMessage());
				return null;
			}

			ArrayList<Podcast> subscriptions = new ArrayList<>();
			JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
			reader.beginArray();
			while (reader.hasNext())
				subscriptions.add(Podcast.readJson(reader));
			reader.endArray();
			return subscriptions;
		} catch (MalformedURLException e) {
			Log.e("Podax", e.getMessage());
			setErrorMessage(e.getMessage());
			return null;
		} catch (IOException e) {
			Log.e("Podax", e.getMessage());
			setErrorMessage(e.getMessage());
			return null;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}
}
