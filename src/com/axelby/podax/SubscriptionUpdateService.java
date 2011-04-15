package com.axelby.podax;

import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.xml.sax.Attributes;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;

import com.axelby.podax.PodcastDownloadService.PodcastDownloadBinder;

public class SubscriptionUpdateService extends Service {
	private Timer _timer = new Timer();
	private Vector<Subscription> _toUpdate = new Vector<Subscription>();
	private UpdateRSSTimerTask _rssTask = new UpdateRSSTimerTask();
	private RefreshSubscriptionsTimerTask _refreshTask = new RefreshSubscriptionsTimerTask();

	private PodcastDownloadService _downloader = null;
	public class PodcastDownloadConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName name, IBinder service) {
			_downloader = ((PodcastDownloadBinder)service).getService();
		}
		public void onServiceDisconnected(ComponentName name) {
		}		
	}
	
	public void updateSubscription(Subscription subscription) {
		_toUpdate.add(subscription);
	}

	private SubscriptionUpdateBinder _binder = new SubscriptionUpdateBinder();
	public class SubscriptionUpdateBinder extends Binder {
		SubscriptionUpdateService getService() {
			return SubscriptionUpdateService.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return _binder;
	}

	@Override
	public void onCreate() {
	}

	@Override
	public void onDestroy() {
	}

	@Override
	public void onStart(Intent intent, int startId) {
	    handleCommand(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    handleCommand(intent);
	    return START_NOT_STICKY;
	}
	
	private void handleCommand(Intent intent) {
		Log.d("PodcastUpdateService", "onStart");
        // check the queue every second
		_timer.scheduleAtFixedRate(_rssTask, 1000, 1000);
		// populate the queue with all of the scriptions every hour
		_timer.scheduleAtFixedRate(_refreshTask, 1000, 1000 * 60 * 60);
		
		Intent service = new Intent(this, PodcastDownloadService.class);
		bindService(service, new PodcastDownloadConnection(), 0);
	}
	
	private class RefreshSubscriptionsTimerTask extends TimerTask {
		@Override
		public void run() {
			Log.d("Podax", "RefreshSubscriptionsTimerTask");
			final DBAdapter dbAdapter = DBAdapter.getInstance(SubscriptionUpdateService.this);
	        _toUpdate.addAll( dbAdapter.getUpdatableSubscriptions() );
		}
	}

	private class UpdateRSSTimerTask extends TimerTask {
		private boolean _isRunning = false;
		
		@Override
		public void run() {
			if (_isRunning || _toUpdate.size() == 0)
				return;
			_isRunning = true;
				
			Log.d("Podax", "UpdateRSSTimerTask");

			try {
				final DBAdapter dbAdapter = DBAdapter.getInstance(SubscriptionUpdateService.this);
		        while (_toUpdate.size() > 0) {
		        	final Subscription subscription = _toUpdate.get(0);
		        	_toUpdate.remove(0);
	
		        	updateNotification(subscription, "Setting up...");
	
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
		    				updateNotification(subscription, podcast.getTitle());
		        			//Log.d("Podax", "Saving podcast " + podcast.getTitle());
		        			podcasts.add((RssPodcast)podcast.clone());
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
			        
					updateNotification(subscription, "Downloading Feed");
		        	InputStream response = Downloader.downloadInputStream(subscription.getUrl());
		        	if (response == null) {
		        		showErrorNotification(subscription, "Feed not available. Check the URL.");
		        	} else {
		        		Xml.parse(response, Xml.Encoding.UTF_8, root.getContentHandler());
		            	
			        	Log.d("Podax", "Saving podcasts");
						updateNotification(subscription, "Saving...");
		        		dbAdapter.updatePodcastsFromFeed(podcasts);
		        		if (_downloader != null)
		        		{
			        		Log.d("Podax", "Update is triggering download");
		        			_downloader.downloadPodcasts();
		        		}
		        		Log.d("Podax", "Done saving podcasts");
		        	}
		        }
        	} catch (Exception e) {
        		e.printStackTrace();
        	} finally {
        		removeNotification();
        		_isRunning = false;
        		
        		sendBroadcast(new Intent(NotificationIds.SUBSCRIPTION_UPDATE_BROADCAST));
        	}
		}
		
		private void showErrorNotification(Subscription subscription, String reason) {
			int icon = R.drawable.icon;
			CharSequence tickerText = "Error Updating Subscription";
			long when = System.currentTimeMillis();			
			Notification notification = new Notification(icon, tickerText, when);
			
			Context context = getApplicationContext();
			CharSequence contentTitle = "Podax: Error updating " + subscription.getDisplayTitle();
			CharSequence contentText = reason;
			Intent notificationIntent = new Intent(SubscriptionUpdateService.this, SubscriptionListActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(SubscriptionUpdateService.this, 0, notificationIntent, 0);
			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
			
			String ns = Context.NOTIFICATION_SERVICE;
			NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
			notificationManager.notify(NotificationIds.SUBSCRIPTION_UPDATE_ERROR, notification);
		}
		

		private void updateNotification(Subscription subscription, String status) {
			int icon = R.drawable.icon;
			CharSequence tickerText = "Updating Subscriptions";
			long when = System.currentTimeMillis();			
			Notification notification = new Notification(icon, tickerText, when);
			
			Context context = getApplicationContext();
			CharSequence contentTitle = "Podax: Updating " + subscription.getDisplayTitle();
			CharSequence contentText = status;
			Intent notificationIntent = new Intent(SubscriptionUpdateService.this, UpdateRSSTimerTask.class);
			PendingIntent contentIntent = PendingIntent.getActivity(SubscriptionUpdateService.this, 0, notificationIntent, 0);
			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			
			String ns = Context.NOTIFICATION_SERVICE;
			NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
			notificationManager.notify(NotificationIds.SUBSCRIPTION_UPDATE_ONGOING, notification);
		}
		
		private void removeNotification() {
			String ns = Context.NOTIFICATION_SERVICE;
			NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
			notificationManager.cancel(NotificationIds.SUBSCRIPTION_UPDATE_ONGOING);
		}
	}
}
