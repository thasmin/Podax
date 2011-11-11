package com.axelby.podax;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
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
			if (!PodaxApp.ensureWifi(_context))
				return;

			DBAdapter dbAdapter = DBAdapter.getInstance(_context);
			Cursor cursor = null;
			try {
				_isRunning = true;
	
				Uri uri = Uri.withAppendedPath(PodcastProvider.URI, "to_download");
				String[] projection = {
						PodcastProvider.COLUMN_ID,
						PodcastProvider.COLUMN_TITLE,
						PodcastProvider.COLUMN_MEDIA_URL,
				};
				cursor = _context.getContentResolver().query(uri, projection, null, null, null);
				while (cursor.moveToNext()) {
					InputStream instream = null;
					PodcastCursor podcast = new PodcastCursor(_context, cursor);
					File mediaFile = new File(podcast.getFilename());
	
					try {
						Log.d("Podax", "Downloading " + podcast.getTitle());
						updateDownloadNotification(podcast, 0);
						dbAdapter.updateActiveDownloadId(podcast.getId());
	
						URL u = new URL(podcast.getMediaUrl());
						HttpURLConnection c = (HttpURLConnection)u.openConnection();
						if (mediaFile.exists() && mediaFile.length() > 0)
							c.setRequestProperty("Range", "bytes=" + mediaFile.length() + "-");
	
						// response code 206 means partial content and range header worked
						boolean append = false;
						if (c.getResponseCode() == 206) {
							if (c.getContentLength() <= 0) {
								podcast.setFileSize(mediaFile.length());
								continue;
							}
							append = true;
						}
						else {
							podcast.setFileSize(c.getContentLength());
						}
	
						FileOutputStream outstream = new FileOutputStream(mediaFile, append);
						instream = c.getInputStream();
						int read;
						byte[] b = new byte[1024*64];
						while ((read = instream.read(b, 0, b.length)) != -1)
							outstream.write(b, 0, read);
						instream.close();
						outstream.close();

						MediaPlayer mp = new MediaPlayer();
						mp.setDataSource(podcast.getFilename());
						mp.prepare();
						podcast.setDuration(mp.getDuration());
						mp.release();
	
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
			} catch (MissingFieldException e) {
				e.printStackTrace();
			}
			finally {
				if (cursor != null)
					cursor.close();
				removeDownloadNotification();
				dbAdapter.updateActiveDownloadId(null);

				_isRunning = false;
			}
		}
	};
	

	void updateDownloadNotification(PodcastCursor podcast, long downloaded) throws MissingFieldException {
		int icon = drawable.icon;
		CharSequence tickerText = "Downloading podcast: " + podcast.getTitle();
		long when = System.currentTimeMillis();			
		Notification notification = new Notification(icon, tickerText, when);
		
		CharSequence contentTitle = "Downloading Podcast";
		CharSequence contentText = podcast.getTitle();
		Intent notificationIntent = new Intent(_context, ActiveDownloadListActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(_context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(_context, contentTitle, contentText, contentIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(ns);
		notificationManager.notify(Constants.PODCAST_DOWNLOAD_ONGOING, notification);
	}

	void removeDownloadNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(ns);
		notificationManager.cancel(Constants.PODCAST_DOWNLOAD_ONGOING);
	}
}