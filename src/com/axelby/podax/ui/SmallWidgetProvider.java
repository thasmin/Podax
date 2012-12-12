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

public class SmallWidgetProvider extends AppWidgetProvider {
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		if (appWidgetIds.length == 0)
			return;

		for (int widgetId : appWidgetIds) {
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.smallwidget);

			PlayerStatus playerState = PlayerStatus.getCurrentState(context);
			updatePodcastDetails(playerState, views);
	
			// set up pending intents
			setClickIntent(context, views, R.id.rewind_btn, Constants.PLAYER_COMMAND_SKIPBACK);
			setClickIntent(context, views, R.id.play_btn, Constants.PLAYER_COMMAND_PLAYSTOP);
			setClickIntent(context, views, R.id.skip_btn, Constants.PLAYER_COMMAND_SKIPFORWARD);
			setClickIntent(context, views, R.id.next_btn, Constants.PLAYER_COMMAND_SKIPTOEND);

			Intent showIntent = new Intent(context, PodcastDetailActivity.class);
			PendingIntent showPendingIntent = PendingIntent.getActivity(context, 0, showIntent, 0);
			views.setOnClickPendingIntent(R.id.show_btn, showPendingIntent);

			try {
				long subscriptionId = playerState.getSubscriptionId();
				String imageFilename = SubscriptionCursor.getThumbnailFilename(subscriptionId);
				if (new File(imageFilename).exists()) {
					Bitmap origBitmap = BitmapFactory.decodeFile(imageFilename);
					if (origBitmap != null && origBitmap.getWidth() > 0 && origBitmap.getHeight() > 0) {
						// scale bitmap down to save memory (needs to be smaller then display size)
						float density = context.getResources().getDisplayMetrics().density;
						Bitmap bitmap = Bitmap.createScaledBitmap(origBitmap, (int)density*83, (int)density*83, false);
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

	public static void setClickIntent(Context context, RemoteViews views, int resourceId, int command) {
		Intent intent = new Intent(context, PlayerService.class);
		// pendingintent will reuse intent if possible, does not look at extras so datauri makes this unique to command
		intent.setData(Uri.parse("podax://playercommand/" + command));
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
		views.setOnClickPendingIntent(resourceId, pendingIntent);
	}
}
