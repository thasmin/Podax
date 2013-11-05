package com.axelby.podax.ui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.R;

public class SmallWidgetProvider extends AppWidgetProvider {
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		if (appWidgetIds.length == 0)
			return;

		for (int widgetId : appWidgetIds) {
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.smallwidget);

			PlayerStatus playerState = PlayerStatus.getCurrentState(context);
			updatePodcastDetails(playerState, views);

			// set up pending intents
			LargeWidgetProvider.setActivePodcastClickIntent(context, views, R.id.restart_btn, Constants.ACTIVE_PODCAST_DATA_RESTART);
			LargeWidgetProvider.setActivePodcastClickIntent(context, views, R.id.rewind_btn, Constants.ACTIVE_PODCAST_DATA_BACK);
			LargeWidgetProvider.setPlayerServiceClickIntent(context, views, R.id.play_btn, Constants.PLAYER_COMMAND_PLAYSTOP);
			LargeWidgetProvider.setActivePodcastClickIntent(context, views, R.id.skip_btn, Constants.ACTIVE_PODCAST_DATA_FORWARD);
			LargeWidgetProvider.setActivePodcastClickIntent(context, views, R.id.next_btn, Constants.ACTIVE_PODCAST_DATA_END);

			Intent showIntent = new Intent(context, MainActivity.class);
			PendingIntent showPendingIntent = PendingIntent.getActivity(context, 0, showIntent, 0);
			views.setOnClickPendingIntent(R.id.show_btn, showPendingIntent);

			Bitmap thumbnail = Helper.getCachedImage(context, playerState.getSubscriptionThumbnailUrl());
			if (thumbnail != null)
				views.setImageViewBitmap(R.id.show_btn, thumbnail);
			else
				views.setImageViewResource(R.id.show_btn, R.drawable.icon);

			appWidgetManager.updateAppWidget(widgetId, views);
		}

		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	public void updatePodcastDetails(PlayerStatus player, RemoteViews views) {
		if (player.hasActivePodcast()) {
			views.setTextViewText(R.id.title, player.getTitle());
			PodcastProgress.remoteSet(views, player.getPosition(), player.getDuration());

			int imageRes = player.isPlaying() ? R.drawable.ic_media_pause : R.drawable.ic_media_play;
			views.setImageViewResource(R.id.play_btn, imageRes);
		} else {
			views.setTextViewText(R.id.title, "Queue empty");
			PodcastProgress.remoteClear(views);
			views.setImageViewResource(R.id.play_btn, R.drawable.ic_media_play);
		}
	}
}
