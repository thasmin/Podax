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

class EpisodeDownloader {
	private EpisodeDownloader() {
	}

	public static void download(Context _context, long episodeId) {
		Cursor cursor = null;
		try {
			String[] projection = {
					EpisodeProvider.COLUMN_ID,
					EpisodeProvider.COLUMN_TITLE,
					EpisodeProvider.COLUMN_SUBSCRIPTION_TITLE,
					EpisodeProvider.COLUMN_MEDIA_URL,
					EpisodeProvider.COLUMN_FILE_SIZE,
					EpisodeProvider.COLUMN_DOWNLOAD_ID,
			};
			cursor = _context.getContentResolver().query(EpisodeProvider.PLAYLIST_URI, projection,
					"podcasts._id = ?",
					new String[]{String.valueOf(episodeId)}, null);
			if (cursor == null || !cursor.moveToNext())
				return;

			EpisodeCursor episode = new EpisodeCursor(cursor);
			if (episode.isDownloaded(_context))
				return;

			if (new File(episode.getOldFilename(_context)).exists()) {
				if (!new File(episode.getOldFilename(_context)).renameTo(new File(episode.getFilename(_context))))
					PodaxLog.log(_context, "unable to move downloaded episode to new folder");
				return;
			}

			DownloadManager downloadManager = (DownloadManager) _context.getSystemService(Context.DOWNLOAD_SERVICE);

			// if download id is already set, download has been queued
			// only continue if download will not be finished itself (failed or successful)
			if (episode.getDownloadId() != null) {
				DownloadManager.Query query = new DownloadManager.Query();
				query.setFilterById(episode.getDownloadId());
				int status = DownloadManager.STATUS_FAILED;
				Cursor c = downloadManager.query(query);
				if (c != null) {
					if (c.moveToFirst())
						status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
					c.close();
				}
				if (status != DownloadManager.STATUS_FAILED && status != DownloadManager.STATUS_SUCCESSFUL)
					return;
				downloadManager.remove(episode.getDownloadId());
			}

			File mediaFile = new File(episode.getFilename(_context));
			if (mediaFile.exists())
				mediaFile.delete();
			File indexFile = new File(episode.getIndexFilename(_context));
			if (indexFile.exists())
				indexFile.delete();

			DownloadManager.Request request = new DownloadManager.Request(Uri.parse(episode.getMediaUrl()));
			int networks = DownloadManager.Request.NETWORK_WIFI;
			if (!PreferenceManager.getDefaultSharedPreferences(_context).getBoolean("wifiPref", true))
				networks |= DownloadManager.Request.NETWORK_MOBILE;
			request.setAllowedNetworkTypes(networks);
			/*
			request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
			request.allowScanningByMediaScanner();
			*/
			request.setTitle("Downloading " + episode.getTitle());
			request.setDescription(episode.getSubscriptionTitle());
			request.setDestinationInExternalFilesDir(_context, Environment.DIRECTORY_PODCASTS, mediaFile.getName());

			long downloadId = downloadManager.enqueue(request);
			ContentValues values = new ContentValues();
			values.put(EpisodeProvider.COLUMN_DOWNLOAD_ID, downloadId);
			_context.getContentResolver().update(EpisodeProvider.getContentUri(episodeId), values, null, null);
		} catch (Exception e) {
			Log.e("Podax", "error while downloading", e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}
}