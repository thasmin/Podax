package com.axelby.podax;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.axelby.podax.R.drawable;
import com.axelby.podax.ui.MainActivity;

class PodcastDownloader {
	private Context _context;
	private Thread _thread;

	PodcastDownloader(Context context) {
		_context = context;
	}
	
	public synchronized void download() {
		if (_thread != null)
			return;
		_thread = new Thread(_worker);
		_thread.start();
	}
	
	public Runnable _worker = new Runnable() {
		public void run() {
			if (!Helper.ensureWifi(_context))
				return;

			verifyDownloadedFiles();

			Log.d("Podax", "starting podcast downloader on thread " + Thread.currentThread().getId());

			// keep going through the queue until there are no podcasts left to download
			boolean foundone = true;
			while (foundone) {
				foundone = false;

				Cursor cursor = null;
				try {
					String[] projection = {
							PodcastProvider.COLUMN_ID,
							PodcastProvider.COLUMN_TITLE,
							PodcastProvider.COLUMN_MEDIA_URL,
							PodcastProvider.COLUMN_FILE_SIZE,
					};
					cursor = _context.getContentResolver().query(PodcastProvider.QUEUE_URI, projection, null, null, null);
					while (cursor.moveToNext()) {
						PodcastCursor podcast = new PodcastCursor(_context, cursor);
						if (podcast.isDownloaded())
							continue;

						foundone = true;

						File mediaFile = new File(podcast.getFilename());

						if (PodcastDownloader.this._thread != Thread.currentThread())
							return;

						Log.d("Podax", "Downloading " + podcast.getTitle());
						updateDownloadNotification(podcast, 0);

						HttpURLConnection c = openConnection(podcast, mediaFile);
						if (c == null)
							continue;

						if (!downloadFile(c, mediaFile))
							continue;

						if (mediaFile.length() == c.getContentLength())
							podcast.determineDuration();

						removeDownloadNotification();
						Log.d("Podax", "Done downloading " + podcast.getTitle());
					}
				} finally {
					if (cursor != null)
						cursor.close();
				}
			}

			_thread = null;
		}

		// returns null if connection should not be used (404, already downloaded, etc)
		private HttpURLConnection openConnection(PodcastCursor podcast, File mediaFile) {
			try {
				URL u = new URL(podcast.getMediaUrl());
				HttpURLConnection c = (HttpURLConnection)u.openConnection();
				if (mediaFile.exists() && mediaFile.length() > 0)
					c.setRequestProperty("Range", "bytes=" + mediaFile.length() + "-");

				// only valid response codes are 200 and 206
				if (c.getResponseCode() != 200 && c.getResponseCode() != 206)
					return null;

				// response code 206 means partial content and range header worked
				if (c.getResponseCode() == 206) {
					// make sure there's more data to download
					if (c.getContentLength() <= 0) {
						podcast.setFileSize(mediaFile.length());
						return null;
					}
				} else {
					podcast.setFileSize(c.getContentLength());
					// all content returned so delete existing content
					mediaFile.delete();
				}
				return c;
			} catch (IOException ex) {
				Log.e("Podax", "Unable to open connection to " + podcast.getMediaUrl() + ": " + ex.getMessage());
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
						Thread.currentThread() == PodcastDownloader.this._thread &&
						(read = instream.read(b, 0, b.length)) != -1)
					outstream.write(b, 0, read);
			} catch (Exception e) {
				Log.e("Podax", "Interrupted while downloading " + conn.getURL().toExternalForm());
				return false;
			} finally {
				close(outstream);
				close(instream);
			}
			return file.length() == conn.getContentLength();
		}
	};

	private void verifyDownloadedFiles() {
		Vector<String> validMediaFilenames = new Vector<String>();
		String[] projection = new String[] {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_MEDIA_URL,
		};
		Uri queueUri = Uri.withAppendedPath(PodcastProvider.URI, "queue");
		Cursor c = _context.getContentResolver().query(queueUri, projection, null, null, null);
		while (c.moveToNext())
			validMediaFilenames.add(new PodcastCursor(_context, c).getFilename());
		c.close();

		File dir = new File(PodcastCursor.getStoragePath());
		for (File f : dir.listFiles()) {
			// make sure the file is a media file
			String extension = PodcastCursor.getExtension(f.getName());
			String[] mediaExtensions = new String[] { "mp3", "ogg", "wma", };
			if (Arrays.binarySearch(mediaExtensions, extension) < 0)
				continue;
			if (!validMediaFilenames.contains(f.getAbsolutePath())) {
				Log.w("Podax", "deleting file " + f.getName());
				f.delete();
			}
		}
	}

	public static void close(Closeable c) {
		if (c == null)
			return;
		try {
			c.close();
		} catch (IOException e) {
		}
	}

	void updateDownloadNotification(PodcastCursor podcast, long downloaded) {
		Intent notificationIntent = MainActivity.getSubscriptionIntent(_context);
		PendingIntent contentIntent = PendingIntent.getActivity(_context, 0, notificationIntent, 0);

		Notification notification = new NotificationCompat.Builder(_context)
				.setSmallIcon(drawable.icon)
				.setTicker("Downloading podcast: " + podcast.getTitle())
				.setWhen(System.currentTimeMillis())
				.setContentTitle("Downloading Podcast")
				.setContentText(podcast.getTitle())
				.setContentIntent(contentIntent)
				.setOngoing(true)
				.getNotification();
		
		NotificationManager notificationManager = (NotificationManager) _context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(Constants.PODCAST_DOWNLOAD_ONGOING, notification);
	}

	void removeDownloadNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(ns);
		notificationManager.cancel(Constants.PODCAST_DOWNLOAD_ONGOING);
	}
}