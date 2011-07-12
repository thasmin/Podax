package com.axelby.podax;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.TimerTask;
import java.util.Vector;

import com.axelby.podax.R.drawable;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

class PodcastDownloadTimerTask extends TimerTask {
	private final UpdateService subscriptionUpdateService;

	PodcastDownloadTimerTask(UpdateService subscriptionUpdateService) {
		this.subscriptionUpdateService = subscriptionUpdateService;
	}

	private boolean _isRunning = false;
	
	@Override
	public void run() {
		if (_isRunning)
			return;
		_isRunning = true;
		
		// find next podcast to download - in queue and filesize is null or wrong
		DBAdapter dbAdapter = DBAdapter.getInstance(this.subscriptionUpdateService);
		Vector<Integer> toProcess = dbAdapter.getQueueIds();
		
		for (Integer podcastId : toProcess) {
			Podcast podcast = dbAdapter.loadPodcast(podcastId);
			if (!podcast.needsDownload())
				continue;

			InputStream instream = null;
			File mediaFile = new File(podcast.getFilename());
			
			try {
				Log.d("Podax", "Downloading " + podcast.getTitle());
				updateDownloadNotification(this.subscriptionUpdateService, podcast, 0);
				dbAdapter.updateActiveDownloadId(podcast.getId());

				URL u = new URL(podcast.getMediaUrl());
				HttpURLConnection c = (HttpURLConnection)u.openConnection();
				if (mediaFile.exists())
					c.setRequestProperty("Range", "bytes=" + mediaFile.length() + "-");
				
				// response code 206 means partial content and range header worked
				boolean append = false;
				long downloaded = 0;
				if (c.getResponseCode() == 206) {
					downloaded = mediaFile.length();
					append = true;
				}
				else {
					int fileSize = c.getContentLength();
					podcast.setFileSize(fileSize);
					dbAdapter.savePodcast(podcast);
				}
				
				FileOutputStream outstream = new FileOutputStream(mediaFile, append);
				instream = c.getInputStream();
				int read;
				byte[] b = new byte[1024*64];
				while ((read = instream.read(b, 0, b.length)) != -1)
				{
					outstream.write(b, 0, read);
					downloaded += read;
					updateDownloadNotification(this.subscriptionUpdateService, podcast, downloaded);
				}
				instream.close();
				
				Log.d("Podax", "Done downloading " + podcast.getTitle());
			} catch (Exception e) {
				Log.d("Podax", "Exception while downloading " + podcast.getTitle() + ": " + e.getMessage());
				removeDownloadNotification(this.subscriptionUpdateService);
				dbAdapter.updateActiveDownloadId(null);

				e.printStackTrace();
				this.subscriptionUpdateService._timer.schedule(this, this.subscriptionUpdateService.ONEMINUTE);
				try {
					if (instream != null)
						instream.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
				break;
			}

			removeDownloadNotification(this.subscriptionUpdateService);
			dbAdapter.updateActiveDownloadId(null);
		}
		_isRunning = false;
	}

	final int HELLO_ID = 2;
	void updateDownloadNotification(UpdateService updateService, Podcast podcast, long downloaded) {
		int icon = drawable.icon;
		CharSequence tickerText = "Downloading podcast: " + podcast.getTitle();
		long when = System.currentTimeMillis();			
		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = updateService.getApplicationContext();
		CharSequence contentTitle = "Downloading Podcast";
		CharSequence contentText = podcast.getTitle();
		if (podcast.getFileSize() != null)
		{
			String pct = Integer.toString((int)(100.0f * downloaded / podcast.getFileSize()));
			contentText = pct + "% of " + contentText;
		}
		Intent notificationIntent = new Intent(updateService, UpdateService.class);
		PendingIntent contentIntent = PendingIntent.getActivity(updateService, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) updateService.getSystemService(ns);
		notificationManager.notify(HELLO_ID, notification);
	}

	void removeDownloadNotification(UpdateService updateService) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) updateService.getSystemService(ns);
		notificationManager.cancel(HELLO_ID);
	}
}