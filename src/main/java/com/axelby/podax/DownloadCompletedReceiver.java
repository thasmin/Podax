package com.axelby.podax;

import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;

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
			if (c != null && c.moveToFirst()) {
				int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
				if (status == DownloadManager.STATUS_SUCCESSFUL) {
					Cursor pc = context.getContentResolver().query(EpisodeProvider.URI, null, "downloadId = ?", new String[]{String.valueOf(downloadId)}, null);
					if (pc != null) {
						if (pc.moveToNext()) {
							EpisodeCursor episode = new EpisodeCursor(pc);
							episode.determineDuration(context);

							ContentValues values = new ContentValues();
							int totalSize = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
							values.put(EpisodeProvider.COLUMN_FILE_SIZE, totalSize);
							context.getContentResolver().update(EpisodeProvider.getContentUri(episode.getId()), values, null, null);
						}
						pc.close();
					}
					context.getContentResolver().notifyChange(EpisodeProvider.ACTIVE_EPISODE_URI, null);
				} else if (status == DownloadManager.STATUS_FAILED) {
					NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
					builder.setSmallIcon(R.drawable.icon);
					String title = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE));
					if (title != null)
						title = title.substring("Downloading ".length());
					else
						title = "episode";
					builder.setContentTitle("Cannot download " + title);
					builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0));
					switch (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON))) {
						case DownloadManager.ERROR_CANNOT_RESUME:
							builder.setContentText("Download cannot be resumed");
							break;
						case DownloadManager.ERROR_DEVICE_NOT_FOUND:
							builder.setContentText("External storage device not found");
							break;
						case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
							builder.setContentText("Episode already exists (email dan@axelby.com)");
							break;
						case DownloadManager.ERROR_FILE_ERROR:
							builder.setContentText("Unknown storage error");
							break;
						case DownloadManager.ERROR_HTTP_DATA_ERROR:
							builder.setContentText("HTTP transport error");
							break;
						case DownloadManager.ERROR_INSUFFICIENT_SPACE:
							builder.setContentText("Insufficient storage");
							break;
						case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
							builder.setContentText("Too many redirects");
							break;
						case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
							builder.setContentText("Unhandled HTTP code");
							break;
						case DownloadManager.ERROR_UNKNOWN:
							builder.setContentText("Unknown error");
							break;
					}
					NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
					nm.notify(Constants.NOTIFICATION_DOWNLOAD_ERROR, builder.build());
				}
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
