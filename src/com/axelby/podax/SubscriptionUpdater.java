package com.axelby.podax;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Vector;

import org.xml.sax.Attributes;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

class SubscriptionUpdater {
	// TODO: allow subscriptions to be added to running process
	private static boolean _isRunning = false;
	private Context _context;
	Vector<Integer> _toUpdate = new Vector<Integer>();
	
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
		if (_isRunning)
			return;
		_isRunning = true;
		new Thread(_worker).start();
	}
	
	private Runnable _worker = new Runnable() {
		public void run() {
			try {
				final DBAdapter dbAdapter = DBAdapter.getInstance(_context);
				while (_toUpdate.size() > 0) {
					final Subscription subscription = dbAdapter.loadSubscription(_toUpdate.get(0));
					_toUpdate.remove(0);

					updateUpdateNotification(subscription, "Setting up...");

					final Vector<RssPodcast> podcasts = new Vector<RssPodcast>();
					final RssPodcast podcast = new RssPodcast(subscription);
					RootElement root = new RootElement("rss");

					Element channel = root.getChild("channel");
					channel.getChild("title").setEndTextElementListener(new EndTextElementListener() {
						public void end(String body) {
							dbAdapter.updateSubscriptionTitle(subscription.getUrl(), body);
						}
					});

					Element item = channel.getChild("item");

					item.setEndElementListener(new EndElementListener() {
						public void end() {
							if (podcast.getMediaUrl() != null && podcast.getMediaUrl().length() > 0) {
								updateUpdateNotification(subscription, podcast.getTitle());
								podcasts.add((RssPodcast)podcast.clone());
							}
							podcast.setTitle(null);
							podcast.setLink(null);
							podcast.setDescription(null);
							podcast.setPubDate(null);
							podcast.setMediaUrl(null);
						}
					});
					item.getChild("title").setEndTextElementListener(new EndTextElementListener(){
						public void end(String body) {
							podcast.setTitle(body);
						}
					});
					item.getChild("link").setEndTextElementListener(new EndTextElementListener(){
						public void end(String body) {
							podcast.setLink(body);
						}
					});
					item.getChild("description").setEndTextElementListener(new EndTextElementListener(){
						public void end(String body) {
							podcast.setDescription(body);
						}
					});
					item.getChild("pubDate").setEndTextElementListener(new EndTextElementListener(){
						public void end(String body) {
							podcast.setPubDate(body);
						}
					});
					item.getChild("http://search.yahoo.com/mrss/", "content").setStartElementListener(new StartElementListener(){
						public void start(Attributes attributes) {
							podcast.setMediaUrl(attributes.getValue("url"));
						}
					});
					item.getChild("enclosure").setStartElementListener(new StartElementListener() {
						public void start(Attributes attributes) {
							podcast.setMediaUrl(attributes.getValue("url"));
						}
					});

					URL u = new URL(subscription.getUrl());
					URLConnection c = u.openConnection();

					Date modified = new Date(c.getLastModified());
					if (subscription.getLastModified() == modified)
						return;

					updateUpdateNotification(subscription, "Downloading Feed");
					InputStream response = c.getInputStream();
					if (response == null) {
						showUpdateErrorNotification(subscription, "Feed not available. Check the URL.");
					} else {
						Xml.parse(response, Xml.Encoding.UTF_8, root.getContentHandler());
						dbAdapter.updateSubscriptionLastModified(subscription, modified);

						updateUpdateNotification(subscription, "Saving...");
						dbAdapter.updatePodcastsFromFeed(podcasts);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				removeUpdateNotification();
				_isRunning = false;

				_context.sendBroadcast(new Intent(NotificationIds.SUBSCRIPTION_UPDATE_BROADCAST));
				UpdateService.downloadPodcasts(_context);
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
		notificationManager.notify(NotificationIds.SUBSCRIPTION_UPDATE_ERROR, notification);
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
		notificationManager.notify(NotificationIds.SUBSCRIPTION_UPDATE_ONGOING, notification);
	}
	
	private void removeUpdateNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(ns);
		notificationManager.cancel(NotificationIds.SUBSCRIPTION_UPDATE_ONGOING);
	}
}