package com.axelby.podax;

import android.content.ContentValues;
import android.content.Context;
import android.preference.PreferenceManager;

public class QueueManager {

	public static void moveToNextInQueue(Context context) {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_LAST_POSITION, 0);
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("autoDeletePref", true)) {
			values.put(PodcastProvider.COLUMN_QUEUE_POSITION, (Integer) null);
		} else {
			values.put(PodcastProvider.COLUMN_QUEUE_POSITION, Integer.MAX_VALUE);
		}
		context.getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);

		Stats.addCompletion(context);
	}

	public static void changeActivePodcast(Context context, long activePodcastId) {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_ID, activePodcastId);
		context.getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);
	}

}
