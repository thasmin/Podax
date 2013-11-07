package com.axelby.podax;

import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;

class PodcastDownloader {
	private PodcastDownloader() {
	}

	public static void download(Context _context, long podcastId) {
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
					new String[]{String.valueOf(podcastId)}, null);
			if (cursor == null || !cursor.moveToNext())
				return;

			PodcastCursor podcast = new PodcastCursor(cursor);
			if (podcast.isDownloaded(_context))
				return;

			if (new File(podcast.getOldFilename(_context)).exists()) {
				if (!new File(podcast.getOldFilename(_context)).renameTo(new File(podcast.getFilename(_context))))
					PodaxLog.log(_context, "unable to move downloaded podcast to new folder");
				return;
			}

			DownloadManager.Request request = new DownloadManager.Request(Uri.parse(podcast.getMediaUrl()));
			int networks = DownloadManager.Request.NETWORK_WIFI;
			if (!PreferenceManager.getDefaultSharedPreferences(_context).getBoolean("wifiPref", true))
				networks |= DownloadManager.Request.NETWORK_MOBILE;
			request.setAllowedNetworkTypes(networks);
			request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
			request.allowScanningByMediaScanner();
			request.setTitle("Downloading " + podcast.getTitle());
			request.setDescription(podcast.getSubscriptionTitle());
			File mediaFile = new File(podcast.getFilename(_context));
			request.setDestinationInExternalFilesDir(_context, Environment.DIRECTORY_PODCASTS, mediaFile.getName());

			DownloadManager downloadManager = (DownloadManager) _context.getSystemService(Context.DOWNLOAD_SERVICE);
			long downloadId = downloadManager.enqueue(request);
			ContentValues values = new ContentValues();
			values.put(PodcastProvider.COLUMN_DOWNLOAD_ID, downloadId);
			_context.getContentResolver().update(PodcastProvider.getContentUri(podcastId), values, null, null);
		} catch (Exception e) {
			Log.e("Podax", "error while downloading", e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}
}