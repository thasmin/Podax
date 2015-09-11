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
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v7.app.NotificationCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.axelby.podax.PlayerStatus.PlayerStates;
import com.axelby.podax.ui.MainActivity;

// this class handles connects the app to the player
// it handles events on two sides - app and player
// app events are handled in onStartCommand and are send to the player
// player events are: started playing, stopped playing, paused, finished episode
public class PlayerService extends Service {
	private long _currentEpisodeId;
	private EpisodePlayer _player;

	private MediaSessionCompat _mediaSession;
	private MediaSessionCompat.Callback _mediaCallback = new MediaSessionCompat.Callback() {
		@Override public void onPlay() {
			super.onPlay();
			play(PlayerService.this);
		}

		@Override public void onPause() {
			super.onPause();
			pause(PlayerService.this);
		}

		@Override public void onStop() {
			super.onStop();
			stop(PlayerService.this);
		}


		@Override public void onSkipToNext() {
			super.onSkipToNext();
			EpisodeProvider.skipToEnd(PlayerService.this, EpisodeProvider.ACTIVE_EPISODE_URI);
		}

		@Override public void onSkipToPrevious() {
			super.onSkipToPrevious();
			EpisodeProvider.restart(PlayerService.this, EpisodeProvider.ACTIVE_EPISODE_URI);
		}

		@Override public void onFastForward() {
			super.onFastForward();
			EpisodeProvider.movePositionBy(PlayerService.this, EpisodeProvider.ACTIVE_EPISODE_URI, 30);
		}
		@Override public void onRewind() {
			super.onRewind();
			EpisodeProvider.movePositionBy(PlayerService.this, EpisodeProvider.ACTIVE_EPISODE_URI, -15);
		}

		@Override public void onSeekTo(long pos) {
			super.onSeekTo(pos);
			EpisodeProvider.movePositionTo(PlayerService.this, EpisodeProvider.ACTIVE_EPISODE_URI, (int) pos);
		}
	};

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

	private final BroadcastReceiver _stopReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
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
		registerReceiver(_stopReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(_stopReceiver);
	}

	private static final long PLAYBACK_ACTIONS = PlaybackStateCompat.ACTION_PLAY
		| PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_STOP
		| PlaybackStateCompat.ACTION_REWIND | PlaybackStateCompat.ACTION_FAST_FORWARD
		| PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
		| PlaybackStateCompat.ACTION_SEEK_TO;

	private class EpisodeEventHandler implements EpisodePlayer.OnCompletionListener,
			EpisodePlayer.OnPauseListener,
			EpisodePlayer.OnPlayListener,
			EpisodePlayer.OnChangeListener,
			EpisodePlayer.OnSeekListener,
			EpisodePlayer.OnStopListener {

		private void updateMediaState(@PlaybackStateCompat.State int state, float positionInSeconds, float playbackRate) {
			PlaybackStateCompat.Builder bob = new PlaybackStateCompat.Builder();
			bob.setState(state, (long) (positionInSeconds * 1000), playbackRate);
			bob.setActions(PLAYBACK_ACTIONS);
			PlaybackStateCompat pbState = bob.build();
			_mediaSession.setPlaybackState(pbState);
			if (state == PlaybackStateCompat.STATE_PLAYING)
				_mediaSession.setActive(true);
		}

		@Override
		public void onPlay(float positionInSeconds, float playbackRate) {
			updateActiveEpisode();

			// listen for changes to the episode
			getContentResolver().registerContentObserver(EpisodeProvider.PLAYER_UPDATE_URI, false, _episodeChangeObserver);

			updateMediaState(PlaybackStateCompat.STATE_PLAYING, positionInSeconds, playbackRate);
			PlayerStatus.updateState(PlayerService.this, PlayerStates.PLAYING);
			showNotification();
		}

		@Override
		public void onPause(float positionInSeconds) {
			updateActiveEpisodePosition(positionInSeconds);

			updateMediaState(PlaybackStateCompat.STATE_PAUSED, positionInSeconds, 0);
			PlayerStatus.updateState(PlayerService.this, PlayerStatus.PlayerStates.PAUSED);
			showNotification();
		}

		@Override
		public void onStop(float positionInSeconds) {
			updateActiveEpisodePosition(positionInSeconds);
			removeNotification();
			getContentResolver().unregisterContentObserver(_episodeChangeObserver);

			updateMediaState(PlaybackStateCompat.STATE_STOPPED, positionInSeconds, 0);
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


		if (_mediaSession == null) {
			_mediaSession = new MediaSessionCompat(this, "podax", new ComponentName(this, PlayerService.class), null);
			_mediaSession.setCallback(_mediaCallback);
			_mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
				| MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
			updateMetadata();
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
		PendingIntent showPI = PendingIntent.getActivity(this, 0, showIntent, 0);

		Intent deleteIntent = new Intent(this, PlayerService.class);
		deleteIntent.putExtra(Constants.EXTRA_PLAYER_COMMAND, Constants.PLAYER_COMMAND_STOP);
		PendingIntent deletePI = PendingIntent.getService(this, 0, deleteIntent, 0);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setSmallIcon(R.drawable.ic_stat_icon)
			.setContentTitle(playerStatus.getTitle())
			.setContentText(playerStatus.getSubscriptionTitle())
			.setDeleteIntent(deletePI)
			.setContentIntent(showPI)
			.setOngoing(true);

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
					.setShowActionsInCompactView(2)
					.setMediaSession(_mediaSession.getSessionToken())
			);

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
		updateMetadata();
	}

	private void updateActiveEpisodePosition(float positionInSeconds) {
		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_LAST_POSITION, (int)(positionInSeconds * 1000));
		getContentResolver().update(EpisodeProvider.PLAYER_UPDATE_URI, values, null, null);
	}

	protected void updateMetadata() {
		PlayerStatus status = PlayerStatus.getCurrentState(this);
		MediaMetadataCompat.Builder bob = new MediaMetadataCompat.Builder();
		bob.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, status.getTitle());
		bob.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, status.getSubscriptionTitle());
		bob.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, status.getSubscriptionTitle());
		bob.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, status.getSubscriptionTitle());
		bob.putString(MediaMetadataCompat.METADATA_KEY_TITLE, status.getTitle());
		bob.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, status.getDuration());

		Bitmap thumbnail = SubscriptionCursor.getThumbnailImage(this, status.getSubscriptionId());
		if (thumbnail != null) {
			bob.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, thumbnail);
			bob.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, thumbnail);
			bob.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, thumbnail);
		}

		_mediaSession.setMetadata(bob.build());
	}
}
