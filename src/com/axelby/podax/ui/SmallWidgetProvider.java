package com.axelby.podax.ui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;

import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PodcastCursor;
import com.axelby.podax.PodcastProvider;
import com.axelby.podax.R;

public class SmallWidgetProvider extends AppWidgetProvider {
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		if (appWidgetIds.length == 0)
			return;

		for (int widgetId : appWidgetIds) {
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.smallwidget);
	
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

			Intent queueIntent = new Intent(context, MainActivity.class);
			queueIntent.putExtra(Constants.EXTRA_TAB, MainActivity.TAB_QUEUE);
			PendingIntent queuePendingIntent = PendingIntent.getActivity(context, 0, queueIntent, 0);
			views.setOnClickPendingIntent(R.id.queue_btn, queuePendingIntent);
	
			appWidgetManager.updateAppWidget(widgetId, views);
		}

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
		if (cursor.isAfterLast()) {
			views.setTextViewText(R.id.title, "Queue empty");
			PodcastProgress.remoteClear(views);
			views.setImageViewResource(R.id.play_btn, R.drawable.ic_media_play);
		} else {
			PodcastCursor podcast = new PodcastCursor(cursor);
			views.setTextViewText(R.id.title, podcast.getTitle());
			PodcastProgress.remoteSet(views, podcast);

			int imageRes = Helper.isPlaying(context) ? R.drawable.ic_media_pause : R.drawable.ic_media_play;
			views.setImageViewResource(R.id.play_btn, imageRes);
		}
		cursor.close();
	}

	public static void setClickIntent(Context context, RemoteViews views, int resourceId, int command) {
		Intent intent = new Intent(context, PlayerService.class);
		// pendingintent will reuse intent if possible, does not look at extras so datauri makes this unique to command
		intent.setData(Uri.parse("podax://playercommand/" + command));
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
		views.setOnClickPendingIntent(resourceId, pendingIntent);
	}
}
