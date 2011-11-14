package com.axelby.podax;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;

public class LargeWidgetProvider extends AppWidgetProvider {
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, "com.axelby.podax.LargeWidgetProvider"));
		if (ids.length == 0)
			return;

		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.largewidget);

		updatePodcastDetails(context, views);
			
		// set up pending intents
		setClickIntent(context, views, R.id.restart_btn, Constants.PLAYER_COMMAND_RESTART);
		setClickIntent(context, views, R.id.rewind_btn, Constants.PLAYER_COMMAND_SKIPBACK);
		setClickIntent(context, views, R.id.play_btn, Constants.PLAYER_COMMAND_PLAYPAUSE);
		setClickIntent(context, views, R.id.skip_btn, Constants.PLAYER_COMMAND_SKIPFORWARD);
		setClickIntent(context, views, R.id.next_btn, Constants.PLAYER_COMMAND_SKIPTOEND);

		Intent showIntent = new Intent(context, PodcastDetailActivity.class);
		PendingIntent showPendingIntent = PendingIntent.getActivity(context, 0, showIntent, 0);
		views.setOnClickPendingIntent(R.id.show_btn, showPendingIntent);

		appWidgetManager.updateAppWidget(new ComponentName(context, "com.axelby.podax.LargeWidgetProvider"), views);

		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	public void updatePodcastDetails(Context context, RemoteViews views) {
		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_LAST_POSITION,
		};
		Uri activeUri = Uri.withAppendedPath(PodcastProvider.URI, "active");
		Cursor cursor = context.getContentResolver().query(activeUri, projection, null, null, null);
		PodcastCursor podcast = new PodcastCursor(context, cursor);

		if (podcast.isNull()) {
			views.setTextViewText(R.id.title, "Queue empty");
			views.setTextViewText(R.id.podcast, "");
			views.setTextViewText(R.id.positionstring, "");
			views.setImageViewResource(R.id.play_btn, android.R.drawable.ic_media_play);
		} else {
			try {
				views.setTextViewText(R.id.title, podcast.getTitle());
				views.setTextViewText(R.id.podcast, podcast.getSubscriptionTitle());
				String position = PlayerService.getPositionString(podcast.getDuration(), podcast.getLastPosition());
				views.setTextViewText(R.id.positionstring, position);
			} catch (MissingFieldException e) {
				e.printStackTrace();
			}
			int imageRes = PlayerService.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
			views.setImageViewResource(R.id.play_btn, imageRes);
		}
		cursor.close();
	}

	public static void setClickIntent(Context context, RemoteViews views, int resourceId, int command) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
		PendingIntent pendingIntent = PendingIntent.getService(context, command, intent, 0);
		views.setOnClickPendingIntent(resourceId, pendingIntent);
	}
}
