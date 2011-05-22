package com.axelby.podax;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.TimerTask;
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

class UpdateSubscriptionTimerTask extends TimerTask {
	private final UpdateService subscriptionUpdateService;

	UpdateSubscriptionTimerTask(UpdateService subscriptionUpdateService) {
		this.subscriptionUpdateService = subscriptionUpdateService;
	}

	private boolean _isRunning = false;
	
	@Override
	public void run() {
		if (_isRunning || this.subscriptionUpdateService._toUpdate.size() == 0)
			return;
		_isRunning = true;

		// remove the subscriptions I will update from toUpdate
    	Vector<Subscription> update = new Vector<Subscription>();
    	update.addAll(this.subscriptionUpdateService._toUpdate);
    	this.subscriptionUpdateService._toUpdate.clear();

    	try {
			final DBAdapter dbAdapter = DBAdapter.getInstance(this.subscriptionUpdateService);
	        while (update.size() > 0) {
	        	final Subscription subscription = update.get(0);
	        	update.remove(0);

	        	updateUpdateNotification(subscription, "Setting up...");

				final Vector<RssPodcast> podcasts = new Vector<RssPodcast>();
	        	final RssPodcast podcast = new RssPodcast(subscription);
	        	RootElement root = new RootElement("rss");

	        	Element channel = root.getChild("channel");
	        	channel.getChild("title").setEndTextElementListener(new EndTextElementListener() {
	        		public void end(String body) {
	        			//Log.d("Podax", "Subscription title set to " + body);
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
    		
    		this.subscriptionUpdateService.sendBroadcast(new Intent(NotificationIds.SUBSCRIPTION_UPDATE_BROADCAST));
    	}
	}

	private void showUpdateErrorNotification(Subscription subscription, String reason) {
		int icon = R.drawable.icon;
		CharSequence tickerText = "Error Updating Subscription";
		long when = System.currentTimeMillis();			
		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = this.subscriptionUpdateService.getApplicationContext();
		CharSequence contentTitle = "Error updating " + subscription.getDisplayTitle();
		CharSequence contentText = reason;
		Intent notificationIntent = new Intent(this.subscriptionUpdateService, SubscriptionListActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this.subscriptionUpdateService, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) this.subscriptionUpdateService.getSystemService(ns);
		notificationManager.notify(NotificationIds.SUBSCRIPTION_UPDATE_ERROR, notification);
	}
	

	private void updateUpdateNotification(Subscription subscription, String status) {
		int icon = R.drawable.icon;
		CharSequence tickerText = "Updating Subscriptions";
		long when = System.currentTimeMillis();			
		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = this.subscriptionUpdateService.getApplicationContext();
		CharSequence contentTitle = "Updating " + subscription.getDisplayTitle();
		CharSequence contentText = status;
		Intent notificationIntent = new Intent(this.subscriptionUpdateService, UpdateSubscriptionTimerTask.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this.subscriptionUpdateService, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) this.subscriptionUpdateService.getSystemService(ns);
		notificationManager.notify(NotificationIds.SUBSCRIPTION_UPDATE_ONGOING, notification);
	}
	
	private void removeUpdateNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) this.subscriptionUpdateService.getSystemService(ns);
		notificationManager.cancel(NotificationIds.SUBSCRIPTION_UPDATE_ONGOING);
	}
}