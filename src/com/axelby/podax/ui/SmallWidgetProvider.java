package com.axelby.podax.ui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.widget.RemoteViews;

import com.androidquery.AQuery;
import com.axelby.podax.Constants;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.PodcastProvider;
import com.axelby.podax.R;

public class SmallWidgetProvider extends AppWidgetProvider {
	Context _context = null;

	private ContentObserver _activeObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			AppWidgetManager widgetManager = AppWidgetManager.getInstance(_context);
			int[] widgetIds = widgetManager.getAppWidgetIds(new ComponentName(_context, SmallWidgetProvider.class));
			onUpdate(_context, widgetManager, widgetIds);
		}

		@Override
		public void onChange(boolean selfChange) {
			onChange(selfChange, null);
		}
	};

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		_context = context;
		context.getContentResolver().registerContentObserver(PodcastProvider.ACTIVE_PODCAST_URI, false, _activeObserver);
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		context.getContentResolver().unregisterContentObserver(_activeObserver);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// set up the content observer if it isn't set up already
		if (_context == null) {
			_context = context;
			context.getContentResolver().registerContentObserver(PodcastProvider.ACTIVE_PODCAST_URI, false, _activeObserver);
		}

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

			if (playerState.hasActivePodcast()) {
				Intent showIntent = new Intent(context, PodcastDetailActivity.class);
				PendingIntent showPendingIntent = PendingIntent.getActivity(context, 0, showIntent, 0);
				views.setOnClickPendingIntent(R.id.show_btn, showPendingIntent);
			} else {
				Intent showIntent = new Intent(context, MainActivity.class);
				PendingIntent showPendingIntent = PendingIntent.getActivity(context, 0, showIntent, 0);
				views.setOnClickPendingIntent(R.id.show_btn, showPendingIntent);
			}

			Bitmap bitmap = new AQuery(context).getCachedImage(playerState.getSubscriptionThumbnailUrl(), 83);
			if (bitmap != null)
				views.setImageViewBitmap(R.id.show_btn, bitmap);
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

	public static void setClickIntent(Context context, RemoteViews views, int resourceId, int command) {
		Intent intent = new Intent(context, PlayerService.class);
		// pendingintent will reuse intent if possible, does not look at extras so datauri makes this unique to command
		intent.setData(Uri.parse("podax://playercommand/" + command));
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
		views.setOnClickPendingIntent(resourceId, pendingIntent);
	}
}
