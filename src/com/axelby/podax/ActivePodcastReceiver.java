package com.axelby.podax;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.axelby.podax.ui.LargeWidgetProvider;
import com.axelby.podax.ui.SmallWidgetProvider;

public class ActivePodcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getData() == null)
			return;

		// only receives com.axelby.podax.activepodcast intents
		Uri activePodcastUri = PodcastProvider.ACTIVE_PODCAST_URI;
		if (intent.getData().equals(Constants.ACTIVE_PODCAST_DATA_RESTART))
			PodcastProvider.restart(context, activePodcastUri);
		else if (intent.getData().equals(Constants.ACTIVE_PODCAST_DATA_BACK))
			PodcastProvider.movePositionBy(context, activePodcastUri, -15);
		else if (intent.getData().equals(Constants.ACTIVE_PODCAST_DATA_FORWARD))
			PodcastProvider.movePositionBy(context, activePodcastUri, 30);
		else if (intent.getData().equals(Constants.ACTIVE_PODCAST_DATA_END))
			PodcastProvider.skipToEnd(context, activePodcastUri);
	}

	public static void notifyExternal(Context context) {
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
		if (widgetManager == null)
			return;

		int[] widgetIds;

		widgetIds = widgetManager.getAppWidgetIds(new ComponentName(context, LargeWidgetProvider.class));
		if (widgetIds.length > 0)
			new LargeWidgetProvider().onUpdate(context, widgetManager, widgetIds);

		widgetIds = widgetManager.getAppWidgetIds(new ComponentName(context, SmallWidgetProvider.class));
		if (widgetIds.length > 0)
			new SmallWidgetProvider().onUpdate(context, widgetManager, widgetIds);
	}
}
