package com.axelby.podax;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ParseException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Xml;

class SubscriptionUpdater {
	private static final String NAMESPACE_ITUNES = "http://www.itunes.com/dtds/podcast-1.0.dtd";
	private static final String NAMESPACE_MEDIA = "http://search.yahoo.com/mrss/";

	private Context _context;
	Vector<Integer> _toUpdate = new Vector<Integer>();
	Thread _runningThread;
	
	public static final SimpleDateFormat rfc822DateFormats[] = new SimpleDateFormat[] {
			new SimpleDateFormat("EEE, d MMM yy HH:mm:ss z"),
			new SimpleDateFormat("EEE, d MMM yy HH:mm z"),
			new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z"),
			new SimpleDateFormat("EEE, d MMM yyyy HH:mm z"),
			new SimpleDateFormat("d MMM yy HH:mm z"),
			new SimpleDateFormat("d MMM yy HH:mm:ss z"),
			new SimpleDateFormat("d MMM yyyy HH:mm z"),
			new SimpleDateFormat("d MMM yyyy HH:mm:ss z"), };
	
	SubscriptionUpdater(Context context) {
		_context = context;
	}
	
	public void addSubscriptionId(int id) {
		_toUpdate.add(id);
	}
	
	public void addAllSubscriptions() {
		final DBAdapter dbAdapter = DBAdapter.getInstance(_context);
		for (Subscription s : dbAdapter.getSubscriptions())
			_toUpdate.add(s.getId());
	}
	
	public void run() {
		if (_runningThread != null)
			return;
		_runningThread = new Thread(_worker);
		_runningThread.start();
	}

	public void stop() {
		if (_runningThread != null && _runningThread.isAlive())
			_runningThread.stop();
	}
	
	private Runnable _worker = new Runnable() {
		public void run() {
			try {
				if (!PodaxApp.ensureWifi(_context))
					return;
				
				DBAdapter dbAdapter = DBAdapter.getInstance(_context);
				
				// send subscriptions to the Podax server
				// errors are acceptible
				try {
					sendSubscriptionsToPodaxServer(dbAdapter);
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				while (_toUpdate.size() > 0) {
					Integer subscriptionId = _toUpdate.get(0);
					Subscription subscription = dbAdapter.loadSubscription(subscriptionId);
					_toUpdate.remove(0);

					HttpGet request = new HttpGet(subscription.getUrl());
if (subscriptionId != 23) {
					if (subscription.getETag() != null)
						request.addHeader("If-None-Match", subscription.getETag());
					if (subscription.getLastModified() != null && subscription.getLastModified().getTime() > 0) {
						SimpleDateFormat imsFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
						request.addHeader("If-Modified-Since", imsFormat.format(subscription.getLastModified()));
					}
}
					HttpClient client = new DefaultHttpClient();
					HttpResponse response = client.execute(request);
					
					int code = response.getStatusLine().getStatusCode();
					if (code == 304) {
						// content not modified
						continue;
					}
					
if (subscriptionId != 23) {
					if (response.containsHeader("ETag") && response.getLastHeader("ETag").getValue().equals(subscription.getETag()))
						continue;
}

					updateUpdateNotification(subscription, "Downloading Feed");
					InputStream responseStream = response.getEntity().getContent();
					if (responseStream == null) {
						showUpdateErrorNotification(subscription, "Feed not available. Check the URL.");
						continue;
					} 

					String name;
					Date lastBuildDate = null;
					boolean done = false;
					
					ContentValues podcastValues = new ContentValues();
					boolean onItem = false;
					XmlPullParser parser = Xml.newPullParser();
					parser.setInput(responseStream, null);
					int eventType = parser.getEventType();
					do {
						switch (eventType) {
						case XmlPullParser.START_DOCUMENT:
							break;
						case XmlPullParser.START_TAG:
							name = parser.getName();
							if (name.equalsIgnoreCase("lastBuildDate")) {
								for (SimpleDateFormat df : rfc822DateFormats) {
									try {
if (subscriptionId != 23) {
										lastBuildDate = df.parse(parser.nextText());
										if (lastBuildDate.getTime() == subscription.getLastModified().getTime())
											done = true;
}
										break;
									} catch (ParseException e) {
									}
								}
							} else if (name.equalsIgnoreCase("item")) {
								podcastValues = new ContentValues();
								podcastValues.put(PodcastProvider.COLUMN_SUBSCRIPTION_ID, subscriptionId);
								onItem = true;
							} else if (name.equalsIgnoreCase("title") && parser.getNamespace().equals("")) {
								if (onItem)
									podcastValues.put(PodcastProvider.COLUMN_TITLE, parser.nextText());
								else
									dbAdapter.updateSubscriptionTitle(subscription.getUrl(), parser.nextText());
							} else if (name.equalsIgnoreCase("link")) {
								if (onItem)
									podcastValues.put(PodcastProvider.COLUMN_LINK, parser.nextText());
							} else if (name.equalsIgnoreCase("description")) {
								if (onItem)
									podcastValues.put(PodcastProvider.COLUMN_DESCRIPTION, parser.nextText());
							} else if (name.equalsIgnoreCase("pubDate")) {
								if (onItem)
									podcastValues.put(PodcastProvider.COLUMN_PUB_DATE, parser.nextText());
							} else if (name.equalsIgnoreCase("enclosure")) {
								if (onItem)
									podcastValues.put(PodcastProvider.COLUMN_MEDIA_URL, parser.getAttributeValue(null, "url"));
							} else if (name.equalsIgnoreCase("thumbnail") &&
									parser.getNamespace() == NAMESPACE_MEDIA) {
								String thumbnail = parser.getAttributeValue(null, "url");
								updateSubscriptionThumbnail(dbAdapter, subscription, thumbnail);
							} else if (name.equalsIgnoreCase("image") &&
									parser.getNamespace() == NAMESPACE_ITUNES) {
								String thumbnail = parser.getAttributeValue(null, "href");
								updateSubscriptionThumbnail(dbAdapter, subscription, thumbnail);
							}
							break;
						case XmlPullParser.END_TAG:
							name = parser.getName();
							if (name.equalsIgnoreCase("item")) {
								String mediaUrl = podcastValues.getAsString(PodcastProvider.COLUMN_MEDIA_URL);
								if (mediaUrl != null && mediaUrl.length() > 0)
									_context.getContentResolver().insert(PodcastProvider.URI, podcastValues);
								podcastValues = null;
							} else if (name.equalsIgnoreCase("channel")) {
								done = true;
							}
							break;
						}
						eventType = parser.next();
					} while (!done && eventType != XmlPullParser.END_DOCUMENT);
					
					updateUpdateNotification(subscription, "Saving...");
					if (lastBuildDate != null && lastBuildDate.getTime() != subscription.getLastModified().getTime())
						dbAdapter.updateSubscriptionLastModified(subscription, lastBuildDate);
					dbAdapter.updateSubscriptionETag(subscription, response.getLastHeader("ETag").getValue());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				removeUpdateNotification();
				_runningThread = null;

				_context.sendBroadcast(new Intent(Constants.ACTION_SUBSCRIPTION_UPDATE_BROADCAST));
				UpdateService.downloadPodcasts(_context);
			}
		}

		public void sendSubscriptionsToPodaxServer(final DBAdapter dbAdapter)
				throws MalformedURLException, IOException, ProtocolException {
			if (PreferenceManager.getDefaultSharedPreferences(_context).getBoolean("usageDataPref", true) == false)
				return;

			Log.d("Podax", "Sending usage data - subscription list");

			URL podaxServer = new URL("http://www.axelby.com/podax.php");
			HttpURLConnection podaxConn = (HttpURLConnection)podaxServer.openConnection();
			podaxConn.setRequestMethod("POST");
			podaxConn.setDoOutput(true);
			podaxConn.setDoInput(true);
			podaxConn.connect();
			
			OutputStreamWriter wr = new OutputStreamWriter(podaxConn.getOutputStream());
			wr.write("inst=");
			wr.write(Installation.id(_context));
			Vector<Subscription> subscriptions = dbAdapter.getSubscriptions();
			for (int i = 0; i < subscriptions.size(); ++i) {
				wr.write("&sub[");
				wr.write(i);
				wr.write("]=");
				wr.write(URLEncoder.encode(subscriptions.get(i).getUrl()));
			}
			wr.flush();
			
			BufferedReader rd = new BufferedReader(new InputStreamReader(podaxConn.getInputStream()));
			Vector<String> response = new Vector<String>();
			String line;
			while ((line = rd.readLine()) != null)
				response.add(line);
			if (response.size() > 1 || !response.get(0).equals("OK")) {
				Log.w("Podax", "Podax server error");
				Log.w("Podax", "------------------");
				for (String s : response)
					Log.w("Podax", s);
			}
		}

		private void updateSubscriptionThumbnail(DBAdapter dbAdapter, Subscription subscription, String thumbnail)
				throws IOException, MalformedURLException {
			try {
				if (subscription.getThumbnail() == null || !subscription.getThumbnail().equals(thumbnail)) {
					InputStream thumbIn = new URL(thumbnail).openStream();
					FileOutputStream thumbOut = new FileOutputStream(subscription.getThumbnailFilename());
					byte[] buffer = new byte[1024];
				    int bufferLength = 0;
				    while ( (bufferLength = thumbIn.read(buffer)) > 0 )
				        thumbOut.write(buffer, 0, bufferLength);
				    thumbOut.close();
				    thumbIn.close();
					
					dbAdapter.updateSubscriptionThumbnail(subscription, thumbnail);
				}
			} catch (FileNotFoundException ex) {
			}
		}
	};

	private void showUpdateErrorNotification(Subscription subscription, String reason) {
		int icon = R.drawable.icon;
		CharSequence tickerText = "Error Updating Subscription";
		long when = System.currentTimeMillis();			
		Notification notification = new Notification(icon, tickerText, when);
		
		CharSequence contentTitle = "Error updating " + subscription.getDisplayTitle();
		CharSequence contentText = reason;
		Intent notificationIntent = new Intent(_context, SubscriptionListActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(_context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(_context, contentTitle, contentText, contentIntent);
		
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(ns);
		notificationManager.notify(Constants.SUBSCRIPTION_UPDATE_ERROR, notification);
	}
	

	private void updateUpdateNotification(Subscription subscription, String status) {
		int icon = R.drawable.icon;
		CharSequence tickerText = "Updating Subscriptions";
		long when = System.currentTimeMillis();			
		Notification notification = new Notification(icon, tickerText, when);
		
		CharSequence contentTitle = "Updating " + subscription.getDisplayTitle();
		CharSequence contentText = status;
		Intent notificationIntent = new Intent(_context, SubscriptionUpdater.class);
		PendingIntent contentIntent = PendingIntent.getActivity(_context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(_context, contentTitle, contentText, contentIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(ns);
		notificationManager.notify(Constants.SUBSCRIPTION_UPDATE_ONGOING, notification);
	}
	
	private void removeUpdateNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(ns);
		notificationManager.cancel(Constants.SUBSCRIPTION_UPDATE_ONGOING);
	}
}