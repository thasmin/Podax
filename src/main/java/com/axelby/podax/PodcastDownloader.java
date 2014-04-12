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
					PodcastProvider.COLUMN_DOWNLOAD_ID,
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

			DownloadManager downloadManager = (DownloadManager) _context.getSystemService(Context.DOWNLOAD_SERVICE);

			// if download id is already set, download has been queued
			// only continue if download will not be finished itself (failed or successful)
			if (podcast.getDownloadId() != null) {
				DownloadManager.Query query = new DownloadManager.Query();
				query.setFilterById(podcast.getDownloadId());
				int status = DownloadManager.STATUS_FAILED;
				Cursor c = downloadManager.query(query);
				if (c != null) {
					if (c.moveToFirst())
						status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
					c.close();
				}
				if (status != DownloadManager.STATUS_FAILED && status != DownloadManager.STATUS_SUCCESSFUL)
					return;
				downloadManager.remove(podcast.getDownloadId());
			}

			File mediaFile = new File(podcast.getFilename(_context));
			if (mediaFile.exists())
				mediaFile.delete();

			DownloadManager.Request request = new DownloadManager.Request(Uri.parse(podcast.getMediaUrl()));
			int networks = DownloadManager.Request.NETWORK_WIFI;
			if (!PreferenceManager.getDefaultSharedPreferences(_context).getBoolean("wifiPref", true))
				networks |= DownloadManager.Request.NETWORK_MOBILE;
			request.setAllowedNetworkTypes(networks);
			/*
			request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
			request.allowScanningByMediaScanner();
			*/
			request.setTitle("Downloading " + podcast.getTitle());
			request.setDescription(podcast.getSubscriptionTitle());
			request.setDestinationInExternalFilesDir(_context, Environment.DIRECTORY_PODCASTS, mediaFile.getName());

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