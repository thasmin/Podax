package com.axelby.podax;

import java.io.Closeable;
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
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.axelby.podax.R.drawable;
import com.axelby.podax.ui.PodcastDetailActivity;

class PodcastDownloader {
	private Context _context;

	PodcastDownloader(Context context) {
		_context = context;
	}
	
	public void download(long podcastId) {
		if (!Helper.ensureWifi(_context))
			return;

		Cursor cursor = null;
		try {
			String[] projection = {
					PodcastProvider.COLUMN_ID,
					PodcastProvider.COLUMN_TITLE,
					PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
					PodcastProvider.COLUMN_MEDIA_URL,
					PodcastProvider.COLUMN_FILE_SIZE,
			};
			cursor = _context.getContentResolver().query(PodcastProvider.QUEUE_URI, projection,
					"podcasts._id = ?",
					new String[] { String.valueOf(podcastId) }, null);
			if (!cursor.moveToNext())
				return;

			PodcastCursor podcast = new PodcastCursor(cursor);
			if (podcast.isDownloaded())
				return;

			showNotification(podcast);

			File mediaFile = new File(podcast.getFilename());

			Log.d("Podax", "Downloading " + podcast.getTitle());

			HttpURLConnection c = openConnection(podcast, mediaFile);
			if (c == null)
				return;

			if (!downloadFile(c, mediaFile))
				return;

			if (mediaFile.length() == c.getContentLength())
				podcast.determineDuration(_context);

			// if there's no active podcast, this becomes active so tell the status to refresh
			Helper.updateWidgets(_context);

			Log.d("Podax", "Done downloading " + podcast.getTitle());
		} catch (Exception e) {
			Log.e("Podax", "error while downloading", e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	// returns null if connection should not be used (404, already downloaded, etc)
	private HttpURLConnection openConnection(PodcastCursor podcast, File mediaFile) {
		try {
			URL u = new URL(podcast.getMediaUrl());
			HttpURLConnection c = (HttpURLConnection)u.openConnection();
			if (mediaFile.exists() && mediaFile.length() > 0)
				c.setRequestProperty("Range", "bytes=" + mediaFile.length() + "-");

			// response code 416 means range is invalid
			if (c.getResponseCode() == 416) {
				mediaFile.delete();
				c = (HttpURLConnection)u.openConnection();
			}

			// only valid response codes are 200 and 206
			if (c.getResponseCode() != 200 && c.getResponseCode() != 206)
				return null;

			// response code 206 means partial content and range header worked
			if (c.getResponseCode() == 206) {
				// make sure there's more data to download
				if (c.getContentLength() <= 0) {
					podcast.setFileSize(_context, mediaFile.length());
					return null;
				}
			} else {
				podcast.setFileSize(_context, c.getContentLength());
				// all content returned so delete existing content
				mediaFile.delete();
			}
			return c;
		} catch (IOException ex) {
			Log.e("Podax", "Unable to open connection to " + podcast.getMediaUrl(), ex);
			return null;
		}
	}

	private boolean downloadFile(HttpURLConnection conn, File file) {
		FileOutputStream outstream = null;
		InputStream instream = null;
		try {
			// file was deleted if accept-range header didn't work so always append
			outstream = new FileOutputStream(file, true);
			instream = conn.getInputStream();
			int read;
			byte[] b = new byte[1024*64];
			while (!Thread.currentThread().isInterrupted() &&
					(read = instream.read(b)) != -1)
				outstream.write(b, 0, read);
		} catch (Exception e) {
			Log.e("Podax", "Interrupted while downloading " + conn.getURL().toExternalForm(), e);
			return false;
		} finally {
			close(outstream);
			close(instream);
		}
		return file.length() == conn.getContentLength();
	}

	void showNotification(PodcastCursor podcast) {
		Intent notificationIntent = new Intent(_context, PodcastDetailActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(_context, 0, notificationIntent, 0);

		Notification notification = new NotificationCompat.Builder(_context)
			.setSmallIcon(drawable.icon)
			.setWhen(System.currentTimeMillis())
			.setTicker("Downloading " + podcast.getTitle())
			.setContentTitle("Downloading " + podcast.getTitle())
			.setContentText(podcast.getSubscriptionTitle())
			.setContentIntent(contentIntent)
			.getNotification();

		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(Constants.NOTIFICATION_UPDATE, notification);
	}

	public static void close(Closeable c) {
		if (c == null)
			return;
		try {
			c.close();
		} catch (IOException e) {
		}
	}
}