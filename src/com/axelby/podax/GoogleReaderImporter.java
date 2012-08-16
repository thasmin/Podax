package com.axelby.podax;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.os.AsyncTask;
import android.sax.Element;
import android.sax.ElementListener;
import android.sax.RootElement;
import android.sax.TextElementListener;
import android.util.Log;
import android.util.Xml;

import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;

public class GoogleReaderImporter extends AsyncTask<String, Void, GoogleReaderImporter.Results> {

	Activity _activity;
	boolean _isNotAuthorized = false;

	public GoogleReaderImporter(Activity activity) {
		_activity = activity;
	}

	static class ReaderFeed {
		public String url = "";
		public String title = "";
		public Vector<String> categories = new Vector<String>();
	}

	public static class Results {
		public Hashtable<String, Hashtable<String, String>> categories = new Hashtable<String, Hashtable<String,String>>();
		public Hashtable<String, String> nofolder = new Hashtable<String, String>();
	}

	public interface UnauthorizedHandler {
		public void onUnauthorized();
	}
	private UnauthorizedHandler _unauthorizedHandler = null;
	public void setUnauthorizedHandler(UnauthorizedHandler handler) {
		_unauthorizedHandler = handler;
	}

	public interface SuccessHandler {
		public void onSuccess(Results results);
	}
	private SuccessHandler _successHandler = null;
	public void setSuccessHandler(SuccessHandler handler) {
		_successHandler = handler;
	}

	private HttpRequestFactory createRequestFactory(
			final HttpTransport transport, final String authToken) {

		return transport.createRequestFactory(new HttpRequestInitializer() {
			public void initialize(HttpRequest request) {
				GoogleHeaders headers = new GoogleHeaders();
				headers.setApplicationName("Podax");
				headers.setGDataVersion("2");
				headers.setGoogleLogin(authToken);
				request.setHeaders(headers);
			}
		});
	}

	@Override
	protected Results doInBackground(String... authTokens) {
		if (authTokens.length != 1)
			return new Results();

		HttpRequestFactory request = createRequestFactory(new ApacheHttpTransport(), authTokens[0]);
		try {
			// put these in an array to we can use them in the inner class
			final ReaderFeed[] feed = new ReaderFeed[] { new ReaderFeed() };
			final Results results = new Results();

			// set up the sax parser
			RootElement root = new RootElement("object");
			Element object = root.getChild("list").getChild("object");
			object.setElementListener(new ElementListener() {
				public void start(Attributes attrs) {
					feed[0] = new ReaderFeed();
				}

				public void end() {
					if (feed[0].categories.size() == 0)
						results.nofolder.put(feed[0].title, feed[0].url);
				}
			});

			object.getChild("string").setTextElementListener(
					new TextElementListener() {
						String name = null;

						public void start(Attributes attributes) {
							name = attributes.getValue("name");
						}

						public void end(String text) {
							if (name == null)
								return;
							if (name.equals("id")) {
								if (text != null && text.startsWith("feed/"))
									text = text.substring(5);
								feed[0].url = text;
							} else if (name.equals("title")) {
								feed[0].title = text;
							}
						}
					});

			final String[] listName = { "" };
			object.getChild("list").setElementListener(new ElementListener() {
				public void start(Attributes attributes) {
					if (attributes.getValue("name").equals("categories"))
						listName[0] = "categories";
				}

				public void end() {
					listName[0] = "";
				}
			});
			object.getChild("list").getChild("object").getChild("string")
					.setTextElementListener(new TextElementListener() {
						boolean isLabel = false;

						public void start(Attributes attributes) {
							// make sure we're in a category and have a string name
							if (!listName[0].equals("categories") || attributes.getValue("name") == null)
								return;
							if (attributes.getValue("name").equals("label"))
								isLabel = true;
						}

						public void end(String text) {
							if (isLabel) {
								feed[0].categories.add(text);
								if (!results.categories.containsKey(text))
									results.categories.put(text, new Hashtable<String, String>());
								results.categories.get(text).put(feed[0].title, feed[0].url);
							}
							isLabel = false;
						}
					});

			GenericUrl url = new GenericUrl("http://www.google.com/reader/api/0/subscription/list");
			HttpResponse response = request.buildGetRequest(url).execute();
			InputStream content = response.getContent();
			Xml.parse(content, Xml.Encoding.UTF_8, root.getContentHandler());

			return results;
		} catch (HttpResponseException e) {
			if (e.getStatusCode() == 401) {
				_isNotAuthorized = true;
				if (_unauthorizedHandler != null)
					_unauthorizedHandler.onUnauthorized();
			} else {
				Log.e("Podax", "HttpResponseException: " + e.getMessage());
			}
			return new Results();
		} catch (IOException e) {
			Log.e("Podax", "IOException: " + e.getMessage());
			return new Results();
		} catch (SAXException e) {
			Log.e("Podax", "SAXException: " + e.getMessage());
			return new Results();
		}
	}

	@Override
	protected void onPostExecute(Results results) {
		if (_isNotAuthorized)
			return;

		if (_successHandler != null)
			_successHandler.onSuccess(results);
	}
}
