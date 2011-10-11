package com.axelby.podax;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class LargeWidgetProvider extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		updateWidget(context);
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	public static void updateWidget(Context context) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, "com.axelby.podax.LargeWidgetProvider"));
		if (ids.length == 0)
			return;

		boolean isPlaying = PlayerService.isPlaying();
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.largewidget);
		Podcast p = PlayerService.getActivePodcast(context);

		if (p == null) {
			views.setTextViewText(R.id.title, "Queue empty");
			views.setTextViewText(R.id.podcast, "");
			views.setTextViewText(R.id.positionstring, "");
			views.setImageViewResource(R.id.play_btn, android.R.drawable.ic_media_play);
		} else {
			views.setTextViewText(R.id.title, p.getTitle());
			views.setTextViewText(R.id.podcast, p.getSubscription().getDisplayTitle());
			views.setTextViewText(R.id.positionstring, PlayerService.getPositionString(p.getDuration(), p.getLastPosition()));
			int imageRes = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
			views.setImageViewResource(R.id.play_btn, imageRes);
			
			// set up pending intents
			setClickIntent(context, views, R.id.restart_btn, Constants.PLAYER_COMMAND_RESTART);
			setClickIntent(context, views, R.id.rewind_btn, Constants.PLAYER_COMMAND_SKIPBACK);
			setClickIntent(context, views, R.id.play_btn, Constants.PLAYER_COMMAND_PLAYPAUSE);
			setClickIntent(context, views, R.id.skip_btn, Constants.PLAYER_COMMAND_SKIPFORWARD);
			setClickIntent(context, views, R.id.next_btn, Constants.PLAYER_COMMAND_SKIPTOEND);

			Intent showIntent = new Intent(context, PodcastDetailActivity.class);
			PendingIntent showPendingIntent = PendingIntent.getActivity(context, 0, showIntent, 0);
			views.setOnClickPendingIntent(R.id.show_btn, showPendingIntent);
		}
		appWidgetManager.updateAppWidget(new ComponentName(context, "com.axelby.podax.LargeWidgetProvider"), views);
	}

	public static void setClickIntent(Context context, RemoteViews views, int resourceId, int command) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
		PendingIntent pendingIntent = PendingIntent.getService(context, command, intent, 0);
		views.setOnClickPendingIntent(resourceId, pendingIntent);
	}
}
