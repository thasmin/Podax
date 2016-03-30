package com.axelby.podax;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.axelby.podax.model.EpisodeEditor;
import com.axelby.podax.model.PodaxDB;

import org.joda.time.LocalTime;

import java.util.Date;

class PlaylistManager {

	public static void completeActiveEpisode(Context context) {
		if (isInSleepytime(context))
			PlayerService.stop(context);

		markEpisodeComplete(EpisodeCursor.getActiveEpisodeId(context));

		Stats.addCompletion(context);
	}

	public static void markEpisodeComplete(long episodeId) {
		new EpisodeEditor(episodeId)
			.setLastPosition(0)
			.setPlaylistPosition(null)
			.setFinishedDate(new Date())
			.commit();
	}

	private static boolean isInSleepytime(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		boolean sleepytimeMode = preferences.getBoolean("sleepytimeEnabled", false);
		if (sleepytimeMode) {
			int startHour = preferences.getInt("sleepytimeStart", 20);
			int endHour = preferences.getInt("sleepytimeEnd", 4) + 24;
			int currentHour = LocalTime.now().getHourOfDay();
			if (currentHour < 12)
				currentHour += 24;
			if (startHour > currentHour || currentHour > endHour)
				sleepytimeMode = false;
		}
		return sleepytimeMode;
	}

	public static void changeActiveEpisode(long activeEpisodeId) {
		PodaxDB.episodes.setActiveEpisode(activeEpisodeId);
	}

}
