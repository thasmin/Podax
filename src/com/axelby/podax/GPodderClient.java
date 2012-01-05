package com.axelby.podax;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.os.Handler;

import com.google.api.client.util.Base64;
import com.google.gson.stream.JsonReader;

public class GPodderClient {

	private static class Config {
		public String mygpo = "https://gpodder.net/";
		public String mygpo_feedservice = "https://mygpo-feedservice.appspot.com/";
		public long update_timeout = 604800L;
	}
	
	private static Config _config;
	private static Calendar _configRefresh = null;

	static {
		verifyCurrentConfig();
	}

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

	private static Config retrieveGPodderConfig() {
		Config config = new Config();

		try {
			URL url = new URL("http://gpodder.net/clientconfig.json");
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
			reader.beginObject();
			
			// get mygpo
			reader.nextName(); // should be mygpo
			reader.beginObject();
			reader.nextName(); // should be baseurl
			config.mygpo = reader.nextString();
			reader.endObject();

			// get mygpo-feedservice
			reader.nextName(); // should be mygpo-feedservice
			reader.beginObject();
			reader.nextName(); // should be baseurl
			config.mygpo_feedservice = reader.nextString();
			reader.endObject();

			// get update_timeout
			reader.nextName();
			config.update_timeout = reader.nextLong();

			reader.endObject();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return config;
	}

	private String _username;
	private String _password;
	private String _sessionId;

	public GPodderClient(String username, String password) {
		_username = username;
		_password = password;
	}

	public Thread authorizeInBackground(final Handler handler,
			final GPodderAuthenticatorActivity activity) {
		final Runnable runnable = new Runnable() {
			public void run() {
				final boolean isValid = authenticate();
				if (handler == null || activity == null) {
					return;
				}
				handler.post(new Runnable() {
					public void run() {
						activity.onAuthenticationResult(isValid);
					}
				});
			}
		};
		final Thread t = new Thread() {
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {

				}
			}
		};
		t.start();
		return t;
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

	public HttpsURLConnection createConnection(URL url) throws IOException, Exception {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}
		} };

		HttpsURLConnection conn;

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("TLS");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		
		conn = (HttpsURLConnection)url.openConnection();

		// basic authentication
		String toBase64 = _username + ":" + _password;
		conn.addRequestProperty("Authorization", "basic " + new String(Base64.encode(toBase64.getBytes())));
		if (_sessionId != null)
			conn.addRequestProperty("Cookie", "sessionid=" + _sessionId);

		// gpodder cert does not resolve on android
		conn.setHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		});

		return conn;
	}

	protected boolean authenticate() {
		verifyCurrentConfig();

		URL url;
		HttpsURLConnection conn = null;
		try {
			url = new URL(_config.mygpo + "api/2/auth/" + _username + "/login.json");
			conn = createConnection(url);
			writePost(conn, " ");

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
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (conn != null)
				conn.disconnect();
		}
		return true;
	}

	public Integer getChanges(long lastCheck) {
		verifyCurrentConfig();

		Integer timestamp = null;

		URL url;
		HttpsURLConnection conn = null;
		try {
			url = new URL(_config.mygpo + "api/2/subscriptions/" + _username + "/podax.json?since=" + String.valueOf(lastCheck));
			conn = createConnection(url);

			conn.connect();

			int code = conn.getResponseCode();
			if (code != 200)
				return timestamp;

			try {
				InputStream stream = conn.getInputStream();
				JsonReader reader = new JsonReader(new InputStreamReader(stream));
				reader.beginObject();

				// get add
				for (int i = 0; i < 3; ++i) {
					String key = reader.nextName();
					if (key.equals("add")) {
						reader.beginArray();
						while (reader.hasNext()) {
							String toAdd = reader.nextString();
							toAdd.charAt(0);
						}
						reader.endArray();
					} else if (key.equals("remove")) {
						reader.beginArray();
						while (reader.hasNext()) {
							String toRemove = reader.nextString();
							toRemove.charAt(0);
						}
						reader.endArray();
					} else if (key.equals("timestamp")) {
						timestamp = reader.nextInt();
					}
				}

				reader.endObject();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (conn != null)
				conn.disconnect();
		}

		return timestamp;
	}

}
