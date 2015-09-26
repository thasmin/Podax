package com.axelby.podax;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;

import com.axelby.podax.PlayerStatus.PlayerStates;
import com.axelby.podax.ui.MainActivity;

// this class handles connects the app to the player
// it handles events on two sides - app and player
// app events are handled in onStartCommand and are send to the player
// player events are: started playing, stopped playing, paused, finished episode
public class PlayerService extends Service {
	private long _currentEpisodeId;
	private EpisodePlayer _player;

	private final ContentObserver _episodeChangeObserver = new ContentObserver(new Handler()) {
		@Override
		public boolean deliverSelfNotifications() {
			return false;
		}

		@Override
		public void onChange(boolean selfChange) {
			onChange(selfChange, null);
		}

		@Override
		public void onChange(boolean selfChange, Uri uri) {
			ensurePlayerStatus();
		}
	};

	private final BroadcastReceiver _pauseReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			PlayerService.pause(PlayerService.this);
		}
	};

	// static functions for easier controls
	public static void play(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAY);
	}

	public static void play(Context context, long episodeId) {
		PlaylistManager.changeActiveEpisode(context, episodeId);
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAY);
	}

	public static void pause(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PAUSE, Constants.PAUSE_MEDIABUTTON);
	}

	public static void stop(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_STOP);
	}

	public static void playpause(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAYPAUSE, Constants.PAUSE_MEDIABUTTON);
	}

	private static void sendCommand(Context context, int command) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
		context.startService(intent);
	}

	private static void sendCommand(Context context, int command, int arg) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, arg);
		context.startService(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		registerReceiver(_pauseReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(_pauseReceiver);
	}

	private class EpisodeEventHandler implements EpisodePlayer.OnCompletionListener,
			EpisodePlayer.OnPauseListener,
			EpisodePlayer.OnPlayListener,
			EpisodePlayer.OnChangeListener,
			EpisodePlayer.OnSeekListener,
			EpisodePlayer.OnStopListener {

		@Override
		public void onPlay(float positionInSeconds, float playbackRate) {
			updateActiveEpisode();

			// listen for changes to the episode
			getContentResolver().registerContentObserver(EpisodeProvider.PLAYER_UPDATE_URI, false, _episodeChangeObserver);

			MediaButtonIntentReceiver.updateState(PlaybackStateCompat.STATE_PLAYING, positionInSeconds, playbackRate);
			PlayerStatus.updateState(PlayerService.this, PlayerStates.PLAYING);
			showNotification();
		}

		@Override
		public void onPause(float positionInSeconds) {
			updateActiveEpisodePosition(positionInSeconds);

			MediaButtonIntentReceiver.updateState(PlaybackStateCompat.STATE_PAUSED, positionInSeconds, 0);
			PlayerStatus.updateState(PlayerService.this, PlayerStatus.PlayerStates.PAUSED);
			showNotification();
		}

		@Override
		public void onStop(float positionInSeconds) {
			updateActiveEpisodePosition(positionInSeconds);
			removeNotification();
			getContentResolver().unregisterContentObserver(_episodeChangeObserver);

			MediaButtonIntentReceiver.updateState(PlaybackStateCompat.STATE_STOPPED, positionInSeconds, 0);
			PlayerStatus.updateState(PlayerService.this, PlayerStatus.PlayerStates.STOPPED);
			stopSelf();
		}

		@Override
		public void onCompletion() {
			PlaylistManager.completeActiveEpisode(PlayerService.this);
		}

		@Override
		public void onChange() {
			// assume that episode playing is the active episode
			PlayerStatus status = PlayerStatus.getCurrentState(PlayerService.this);
			_currentEpisodeId = status.getEpisodeId();
			if (status.getState() == PlayerStates.PLAYING)
				_player.play();
			PlaylistManager.changeActiveEpisode(PlayerService.this, status.getEpisodeId());
			showNotification();

			if (status.getDuration() == 0) {
				EpisodeCursor ep = EpisodeCursor.getCursor(PlayerService.this, status.getEpisodeId());
				if (ep != null) {
					ep.determineDuration(PlayerService.this);
					ep.closeCursor();
				}
			}
		}

		@Override
		public void onSeek(float positionInSeconds) {
			updateActiveEpisodePosition(positionInSeconds);
		}
	}
	private final EpisodeEventHandler _episodeEventHandler = new EpisodeEventHandler();

	private void createPlayer() {
		if (_player == null) {
			_player = new EpisodePlayer(this);
			_player.setOnCompletionListener(_episodeEventHandler);
			_player.setOnPlayListener(_episodeEventHandler);
			_player.setOnPauseListener(_episodeEventHandler);
			_player.setOnStopListener(_episodeEventHandler);
			_player.setOnChangeListener(_episodeEventHandler);
			_player.setOnSeekListener(_episodeEventHandler);
			ensurePlayerStatus();
		}


	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null || intent.getExtras() == null)
			return START_NOT_STICKY;

		int extra = intent.getIntExtra(Constants.EXTRA_PLAYER_COMMAND, -1);
		if (extra == -1)
			return START_NOT_STICKY;

		int pauseReason = intent.getIntExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, -1);

		if (_player == null) {
			if (extra != Constants.PLAYER_COMMAND_PAUSE && extra != Constants.PLAYER_COMMAND_STOP)
				createPlayer();
			else {
				PlayerStatus.updateState(PlayerService.this, PlayerStatus.PlayerStates.STOPPED);
				return START_NOT_STICKY;
			}
		}

		switch (extra) {
			case Constants.PLAYER_COMMAND_PLAYPAUSE:
				_player.playPause(pauseReason);
				break;
			case Constants.PLAYER_COMMAND_PLAY:
				_player.play();
				break;
			case Constants.PLAYER_COMMAND_PLAYSTOP:
				_player.playStop();
				break;
			case Constants.PLAYER_COMMAND_PAUSE:
				_player.pause(pauseReason);
				break;
			case Constants.PLAYER_COMMAND_STOP:
				_player.stop();
				break;
			case Constants.PLAYER_COMMAND_REFRESHEPISODE:
				ensurePlayerStatus();
				break;
		}

		return START_NOT_STICKY;
	}

	private void ensurePlayerStatus() {
		if (_player == null)
			return;

		PlayerStatus status = PlayerStatus.getCurrentState(this);
		if (!status.hasActiveEpisode()) {
			_player.stop();
			return;
		}

		if (status.getEpisodeId() == _currentEpisodeId)
			_player.seekTo(status.getPosition() / 1000.0f);
		else
			_player.changeEpisode(status.getEpisodeId());
	}

	private NotificationCompat.Action makeAction(@DrawableRes int drawableId, @StringRes int stringId, Uri aedUri) {
		Intent intent = new Intent(this, ActiveEpisodeReceiver.class);
		intent.setData(aedUri);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);
		return new NotificationCompat.Action(drawableId, getResources().getString(stringId), pi);
	}

	private void showNotification() {
		PlayerStatus playerStatus = PlayerStatus.getCurrentState(this);

		Intent showIntent = new Intent(this, MainActivity.class);
		showIntent.setAction("podax://show");
		PendingIntent showPI = PendingIntent.getActivity(this, 0, showIntent, 0);

		Intent deleteIntent = new Intent(this, PlayerService.class);
		deleteIntent.setAction("podax://delete");
		deleteIntent.putExtra(Constants.EXTRA_PLAYER_COMMAND, Constants.PLAYER_COMMAND_STOP);
		PendingIntent deletePI = PendingIntent.getService(this, 0, deleteIntent, 0);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setSmallIcon(R.drawable.ic_stat_icon)
			.setContentTitle(playerStatus.getTitle())
			.setContentText(playerStatus.getSubscriptionTitle())
			.setDeleteIntent(deletePI)
			.setContentIntent(showPI);

		Bitmap subscriptionBitmap = SubscriptionCursor.getThumbnailImage(this, playerStatus.getSubscriptionId());
		if (subscriptionBitmap != null)
			builder.setLargeIcon(subscriptionBitmap);

		builder
			.addAction(makeAction(android.R.drawable.ic_media_previous, R.string.restart, Constants.ACTIVE_EPISODE_DATA_RESTART))
			.addAction(makeAction(android.R.drawable.ic_media_rew, R.string.rewind, Constants.ACTIVE_EPISODE_DATA_BACK));

		if (playerStatus.getState() == PlayerStates.PLAYING)
			builder.addAction(makeAction(android.R.drawable.ic_media_pause, R.string.pause, Constants.ACTIVE_EPISODE_DATA_PAUSE));
		else
			builder.addAction(makeAction(android.R.drawable.ic_media_play, R.string.play, Constants.ACTIVE_EPISODE_DATA_PAUSE));

		builder
			.addAction(makeAction(android.R.drawable.ic_media_ff, R.string.fast_forward, Constants.ACTIVE_EPISODE_DATA_FORWARD))
			.addAction(makeAction(android.R.drawable.ic_media_next, R.string.skip_to_end, Constants.ACTIVE_EPISODE_DATA_END))
			.setStyle(
				new NotificationCompat.MediaStyle()
					.setShowCancelButton(true)
					.setCancelButtonIntent(deletePI)
					.setShowActionsInCompactView(2,3)
					.setMediaSession(MediaButtonIntentReceiver.getSessionToken())
			);

		Notification notification = builder.build();
		if (playerStatus.getState() == PlayerStates.PLAYING)
			startForeground(Constants.NOTIFICATION_PLAYING, notification);
		else {
			NotificationManagerCompat.from(this).notify(Constants.NOTIFICATION_PLAYING, notification);
			stopForeground(false);
		}
	}

	private void removeNotification() {
		stopForeground(true);
	}

	private void updateActiveEpisode() {
		ContentValues values = new ContentValues(1);
		values.put(EpisodeProvider.COLUMN_ID, _currentEpisodeId);
		getContentResolver().update(EpisodeProvider.ACTIVE_EPISODE_URI, values, null, null);
		MediaButtonIntentReceiver.updateMetadata(this);
	}

	private void updateActiveEpisodePosition(float positionInSeconds) {
		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_LAST_POSITION, (int)(positionInSeconds * 1000));
		getContentResolver().update(EpisodeProvider.PLAYER_UPDATE_URI, values, null, null);
	}

}
