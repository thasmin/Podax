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
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.axelby.podax.PlayerStatus.PlayerStates;
import com.axelby.podax.ui.MainActivity;

import java.io.IOException;
import java.util.Locale;

public class PlayerService extends Service {

	private final OnAudioFocusChangeListener _afChangeListener = new OnAudioFocusChangeListener() {
		public void onAudioFocusChange(int focusChange) {
			if (_player == null)
				return;

			if (focusChange == AudioManager.AUDIOFOCUS_LOSS)
				stop();

			if (focusChange == AudioManager.AUDIOFOCUS_GAIN && PlayerStatus.getCurrentState(PlayerService.this).isPaused())
				PlayerService.resume(PlayerService.this, Constants.PAUSE_AUDIOFOCUS);
			else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
				PlayerService.pause(PlayerService.this, Constants.PAUSE_AUDIOFOCUS);
		}
	};
	protected MediaPlayer _player;
	protected boolean _onPhone;
	Thread _updateThread;
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
			if (_player == null)
				return;

			String[] projection = new String[]{
					PodcastProvider.COLUMN_ID,
					PodcastProvider.COLUMN_MEDIA_URL,
					PodcastProvider.COLUMN_LAST_POSITION,
					PodcastProvider.COLUMN_TITLE,
					PodcastProvider.COLUMN_DURATION,
					PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
					PodcastProvider.COLUMN_SUBSCRIPTION_THUMBNAIL,
			};
			Cursor c = getContentResolver().query(PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
			if (c == null)
				return;
			if (!c.moveToFirst()) {
				c.close();
				return;
			}
			long newPodcastId = c.getLong(0);
			int newPosition = c.getInt(2);
			if (newPodcastId != _currentPodcastId) {
				PodcastCursor p = new PodcastCursor(c);
				prepareMediaPlayer(p);
				QueueManager.changeActivePodcast(PlayerService.this, newPodcastId);
				_lockscreenManager.setupLockscreenControls(PlayerService.this, p);
				showNotification();
			} else if (Math.abs(newPosition - _player.getCurrentPosition()) < 5) {
				// TODO: find what is triggering this and stop it
				PodaxLog.log(PlayerService.this, "inappropriate position change");
			} else {
				_player.seekTo(newPosition);
			}
			c.close();
		}
	};
	private BroadcastReceiver _stopReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			PodaxLog.log(PlayerService.this, "stopping for intent " + intent.getAction());
			PlayerService.stop(PlayerService.this);
		}
	};
	private boolean _pausingFor[] = new boolean[]{false, false};
	private LockscreenManager _lockscreenManager = null;
	private long _currentPodcastId;

	// static functions for easier controls
	public static void play(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAY);
	}

	public static void pause(Context context, int pause_reason) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PAUSE, pause_reason);
	}

	public static void resume(Context context, int pause_reason) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_RESUME, pause_reason);
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

	private void createUpdateTimer() {
		if (_updateThread != null)
			stopUpdateTimer();
		_updateThread = new Thread(new UpdatePositionTimerTask());
		_updateThread.start();
	}

	private void stopUpdateTimer() {
		if (_updateThread == null)
			return;
		_updateThread.interrupt();
	}

	private void createPlayer() {
		if (_player == null) {
			_player = new MediaPlayer();
			_player.setAudioStreamType(AudioManager.STREAM_MUSIC);

			// handle errors so the onCompletionListener doens't get called
			_player.setOnErrorListener(new OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer player, int what, int extra) {
					String message = String.format(Locale.US, "mediaplayer error - what: %d, extra: %d", what, extra);
					PodaxLog.log(PlayerService.this, message);

					stopUpdateTimer();
					player.reset();
					return true;
				}
			});

			_player.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer player) {
					playNextPodcast();
				}
			});

			_player.setOnPreparedListener(new OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer player) {
					if (player.getDuration() > 0) {
						ContentValues values = new ContentValues();
						values.put(PodcastProvider.COLUMN_DURATION, player.getDuration());
						PlayerService.this.getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);
					}

					player.start();
					createUpdateTimer();
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

		switch (intent.getIntExtra(Constants.EXTRA_PLAYER_COMMAND, -1)) {
			case -1:
				break;
			case Constants.PLAYER_COMMAND_PLAYPAUSE:
				if (_player != null && _player.isPlaying())
					pauseForReason(pauseReason);
				else
					resume();
				break;
			case Constants.PLAYER_COMMAND_PLAYSTOP:
				if (_player != null && _player.isPlaying())
					stop();
				else
					resume();
				break;
			case Constants.PLAYER_COMMAND_PLAY:
				resume();
				break;
			case Constants.PLAYER_COMMAND_PAUSE:
				pauseForReason(pauseReason);
				break;
			case Constants.PLAYER_COMMAND_RESUME:
				if (unpauseForReason(pauseReason))
					resume();
				break;
			case Constants.PLAYER_COMMAND_STOP:
				stop();
				break;
		}

		return START_NOT_STICKY;
	}

	public boolean isPausedForAnyReason() {
		// make sure all of our pause reasons are OK
		for (int i = 0; i < Constants.PAUSE_COUNT; ++i)
			if (_pausingFor[i])
				return false;
		return true;
	}

	private boolean unpauseForReason(int reason) {
		if (reason != -1)
			_pausingFor[reason] = false;
		return isPausedForAnyReason();
	}

	private void pauseForReason(int reason) {
		if (reason == -1)
			return;

		_pausingFor[reason] = true;
		if (_player != null && _player.isPlaying()) {
			_player.pause();
			updateActivePodcastPosition(_player.getCurrentPosition());
			PlayerStatus.updateState(this, PlayerStates.PAUSED);
			_lockscreenManager.setLockscreenPaused();
			showNotification();
		}
	}

	private void stop() {
		PlayerStatus.updateState(this, PlayerStates.STOPPED);

		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		am.abandonAudioFocus(_afChangeListener);

		stopUpdateTimer();

		removeNotification();

		if (_lockscreenManager != null)
			_lockscreenManager.removeLockscreenControls();

		if (_player != null && _player.isPlaying()) {
			_player.pause();
			updateActivePodcastPosition(_player.getCurrentPosition());
			_player.stop();
			_player.release();
		}

		_player = null;

		// tell anything listening to the active podcast to refresh now that we're stopped
		ContentValues values = new ContentValues();
		getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);
		getContentResolver().unregisterContentObserver(_podcastChangeObserver);

		stopSelf();
	}

	private boolean grabAudioFocus() {
		if (_onPhone)
			return false;

		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int result = audioManager.requestAudioFocus(_afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
			stop();
			return false;
		}

		// grab the media button when we have audio focus
		audioManager.registerMediaButtonEventReceiver(new ComponentName(this, MediaButtonIntentReceiver.class));

		return true;
	}

	private void resume() {
		if (!grabAudioFocus())
			return;

		// make sure we don't pause for media button when audio focus event happens
		for (int i = 0; i < Constants.PAUSE_COUNT; ++i)
			_pausingFor[i] = false;

		// find out where to start playing
		String[] projection = new String[]{
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_ID,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_THUMBNAIL,
				PodcastProvider.COLUMN_MEDIA_URL,
				PodcastProvider.COLUMN_LAST_POSITION,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_FILE_SIZE,
		};
		Cursor c = getContentResolver().query(PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
		if (c == null)
			return;
		if (c.isAfterLast()) {
			c.close();
			return;
		}
		PodcastCursor p = new PodcastCursor(c);

		// make sure the podcast is downloaded
		if (!p.isDownloaded(this)) {
			Toast.makeText(this, R.string.podcast_not_downloaded, Toast.LENGTH_SHORT).show();
			return;
		}

		// create and prepare the media player
		createPlayer();
		if (!prepareMediaPlayer(p)) {
			Toast.makeText(this, getResources().getString(R.string.cannot_play_podcast, p.getTitle()), Toast.LENGTH_LONG).show();
			return;
		}

		// set this podcast as active -- it may not have been first in queue
		QueueManager.changeActivePodcast(this, p.getId());
		getContentResolver().registerContentObserver(PodcastProvider.PLAYER_UPDATE_URI, false, _podcastChangeObserver);
		PlayerStatus.updateState(this, PlayerStates.PLAYING);

		_lockscreenManager = new LockscreenManager();
		_lockscreenManager.setupLockscreenControls(this, p);

		c.close();

		showNotification();
	}

	// needs id, media_url, and last_position
	private boolean prepareMediaPlayer(PodcastCursor p) {
		try {
			_currentPodcastId = p.getId();
			_player.reset();
			_player.setDataSource(p.getFilename(this));
			_player.prepare();
			_player.seekTo(p.getLastPosition());
			return true;
		} catch (IllegalStateException e) {
			// called if player is not in idle state
			return false;
		} catch (IllegalArgumentException e) {
			return false;
		} catch (SecurityException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}

	private void playNextPodcast() {
		if (_player != null) {
			// stop the player and the updating while we do some administrative stuff
			_player.pause();
			stopUpdateTimer();
			updateActivePodcastPosition(_player.getCurrentPosition());
		}

		QueueManager.moveToNextInQueue(this);

		if (!PlayerStatus.getCurrentState(this).hasActivePodcast()) {
			stop();
			return;
		}

		resume();
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

	private void updateActivePodcastPosition(int position) {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_LAST_POSITION, position);
		getContentResolver().update(PodcastProvider.PLAYER_UPDATE_URI, values, null, null);
	}

	private class UpdatePositionTimerTask implements Runnable {
		public void run() {
			int _lastPosition = 0;
			while (true) {
				if (_player == null || !_player.isPlaying())
					return;

				// we're interrupted when it's time to stop
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					return;
				}

				int currentPosition = _player.getCurrentPosition();
				if (_lastPosition / 1000 != currentPosition / 1000) {
					ContentValues values = new ContentValues();
					values.put(PodcastProvider.COLUMN_LAST_POSITION, currentPosition);
					getContentResolver().update(PodcastProvider.PLAYER_UPDATE_URI, values, null, null);
				}
				_lastPosition = currentPosition;
			}
		}
	}
}
