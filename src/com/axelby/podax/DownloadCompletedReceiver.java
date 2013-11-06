package com.axelby.podax;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.axelby.podax.ui.MainActivity;

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
				if (pc != null && pc.moveToNext())
					new PodcastCursor(pc).determineDuration(context);
				context.getContentResolver().notifyChange(PodcastProvider.ACTIVE_PODCAST_URI, null);
			}
		} else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
			long[] downloadIds = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);
			// find the first one from podax and start the activity for that one
			for (long downloadId : downloadIds) {
				String[] projection = {PodcastProvider.COLUMN_ID};
				String[] selectionArgs = {String.valueOf(downloadId)};
				Cursor pc = context.getContentResolver().query(PodcastProvider.URI, projection, "downloadId = ?", selectionArgs, null);
				if (pc != null && pc.moveToNext()) {
					Intent mainActivity = new Intent(context, MainActivity.class);
					mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					mainActivity.putExtra("fragmentId", 2);
					mainActivity.putExtra(Constants.EXTRA_PODCAST_ID, pc.getLong(0));
					context.startActivity(mainActivity);
					break;
				}
			}
		}
	}
}
