package com.axelby.podax;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
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
import android.support.v4.app.NotificationCompat;

import com.axelby.podax.PlayerStatus.PlayerStates;
import com.axelby.podax.ui.MainActivity;

// this class handles connects the app to the player
// it handles events on two sides - app and player
// app events are handled in onStartCommand and are send to the player
// player events are: started playing, stopped playing, paused, finished episode
public class PlayerService extends Service {
	private long _currentEpisodeId;
	protected EpisodePlayer _player;
	private LockscreenManager _lockscreenManager = new LockscreenManager();

	private ContentObserver _episodeChangeObserver = new ContentObserver(new Handler()) {
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

	private BroadcastReceiver _stopReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			PodaxLog.log(PlayerService.this, "stopping for intent " + intent.getAction());
			PlayerService.stop(PlayerService.this);
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

	public static void pause(Context context, int pause_reason) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PAUSE, pause_reason);
	}

	public static void stop(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_STOP);
	}

	public static void playpause(Context context, int pause_reason) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAYPAUSE, pause_reason);
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
		registerReceiver(_stopReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(_stopReceiver);
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

			// grab the media button
			AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			ComponentName eventReceiver = new ComponentName(PlayerService.this, MediaButtonIntentReceiver.class);
			audioManager.registerMediaButtonEventReceiver(eventReceiver);

			PlayerStatus.updateState(PlayerService.this, PlayerStates.PLAYING);
			showNotification();
			_lockscreenManager.setLockscreenPlaying(positionInSeconds, playbackRate);
		}

		@Override
		public void onPause(float positionInSeconds) {
			updateActiveEpisodePosition(positionInSeconds);
			PlayerStatus.updateState(PlayerService.this, PlayerStatus.PlayerStates.PAUSED);
			_lockscreenManager.setLockscreenPaused(positionInSeconds);
			showNotification();
		}

		@Override
		public void onStop(float positionInSeconds) {
			updateActiveEpisodePosition(positionInSeconds);
			_lockscreenManager.removeLockscreenControls(positionInSeconds);
			removeNotification();
			getContentResolver().unregisterContentObserver(_episodeChangeObserver);
			PlayerStatus.updateState(PlayerService.this, PlayerStatus.PlayerStates.STOPPED);
			stopSelf();
		}

		@Override
		public void onCompletion() {
			PlaylistManager.moveToNextInPlaylist(PlayerService.this);
		}

		@Override
		public void onChange() {
			// assume that episode playing is the active episode
			PlayerStatus status = PlayerStatus.getCurrentState(PlayerService.this);
			_currentEpisodeId = status.getEpisodeId();
			if (status.getState() == PlayerStates.PLAYING)
				_player.play();
			PlaylistManager.changeActiveEpisode(PlayerService.this, status.getEpisodeId());
			_lockscreenManager.setupLockscreenControls(PlayerService.this, status);
			showNotification();
		}

		@Override
		public void onSeek(float positionInSeconds) {
			updateActiveEpisodePosition(positionInSeconds);
		}
	}
	private EpisodeEventHandler _episodeEventHandler = new EpisodeEventHandler();

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

		if (!intent.getExtras().containsKey(Constants.EXTRA_PLAYER_COMMAND))
			return START_NOT_STICKY;

		int pauseReason = intent.getIntExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, -1);

		if (_player == null && intent.getExtras().containsKey(Constants.EXTRA_PLAYER_COMMAND))
			createPlayer();
		switch (intent.getIntExtra(Constants.EXTRA_PLAYER_COMMAND, -1)) {
			case -1:
				break;
			case Constants.PLAYER_COMMAND_PLAYPAUSE:
				_player.playPause(pauseReason);
				break;
			case Constants.PLAYER_COMMAND_PLAY:
                ensurePlayerStatus();
				_player.play();
				break;
			case Constants.PLAYER_COMMAND_PLAYSTOP:
				_player.playStop();
				break;
			case Constants.PLAYER_COMMAND_PAUSE:
				_player.pause(pauseReason);
				break;
			case Constants.PLAYER_COMMAND_RESUME:
				_player.unpause(pauseReason);
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

		if (status.getEpisodeId() != _currentEpisodeId) {
			_player.changeEpisode(status.getFilename(), status.getPosition() / 1000.0f);
		} else
			_player.seekTo(status.getPosition() / 1000.0f);
	}

	private void showNotification() {
		PlayerStatus playerStatus = PlayerStatus.getCurrentState(this);

		// both paths use the pendingintent
		Intent showIntent = new Intent(this, MainActivity.class);
		PendingIntent showPendingIntent = PendingIntent.getActivity(this, 0, showIntent, 0);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.icon)
				.setWhen(0)
				.setContentTitle(playerStatus.getTitle())
				.setContentText(playerStatus.getSubscriptionTitle())
				.setContentIntent(showPendingIntent)
				.setOngoing(true);

		// set up pause intent
		Intent pauseIntent = new Intent(this, PlayerService.class);
		// use data to make intent unique
		pauseIntent.setData(Uri.parse("podax://playercommand/playpause"));
		pauseIntent.putExtra(Constants.EXTRA_PLAYER_COMMAND, Constants.PLAYER_COMMAND_PLAYPAUSE);
		pauseIntent.putExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, Constants.PAUSE_MEDIABUTTON);
		PendingIntent pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, 0);

		// set up forward intent
		Intent forwardIntent = new Intent(this, ActiveEpisodeReceiver.class);
		forwardIntent.setData(Constants.ACTIVE_EPISODE_DATA_FORWARD);
		PendingIntent forwardPendingIntent = PendingIntent.getService(this, 0, forwardIntent, 0);

		Bitmap subscriptionBitmap = SubscriptionCursor.getThumbnailImage(this, playerStatus.getSubscriptionId());
		if (subscriptionBitmap != null)
			builder.setLargeIcon(subscriptionBitmap);

		if (playerStatus.getState() == PlayerStates.PLAYING)
			builder.addAction(R.drawable.ic_media_pause_normal, getString(R.string.pause), pausePendingIntent);
		else
			builder.addAction(R.drawable.ic_media_play_normal, getString(R.string.play), pausePendingIntent);
		builder.addAction(R.drawable.ic_media_ff_normal, getString(R.string.fast_forward), forwardPendingIntent);

		Notification notification = builder.build();
		startForeground(Constants.NOTIFICATION_PLAYING, notification);
	}

	private void removeNotification() {
		stopForeground(true);
	}

	private void updateActiveEpisode() {
		ContentValues values = new ContentValues(1);
		values.put(EpisodeProvider.COLUMN_ID, _currentEpisodeId);
		getContentResolver().update(EpisodeProvider.ACTIVE_EPISODE_URI, values, null, null);
	}

	private void updateActiveEpisodePosition(float positionInSeconds) {
		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_LAST_POSITION, (int)(positionInSeconds * 1000));
		getContentResolver().update(EpisodeProvider.PLAYER_UPDATE_URI, values, null, null);
	}
}
