package com.axelby.podax;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

public class QueueManager {

	public static Long findFirstDownloadedInQueue(Context context) {
		// make sure the active podcast has been downloaded
		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_FILE_SIZE,
				PodcastProvider.COLUMN_MEDIA_URL,
		};
		Uri queueUri = Uri.withAppendedPath(PodcastProvider.URI, "queue");
		Cursor c = context.getContentResolver().query(queueUri, projection, null, null, null);
		try {
			while (c.moveToNext()) {
				PodcastCursor podcast = new PodcastCursor(c);
				if (podcast.isDownloaded())
					return podcast.getId();
			}
			return null;
		} finally {
			c.close();
		}
	}

	public static Long moveToNextInQueue(Context context) {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_LAST_POSITION, 0);
		values.put(PodcastProvider.COLUMN_QUEUE_POSITION, (Integer) null);
		context.getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);

		SharedPreferences prefs = context.getSharedPreferences("internals", Context.MODE_PRIVATE);
		prefs.edit().remove(PodcastProvider.PREF_ACTIVE).commit();

		Long activePodcastId = findFirstDownloadedInQueue(context);
		if (activePodcastId == null)
			return null;
		changeActivePodcast(context, activePodcastId);
		return activePodcastId;
	}

	public static void changeActivePodcast(Context context, Long activePodcastId) {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_ID, activePodcastId);
		context.getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);

		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_LAST_POSITION,
		};
		Cursor c = context.getContentResolver().query(PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
		try {
			if (c.moveToNext()) {
				PodcastCursor podcast = new PodcastCursor(c);
				// if the podcast has ended and it's back in the queue, restart it
				boolean needToRestart = podcast.getDuration() > 0 && podcast.getLastPosition() > podcast.getDuration() - 1000;
				if (needToRestart)
					podcast.setLastPosition(context, 0);
				PlayerStatus.updatePodcast(podcast.getTitle(), 
						podcast.getSubscriptionTitle(), 
						needToRestart ? 0 : podcast.getLastPosition(), 
						podcast.getDuration());
			} else
				PlayerStatus.updateQueueEmpty();
		} finally {
			c.close();
		}
	}

}
