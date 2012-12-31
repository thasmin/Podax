package com.axelby.podax.ui;

import java.io.File;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.axelby.podax.Constants;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;

public class LargeWidgetProvider extends AppWidgetProvider {
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		if (appWidgetIds.length == 0)
			return;

		for (int widgetId : appWidgetIds) {
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.largewidget);

			PlayerStatus playerState = PlayerStatus.getCurrentState(context);

			updatePodcastDetails(playerState, views);
	
			// set up pending intents
			setClickIntent(context, views, R.id.restart_btn, Constants.PLAYER_COMMAND_RESTART);
			setClickIntent(context, views, R.id.rewind_btn, Constants.PLAYER_COMMAND_SKIPBACK);
			setClickIntent(context, views, R.id.play_btn, Constants.PLAYER_COMMAND_PLAYSTOP);
			setClickIntent(context, views, R.id.skip_btn, Constants.PLAYER_COMMAND_SKIPFORWARD);
			setClickIntent(context, views, R.id.next_btn, Constants.PLAYER_COMMAND_SKIPTOEND);

			try {
				long subscriptionId = playerState.getSubscriptionId();
				String imageFilename = SubscriptionCursor.getThumbnailFilename(subscriptionId);
				if (new File(imageFilename).exists()) {
					Bitmap origBitmap = BitmapFactory.decodeFile(imageFilename);
					if (origBitmap != null) {
						// scale bitmap down to save memory (needs to be smaller then display size)
						float density = context.getResources().getDisplayMetrics().density;
						Bitmap bitmap = Bitmap.createScaledBitmap(origBitmap, (int)density*92, (int)density*92, false);
						views.setImageViewBitmap(R.id.show_btn, bitmap);
					}
				} else {
					Log.d("Podax", "file doesn't exist: " + imageFilename);
					views.setImageViewResource(R.id.show_btn, R.drawable.icon);
				}
			} catch (OutOfMemoryError e) {
				Log.d("Podax", "out of memory error");
				views.setImageViewResource(R.id.show_btn, R.drawable.icon);
			}

			if (playerState.hasActivePodcast()) {
				Intent showIntent = new Intent(context, PodcastDetailActivity.class);
				PendingIntent showPendingIntent = PendingIntent.getActivity(context, 0, showIntent, 0);
				views.setOnClickPendingIntent(R.id.show_btn, showPendingIntent);
			} else {
				Intent showIntent = new Intent(context, MainActivity.class);
				PendingIntent showPendingIntent = PendingIntent.getActivity(context, 0, showIntent, 0);
				views.setOnClickPendingIntent(R.id.show_btn, showPendingIntent);
			}

			Intent queueIntent = new Intent(context, MainActivity.class);
			queueIntent.putExtra(Constants.EXTRA_TAB, MainActivity.TAB_QUEUE);
			PendingIntent queuePendingIntent = PendingIntent.getActivity(context, 0, queueIntent, 0);
			views.setOnClickPendingIntent(R.id.queue_btn, queuePendingIntent);
	
			appWidgetManager.updateAppWidget(widgetId, views);
		}

		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	public void updatePodcastDetails(PlayerStatus player, RemoteViews views) {
		if (player.hasActivePodcast()) {
			views.setTextViewText(R.id.title, player.getTitle());
			views.setTextViewText(R.id.podcast, player.getSubscriptionTitle());
			PodcastProgress.remoteSet(views, player.getPosition(), player.getDuration());

			int imageRes = player.isPlaying() ? R.drawable.ic_media_pause : R.drawable.ic_media_play;
			views.setImageViewResource(R.id.play_btn, imageRes);
		} else {
			views.setTextViewText(R.id.title, "Queue empty");
			views.setTextViewText(R.id.podcast, "");
			PodcastProgress.remoteClear(views);
			views.setImageViewResource(R.id.play_btn, R.drawable.ic_media_play);
		}
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
