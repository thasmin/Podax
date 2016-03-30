package com.axelby.podax;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.axelby.podax.ui.SmallWidgetProvider;

public class ActiveEpisodeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getData() == null)
			return;

		// only receives com.axelby.podax.activeepisode intents
		long activeEpisodeId = EpisodeCursor.getActiveEpisodeId(context);
		if (intent.getData().equals(Constants.ACTIVE_EPISODE_DATA_RESTART))
			EpisodeProvider.restart(activeEpisodeId);
		else if (intent.getData().equals(Constants.ACTIVE_EPISODE_DATA_BACK))
			EpisodeProvider.movePositionBy(activeEpisodeId, -15);
		else if (intent.getData().equals(Constants.ACTIVE_EPISODE_DATA_FORWARD))
			EpisodeProvider.movePositionBy(activeEpisodeId, 30);
		else if (intent.getData().equals(Constants.ACTIVE_EPISODE_DATA_END))
			EpisodeProvider.skipToEnd(activeEpisodeId);
		else if (intent.getData().equals(Constants.ACTIVE_EPISODE_DATA_PAUSE))
			PlayerService.playpause(context);
	}

	public static void notifyExternal(Context context) {
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
		if (widgetManager == null)
			return;

		int[] widgetIds = widgetManager.getAppWidgetIds(new ComponentName(context, SmallWidgetProvider.class));
		if (widgetIds.length > 0)
			new SmallWidgetProvider().onUpdate(context, widgetManager, widgetIds);
	}
}
