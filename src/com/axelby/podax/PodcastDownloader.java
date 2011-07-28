package com.axelby.podax;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.axelby.podax.R.drawable;

class PodcastDownloader {
	private Context _context;
	private boolean _isRunning = false;

	PodcastDownloader(Context context) {
		_context = context;
	}
	
	public void download() {
		new Thread(_worker).start();
	}
	
	public Runnable _worker = new Runnable() {
		public void run() {
			if (_isRunning)
				return;
			
			DBAdapter dbAdapter = DBAdapter.getInstance(_context);

			try {
				_isRunning = true;
	
				Vector<Podcast> toProcess = dbAdapter.getQueue();
				for (Podcast podcast : toProcess) {
					if (!podcast.needsDownload())
						continue;
	
					InputStream instream = null;
					File mediaFile = new File(podcast.getFilename());
	
					try {
						Log.d("Podax", "Downloading " + podcast.getTitle());
						updateDownloadNotification(podcast, 0);
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
							podcast.setFileSize(c.getContentLength());
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
							updateDownloadNotification(podcast, downloaded);
						}
						instream.close();
						outstream.close();
	
						Log.d("Podax", "Done downloading " + podcast.getTitle());
					} catch (Exception e) {
						Log.d("Podax", "Exception while downloading " + podcast.getTitle() + ": " + e.getMessage());
						removeDownloadNotification();
						dbAdapter.updateActiveDownloadId(null);
	
						try {
							if (instream != null)
								instream.close();
						} catch (IOException e2) {
							e2.printStackTrace();
						}
						break;
					}
				}
			}
			finally {
				removeDownloadNotification();
				dbAdapter.updateActiveDownloadId(null);

				_isRunning = false;
			}
		}
	};
	

	void updateDownloadNotification(Podcast podcast, long downloaded) {
		int icon = drawable.icon;
		CharSequence tickerText = "Downloading podcast: " + podcast.getTitle();
		long when = System.currentTimeMillis();			
		Notification notification = new Notification(icon, tickerText, when);
		
		CharSequence contentTitle = "Downloading Podcast";
		CharSequence contentText = podcast.getTitle();
		if (podcast.getFileSize() != null)
		{
			String pct = Integer.toString((int)(100.0f * downloaded / podcast.getFileSize()));
			contentText = pct + "% of " + contentText;
		}
		Intent notificationIntent = new Intent(_context, ActiveDownloadListActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(_context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(_context, contentTitle, contentText, contentIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(ns);
		notificationManager.notify(NotificationIds.PODCAST_DOWNLOAD_ONGOING, notification);
	}

	void removeDownloadNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(ns);
		notificationManager.cancel(NotificationIds.PODCAST_DOWNLOAD_ONGOING);
	}
}