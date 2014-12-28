package com.axelby.podax;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.axelby.podax.ui.MainActivity;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import okio.BufferedSource;

public class EpisodeDownloader {
	private final static ArrayList<String> _currentlyDownloading = new ArrayList<>(5);
	public static boolean isDownloading(String filename) { return _currentlyDownloading.contains(filename); }

	private EpisodeDownloader() { }

	public static void download(Context context, long episodeId) {
		EpisodeCursor episode = null;
		FileOutputStream outStream = null;
		File mediaFile = null;
		try {
			if (Helper.isInvalidNetworkState(context)) {
				Log.d("EpisodeDownloader", "not downloading for wifi pref");
				return;
			}

			episode = EpisodeCursor.getCursor(context, episodeId);
			if (episode == null) {
				Log.d("EpisodeDownloader", "episode cursor is null");
				return;
			}
			if (episode.isDownloaded(context)) {
				Log.d("EpisodeDownloader", "episode is already downloaded");
				return;
			}

			// don't do two downloads simultaneously
			if (isDownloading(episode.getFilename(context))) {
				Log.d("EpisodeDownloader", "episode is already being downloaded");
				return;
			}

			mediaFile = new File(episode.getFilename(context));
			_currentlyDownloading.add(mediaFile.getAbsolutePath());
			outStream = new FileOutputStream(mediaFile, true);

			File indexFile = new File(episode.getIndexFilename(context));
			if (indexFile.exists())
				indexFile.delete();

			showNotification(context, episode);

			OkHttpClient client = new OkHttpClient();
			Request.Builder url = new Request.Builder().url(episode.getMediaUrl());
			if (mediaFile.exists() && mediaFile.length() > 0)
				url.addHeader("Range", "bytes=" + mediaFile.length() + "-");
			Response response = client.newCall(url.build()).execute();
			if (response.code() != 200 && response.code() != 206) {
				Log.d("EpisodeDownloader", "response not 200 or 206");
				return;
			}

			ResponseBody body = response.body();
			ContentValues values = new ContentValues(1);
			values.put(EpisodeProvider.COLUMN_FILE_SIZE, body.contentLength());
			context.getContentResolver().update(episode.getContentUri(), values, null, null);

			BufferedSource source = body.source();
			byte[] b = new byte[100000];
			while (!source.exhausted()) {
				int read = source.read(b);
				outStream.write(b, 0, read);
			}

			episode.determineDuration(context);
		} catch (Exception e) {
			showErrorNotification(context, episode, e);
			Log.e("Podax", "error while downloading", e);
		} finally {
			if (mediaFile != null)
				_currentlyDownloading.remove(mediaFile.getAbsolutePath());
			hideNotification(context);

			if (episode != null)
				episode.closeCursor();

			try {
				if (outStream != null)
					outStream.close();
			} catch (IOException e) {
				Log.e("EpisodeDownloader", "unable to close output stream", e);
			}
		}
	}

	private static void showErrorNotification(Context context, EpisodeCursor podcast, Exception e) {
		Intent notificationIntent = new Intent(context, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		Notification notification = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.ic_stat_icon)
				.setTicker("Error downloading " + podcast.getTitle())
				.setWhen(System.currentTimeMillis())
				.setContentTitle("Error Downloading Podcast Episode")
				.setContentText(e.getMessage())
				.setContentIntent(contentIntent)
				.setOngoing(true)
				.build();
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(Constants.NOTIFICATION_DOWNLOADING, notification);
	}

	private static void showNotification(Context context, EpisodeCursor podcast) {
		Intent notificationIntent = new Intent(context, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		Notification notification = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.ic_stat_icon)
				.setTicker("Downloading " + podcast.getTitle())
				.setWhen(System.currentTimeMillis())
				.setContentTitle("Downloading Podcast Episode")
				.setContentText(podcast.getTitle())
				.setContentIntent(contentIntent)
				.setOngoing(true)
				.build();
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(Constants.NOTIFICATION_DOWNLOADING, notification);
	}

	private static void hideNotification(Context context) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(ns);
		notificationManager.cancel(Constants.NOTIFICATION_DOWNLOADING);
	}
}