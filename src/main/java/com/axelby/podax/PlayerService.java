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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.axelby.podax.PlayerStatus.PlayerStates;
import com.axelby.podax.ui.MainActivity;

public class PlayerService extends Service {
	protected PodcastPlayer _player;

	private ContentObserver _podcastChangeObserver = new ContentObserver(new Handler()) {
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
			refreshPodcast();
		}
	};

	private void refreshPodcast() {
		if (_player == null)
			return;

		PlayerStatus status = PlayerStatus.getCurrentState(this);
		if (!status.hasActivePodcast()) {
			_player.stop();
			return;
		}

		if (status.getPodcastId() != _currentPodcastId) {
			_currentPodcastId = status.getPodcastId();
			if (!_player.changePodcast(status.getFilename(), status.getPosition() / 1000.0f)) {
				_player.stop();
				String toastMessage = getResources().getString(R.string.cannot_play_podcast, status.getTitle());
				Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
			} else {
				QueueManager.changeActivePodcast(this, status.getPodcastId());
				_lockscreenManager.setupLockscreenControls(this, status);
				showNotification();
			}
		} else {
			_player.seekTo(status.getPosition() / 1000.0f);
		}
	}

	private BroadcastReceiver _stopReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			PodaxLog.log(PlayerService.this, "stopping for intent " + intent.getAction());
			PlayerService.stop(PlayerService.this);
		}
	};
	private LockscreenManager _lockscreenManager = null;
	private long _currentPodcastId;

	// static functions for easier controls
	public static void play(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAY);
	}

	public static void play(Context context, long podcastId) {
		QueueManager.changeActivePodcast(context, podcastId);
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

	public static void playstop(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAYSTOP);
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

	private void createPlayer() {
		if (_player == null) {
			_player = new PodcastPlayer(this);
			prepareNextPodcast();

			// handle errors so the onCompletionListener doens't get called
			/*
			_player.setOnErrorListener(new OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer player, int what, int extra) {
					String message = String.format(Locale.US, "mediaplayer error - what: %d, extra: %d", what, extra);
					PodaxLog.log(PlayerService.this, message);
					return true;
				}
			});
			*/

			_player.setOnCompletionListener(new PodcastPlayer.OnCompletionListener() {
				@Override
				public void onCompletion() {
					QueueManager.moveToNextInQueue(PlayerService.this);
					prepareNextPodcast();
				}
			});

			_player.setOnPlayListener(new PodcastPlayer.OnPlayListener() {
				@Override
				public void onPlay(float durationInSeconds) {
					// set this podcast as active
					ContentValues values = new ContentValues(1);
					if (durationInSeconds > 0 && durationInSeconds < 60 * 60 * 6)
						values.put(PodcastProvider.COLUMN_DURATION, (int)(durationInSeconds * 1000));
					values.put(PodcastProvider.COLUMN_ID, _currentPodcastId);
					getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);

					// listen for changes to the podcast
					getContentResolver().registerContentObserver(PodcastProvider.PLAYER_UPDATE_URI, false, _podcastChangeObserver);

					// grab the media button
					AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
					ComponentName eventReceiver = new ComponentName(PlayerService.this, MediaButtonIntentReceiver.class);
					audioManager.registerMediaButtonEventReceiver(eventReceiver);

					PlayerStatus.updateState(PlayerService.this, PlayerStates.PLAYING);
					_lockscreenManager.setLockscreenPlaying();
				}
			});

			_player.setOnPauseListener(new PodcastPlayer.OnPauseListener() {
				@Override
				public void onPause(float positionInSeconds) {
					updateActivePodcastPosition(positionInSeconds);
					PlayerStatus.updateState(PlayerService.this, PlayerStatus.PlayerStates.PAUSED);
					_lockscreenManager.setLockscreenPaused();
					showNotification();
				}
			});

			_player.setOnStopListener(new PodcastPlayer.OnStopListener() {
				@Override
				public void onStop(float positionInSeconds) {
					updateActivePodcastPosition(positionInSeconds);

					removeNotification();

					if (_lockscreenManager != null)
						_lockscreenManager.removeLockscreenControls();

					getContentResolver().unregisterContentObserver(_podcastChangeObserver);

					PlayerStatus.updateState(PlayerService.this, PlayerStatus.PlayerStates.STOPPED);

					stopSelf();
				}
			});

			_player.setOnSeekListener(new PodcastPlayer.OnSeekListener() {
				@Override
				public void onSeek(float positionInSeconds) {
					updateActivePodcastPosition(positionInSeconds);
				}
			});
		}

		if (_lockscreenManager == null)
			_lockscreenManager = new LockscreenManager();
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
			case Constants.PLAYER_COMMAND_PLAYSTOP:
				_player.playStop();
				break;
			case Constants.PLAYER_COMMAND_PLAY:
				_player.play();
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
			case Constants.PLAYER_COMMAND_REFRESHPODCAST:
				refreshPodcast();
				break;
		}

		return START_NOT_STICKY;
	}

	private void prepareNextPodcast() {
		PlayerStatus currentState = PlayerStatus.getCurrentState(this);
		if (!currentState.hasActivePodcast()) {
			_player.stop();
		} else {
			_player.changePodcast(currentState.getFilename(), currentState.getPosition() / 1000.0f);
			_currentPodcastId = currentState.getPodcastId();
		}
	}

	private void showNotification() {
		String[] projection = new String[]{
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_ID,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_THUMBNAIL,
		};
		Cursor c = getContentResolver().query(PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
		if (c == null)
			return;
		if (c.isAfterLast()) {
			c.close();
			return;
		}
		PodcastCursor podcast = new PodcastCursor(c);

		// both paths use the pendingintent
		Intent showIntent = new Intent(this, MainActivity.class);
		PendingIntent showPendingIntent = PendingIntent.getActivity(this, 0, showIntent, 0);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.icon)
				.setWhen(0)
				.setContentTitle(podcast.getTitle())
				.setContentText(podcast.getSubscriptionTitle())
				.setContentIntent(showPendingIntent)
				.setOngoing(true)
				.setPriority(NotificationCompat.PRIORITY_DEFAULT);

		// set up pause intent
		Intent pauseIntent = new Intent(this, PlayerService.class);
		// use data to make intent unique
		pauseIntent.setData(Uri.parse("podax://playercommand/playpause"));
		pauseIntent.putExtra(Constants.EXTRA_PLAYER_COMMAND, Constants.PLAYER_COMMAND_PLAYPAUSE);
		pauseIntent.putExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, Constants.PAUSE_MEDIABUTTON);
		PendingIntent pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, 0);

		// set up forward intent
		Intent forwardIntent = new Intent(this, ActivePodcastReceiver.class);
		forwardIntent.setData(Constants.ACTIVE_PODCAST_DATA_FORWARD);
		PendingIntent forwardPendingIntent = PendingIntent.getService(this, 0, forwardIntent, 0);

		Bitmap subscriptionBitmap = Helper.getCachedImage(this, podcast.getSubscriptionThumbnailUrl(), 128, 128);
		if (subscriptionBitmap != null)
			builder.setLargeIcon(subscriptionBitmap);

		if (PlayerStatus.getPlayerState(this) == PlayerStates.PLAYING)
			builder.addAction(R.drawable.ic_media_pause_normal, getString(R.string.pause), pausePendingIntent);
		else
			builder.addAction(R.drawable.ic_media_play_normal, getString(R.string.play), pausePendingIntent);
		builder.addAction(R.drawable.ic_media_ff_normal, getString(R.string.fast_forward), forwardPendingIntent);

		Notification notification = builder.build();
		startForeground(Constants.NOTIFICATION_PLAYING, notification);

		c.close();
	}

	private void removeNotification() {
		stopForeground(true);
	}

	private void updateActivePodcastPosition(float positionInSeconds) {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_LAST_POSITION, (int)(positionInSeconds * 1000));
		getContentResolver().update(PodcastProvider.PLAYER_UPDATE_URI, values, null, null);
	}
}
