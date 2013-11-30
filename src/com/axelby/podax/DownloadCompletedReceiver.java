package com.axelby.podax;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.axelby.podax.ui.MainActivity;

import java.io.File;

public class DownloadCompletedReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
			DownloadManager.Query query = new DownloadManager.Query();
			long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
			query.setFilterById(downloadId);
			DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
			Cursor c = downloadManager.query(query);
			if (c != null && c.moveToFirst() && c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
				Cursor pc = context.getContentResolver().query(PodcastProvider.URI, null, "downloadId = ?", new String[]{String.valueOf(downloadId)}, null);
				if (pc != null) {
					if (pc.moveToNext()) {
						PodcastCursor podcast = new PodcastCursor(pc);
						podcast.determineDuration(context);

						ContentValues values = new ContentValues();
						int totalSize = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
						values.put(PodcastProvider.COLUMN_FILE_SIZE, totalSize);
						context.getContentResolver().update(PodcastProvider.getContentUri(podcast.getId()), values, null, null);
					}
					pc.close();
				}
				context.getContentResolver().notifyChange(PodcastProvider.ACTIVE_PODCAST_URI, null);
			}
			if (c != null)
				c.close();
		} else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
			Intent mainActivity = new Intent(context, MainActivity.class);
			mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mainActivity.putExtra(Constants.EXTRA_FRAGMENT, 3);
			context.startActivity(mainActivity);
		}
	}
}
