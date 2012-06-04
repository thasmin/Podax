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
import com.axelby.podax.PodcastProgress;
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
			Intent playIntent = new Intent(context, PlayerService.class);
			playIntent.putExtra(Constants.EXTRA_PLAYER_COMMAND, Constants.PLAYER_COMMAND_PLAYPAUSE);
			PendingIntent playPendingIntent = PendingIntent.getService(context, 0, playIntent, 0);
			views.setOnClickPendingIntent(R.id.play_btn, playPendingIntent);
	
			Intent showIntent = new Intent(context, PodcastDetailActivity.class);
			PendingIntent showPendingIntent = PendingIntent.getActivity(context, 0, showIntent, 0);
			views.setOnClickPendingIntent(R.id.show_btn, showPendingIntent);
	
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
		PodcastCursor podcast = new PodcastCursor(cursor);

		if (podcast.isNull()) {
			views.setTextViewText(R.id.title, "Queue empty");
			views.setTextViewText(R.id.podcast, "");
			PodcastProgress.remoteClear(views);
			views.setImageViewResource(R.id.play_btn, R.drawable.ic_media_play);
		} else {
			views.setTextViewText(R.id.title, podcast.getTitle());
			views.setTextViewText(R.id.podcast, podcast.getSubscriptionTitle());
			PodcastProgress.remoteSet(views, podcast);
		}

		cursor.close();

		int imageRes = Helper.isPlaying(context) ? R.drawable.ic_media_pause : R.drawable.ic_media_play;
		views.setImageViewResource(R.id.play_btn, imageRes);
	}
}
