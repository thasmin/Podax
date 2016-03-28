package com.axelby.podax.ui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.axelby.podax.ActiveEpisodeReceiver;
import com.axelby.podax.Constants;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.R;
import com.axelby.podax.model.SubscriptionData;

public class SmallWidgetProvider extends AppWidgetProvider {
    private static void setActivePodcastClickIntent(Context context, RemoteViews views, int resourceId, Uri command) {
        Intent intent = new Intent(context, ActiveEpisodeReceiver.class);
        intent.setData(command);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        views.setOnClickPendingIntent(resourceId, pendingIntent);
    }

    private static void setPlayerServiceClickIntent(Context context, RemoteViews views, int resourceId, int command) {
        Intent intent = new Intent(context, PlayerService.class);
        // pendingintent will reuse intent if possible, does not look at extras so datauri makes this unique to command
        intent.setData(Uri.parse("podax://playercommand/" + command));
        intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(resourceId, pendingIntent);
    }

    @Override
	public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		if (appWidgetIds.length == 0)
			return;

		final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.smallwidget);

		PlayerStatus playerState = PlayerStatus.getCurrentState(context);
		updatePodcastDetails(playerState, views);

		// set up pending intents
		setActivePodcastClickIntent(context, views, R.id.restart_btn, Constants.ACTIVE_EPISODE_DATA_RESTART);
		setActivePodcastClickIntent(context, views, R.id.rewind_btn, Constants.ACTIVE_EPISODE_DATA_BACK);
		setPlayerServiceClickIntent(context, views, R.id.play_btn, Constants.PLAYER_COMMAND_PLAYSTOP);
		setActivePodcastClickIntent(context, views, R.id.skip_btn, Constants.ACTIVE_EPISODE_DATA_FORWARD);
		setActivePodcastClickIntent(context, views, R.id.next_btn, Constants.ACTIVE_EPISODE_DATA_END);

		Intent showIntent = new Intent(context, MainActivity.class);
		PendingIntent showPendingIntent = PendingIntent.getActivity(context, 0, showIntent, 0);
		views.setOnClickPendingIntent(R.id.show_btn, showPendingIntent);

		SubscriptionData.getThumbnailImage(context, playerState.getSubscriptionId()).into(views, R.id.show_btn, appWidgetIds);

		appWidgetManager.updateAppWidget(appWidgetIds, views);

		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	void updatePodcastDetails(PlayerStatus player, RemoteViews views) {
		if (player.hasActiveEpisode()) {
			views.setTextViewText(R.id.title, player.getTitle());
			EpisodeProgress.remoteSet(views, player.getPosition(), player.getDuration());

			int imageRes = player.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
			views.setImageViewResource(R.id.play_btn, imageRes);
		} else {
			views.setTextViewText(R.id.title, "Playlist empty");
			EpisodeProgress.remoteClear(views);
			views.setImageViewResource(R.id.play_btn, android.R.drawable.ic_media_play);
		}
	}
}
