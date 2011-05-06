package com.axelby.podax;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class PodcastDownloadService extends Service {
	private final long ONEMINUTE = 1000 * 60;

	private Timer _timer = new Timer();
	private PodcastDownloadTimerTask _downloadTask = new PodcastDownloadTimerTask();
	
	private PodcastDownloadBinder _binder = new PodcastDownloadBinder();
	public class PodcastDownloadBinder extends Binder {
		PodcastDownloadService getService() {
			return PodcastDownloadService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return _binder;
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
		_timer.scheduleAtFixedRate(_downloadTask, 1000, 3000);
	}

	private class PodcastDownloadTimerTask extends TimerTask {
		private boolean _isRunning = false;
		
		@Override
		public void run() {
			if (_isRunning)
				return;
			_isRunning = true;
			
			Log.d("Podax", "PodcastDownloadTimerTask run");
			
			// find next podcast to download - in queue and filesize is null or wrong
			DBAdapter dbAdapter = DBAdapter.getInstance(PodcastDownloadService.this);
			Vector<Integer> toProcess = new Vector<Integer>();
			toProcess.addAll(dbAdapter.getQueueIds());
			
			for (Integer podcastId : toProcess) {
				Podcast podcast = dbAdapter.loadPodcast(podcastId);
				File mediaFile = new File(podcast.getFilename());
				try {
					if (podcast.getMediaUrl().length() == 0 || podcast.isDownloaded())
						continue;
				} catch (Exception e) {
					e.printStackTrace();
					_timer.schedule(this, ONEMINUTE);
					return;
				}

				// download file and update notification
				if (!mediaFile.exists() || podcast.getFileSize() == null || mediaFile.length() != podcast.getFileSize())
				{
					InputStream instream = null;
					
					try {
						Log.d("Podax", "Downloading " + podcast.getTitle());
						updateNotification(podcast, 0);
						
						mediaFile.createNewFile();
						FileOutputStream outstream = new FileOutputStream(mediaFile);

						URL u = new URL(podcast.getMediaUrl());
						URLConnection c = u.openConnection();
						int fileSize = c.getContentLength();
						podcast.setFileSize(fileSize);
						dbAdapter.savePodcast(podcast);

						instream = c.getInputStream();
						int read, downloaded = 0;
						byte[] b = new byte[1024*64];
						while ((read = instream.read(b, 0, b.length)) != -1)
						{
							outstream.write(b, 0, read);
							downloaded += read;
							updateNotification(podcast, downloaded);
						}
						instream.close();
						
						Log.d("Podax", "Done downloading " + podcast.getTitle());
					} catch (Exception e) {
						Log.d("Podax", "Exception while downloading " + podcast.getTitle() + ": " + e.getMessage());
						removeNotification();
						e.printStackTrace();
						_timer.schedule(this, ONEMINUTE);
						try {
							if (instream != null)
								instream.close();
						} catch (IOException e2) {
							e2.printStackTrace();
						}
						break;
					}
					removeNotification();
				}
			}
			
			_isRunning = false;
		}

	}

	private final int HELLO_ID = 2;
	private void updateNotification(Podcast podcast, int downloaded) {
		int icon = R.drawable.icon;
		CharSequence tickerText = "Downloading podcast: " + podcast.getTitle();
		long when = System.currentTimeMillis();			
		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "Downloading Podcast";
		CharSequence contentText = podcast.getTitle();
		if (podcast.getFileSize() != null)
			contentText = contentText + " " + Integer.toString((int)(100.0f * downloaded / podcast.getFileSize())) + "%";
		Intent notificationIntent = new Intent(this, PodcastDownloadTimerTask.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
		notificationManager.notify(HELLO_ID, notification);
	}
	
	private void removeNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
		notificationManager.cancel(HELLO_ID);
	}
}
