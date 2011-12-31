package com.axelby.podax;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Handler;
import android.webkit.CookieManager;

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
		String url = "http://gpodder.net/clientconfig.json";
		HttpGet get = new HttpGet(url);
		HttpClient client = new DefaultHttpClient();

		Config config = new Config();

		try {
			HttpResponse response = client.execute(get);
			JsonReader reader = new JsonReader(new InputStreamReader(response.getEntity().getContent()));
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
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return config;
	}

	private String _username;
	private String _password;

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

	protected boolean authenticate() {
		verifyCurrentConfig();

		URL url;
		HttpsURLConnection conn;
		try {
			url = new URL(_config.mygpo + "api/2/auth/" + _username + "/login.json");
			conn = createConnection(_username, _password, url);
		} catch (Exception e) {
			return false;
		}
		
		try {
			// do a post
			conn.setDoOutput(true);
			OutputStream output = null;
			try {
			     output = conn.getOutputStream();
			     output.write(" ".getBytes());
			} finally {
			     if (output != null) try { output.close(); } catch (IOException logOrIgnore) {}
			}

			conn.connect();
			int code = conn.getResponseCode();
			conn.disconnect();
			return code == 200;
			
			/*
			for (Entry<String, List<String>> h : conn.getHeaderFields().entrySet())
				System.out.println(h.getKey() + "(" + h.getValue().size() + "): " + h.getValue().get(0));

			String contentType = conn.getHeaderField("Content-Type");
			String charset = null;
			for (String param : contentType.replace(" ", "").split(";")) {
			    if (param.startsWith("charset=")) {
			        charset = param.split("=", 2)[1];
			        break;
			    }
			}
			if (charset != null) {
			    BufferedReader reader = null;
			    try {
			    	InputStream is = conn.getInputStream();
			        InputStreamReader inputStreamReader = new InputStreamReader(is, charset);
					reader = new BufferedReader(inputStreamReader);
			        for (String line; (line = reader.readLine()) != null;) {
			            System.out.println(line);
			        }
			    } finally {
			        if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
			    }
			}

			conn.disconnect();
			*/
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public HttpsURLConnection createConnection(final String username,
			final String password, URL url) throws IOException, Exception {
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
		String toBase64 = username + ":" + password;
		conn.addRequestProperty("Authorization", "basic " + new String(Base64.encode(toBase64.getBytes())));

		// gpodder cert does not resolve on android
		conn.setHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		});

		return conn;
	}

}
