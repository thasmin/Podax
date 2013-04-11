package com.axelby.podax;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.axelby.podax.PlayerStatus.PlayerStates;
import com.axelby.podax.ui.PodcastDetailActivity;

public class PlayerService extends Service {

	private class UpdatePositionTimerTask extends TimerTask {
		protected int _lastPosition = 0;
		public void run() {
			if (_player == null || !_player.isPlaying())
				return;

			int currentPosition = _player.getCurrentPosition();
			if (_lastPosition / 1000 != currentPosition / 1000) {
				updateActivePodcastPosition(currentPosition);
			}
			_lastPosition = currentPosition;
		}
	};

	private final OnAudioFocusChangeListener _afChangeListener = new OnAudioFocusChangeListener() {
		public void onAudioFocusChange(int focusChange) {
			PodaxLog.log(PlayerService.this, "audio focus change event");

			if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT ||
					focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
					PodaxLog.log(PlayerService.this, "got a transient audio focus gain event somehow");

			if (focusChange == AudioManager.AUDIOFOCUS_LOSS)
				PlayerService.stop(PlayerService.this);

			if (PlayerStatus.getCurrentState(PlayerService.this).isPaused() &&
					focusChange == AudioManager.AUDIOFOCUS_GAIN)
				PlayerService.resume(PlayerService.this, Constants.PAUSE_AUDIOFOCUS);
			else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
					focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
				PlayerService.pause(PlayerService.this, Constants.PAUSE_AUDIOFOCUS);
		}
	};

	protected MediaPlayer _player;
	protected boolean _onPhone;
	protected Timer _updateTimer;
	private boolean _pausingFor[] = new boolean[] {false, false};

	private LockscreenManager _lockscreenManager = null;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void createUpdateTimer() {
		if (_updateTimer != null)
			_updateTimer.cancel();
		_updateTimer = new Timer();
		_updateTimer.schedule(new UpdatePositionTimerTask(), 250, 250);
	}

	private void stopUpdateTimer() {
		if (_updateTimer == null)
			return;

		_updateTimer.cancel();
		_updateTimer = null;
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
		case Constants.PLAYER_COMMAND_SKIPTO:
			skipTo(intent.getIntExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, 0));
			break;
		case Constants.PLAYER_COMMAND_SKIPTOEND:
			playNextPodcast();
			break;
		case Constants.PLAYER_COMMAND_RESTART:
			restart();
			break;
		case Constants.PLAYER_COMMAND_SKIPBACK:
			skip(-15);
			break;
		case Constants.PLAYER_COMMAND_SKIPFORWARD:
			skip(30);
			break;
		case Constants.PLAYER_COMMAND_PLAYPAUSE:
			if (_player.isPlaying())
				pauseForReason(pauseReason);
			else
				grabAudioFocusAndResume();
			break;
		case Constants.PLAYER_COMMAND_PLAYSTOP:
			if (_player != null && _player.isPlaying())
				stop();
			else
				grabAudioFocusAndResume();
			break;
		case Constants.PLAYER_COMMAND_PLAY:
			grabAudioFocusAndResume();
			break;
		case Constants.PLAYER_COMMAND_PAUSE:
			pauseForReason(pauseReason);
			break;
		case Constants.PLAYER_COMMAND_RESUME:
			if (unpauseForReason(pauseReason))
				grabAudioFocusAndResume();
			break;
		case Constants.PLAYER_COMMAND_STOP:
			stop();
			break;
		case Constants.PLAYER_COMMAND_PLAY_SPECIFIC_PODCAST:
			long podcastId = intent.getLongExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, -1);
			play(podcastId);
			break;
		}
		
		return START_NOT_STICKY;
	}

	public boolean isPausedForAnyReason() {
		// make sure all of our pause reasons are OK
		for (int i = 0; i < Constants.PAUSE_COUNT; ++i) {
			if (_pausingFor[i])
				return false;
		}

		return true;
	}

	private boolean unpauseForReason(int reason) {
		if (reason == -1)
			return false;

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

		PodaxLog.log(this, "PlayerService stopping");

		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		am.abandonAudioFocus(_afChangeListener);

		stopUpdateTimer();

		removeNotification();

		_lockscreenManager.removeLockscreenControls();

		if (_player != null && _player.isPlaying()) {
			_player.pause();
			updateActivePodcastPosition(_player.getCurrentPosition());
			_player.stop();
		}

		_player = null;

		// tell anything listening to the active podcast to refresh now that we're stopped
		ContentValues values = new ContentValues();
		getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);

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

	private void grabAudioFocusAndResume() {
		if (!grabAudioFocus())
			return;

		// make sure we don't pause for media button when audio focus event happens
		for (int i = 0; i < Constants.PAUSE_COUNT; ++i)
			_pausingFor[i] = false;

		// find out where to start playing
		String[] projection = new String[] {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_ID,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
				PodcastProvider.COLUMN_MEDIA_URL,
				PodcastProvider.COLUMN_LAST_POSITION,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_FILE_SIZE,
		};
		Cursor c = getContentResolver().query(PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
		if (c.isAfterLast())
			return;
		PodcastCursor p = new PodcastCursor(c);
		
		// make sure the podcast is downloaded
		if (!p.isDownloaded()) {
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
		PlayerStatus.updateState(this, PlayerStates.PLAYING);

		_lockscreenManager = new LockscreenManager();
		_lockscreenManager.setupLockscreenControls(this, p);

		c.close();

		createUpdateTimer();
		showNotification();
	}

	private boolean prepareMediaPlayer(PodcastCursor p) {
		try {
			_player.reset();
			_player.setDataSource(p.getFilename());
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

	private void play(long podcastId) {
		QueueManager.changeActivePodcast(this, podcastId);
		grabAudioFocusAndResume();
	}

	private void skip(int secs) {
		if (_player.isPlaying()) {
			_player.seekTo(_player.getCurrentPosition() + secs * 1000);
			updateActivePodcastPosition(_player.getCurrentPosition());
		} else {
			String[] projection = { PodcastProvider.COLUMN_LAST_POSITION };
			Cursor c = getContentResolver().query(PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
			try {
				if (c.moveToNext()) {
					updateActivePodcastPosition(c.getInt(0) + secs * 1000);
				}
			} finally {
				c.close();
			}
		}
	}

	private void skipTo(int secs) {
		if (_player.isPlaying()) {
			_player.seekTo(secs * 1000);
			updateActivePodcastPosition(_player.getCurrentPosition());
		} else {
			updateActivePodcastPosition(secs * 1000);
		}
	}

	private void restart() {
		if (_player.isPlaying()) {
			_player.seekTo(0);
			updateActivePodcastPosition(_player.getCurrentPosition());
		} else {
			updateActivePodcastPosition(0);
		}
	}

	private void playNextPodcast() {
		PodaxLog.log(this, "moving to next podcast");

		if (_player != null) {
			// stop the player and the updating while we do some administrative stuff
			_player.pause();
			stopUpdateTimer();
			updateActivePodcastPosition(_player.getCurrentPosition());
		}

		QueueManager.moveToNextInQueue(this);

		if (!PlayerStatus.getCurrentState(this).hasActivePodcast())
			stop();

		grabAudioFocusAndResume();
	}

	private void showNotification() {
		String[] projection = new String[] {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_ID,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
		};
		Cursor c = getContentResolver().query(PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
		if (c.isAfterLast())
			return;
		PodcastCursor podcast = new PodcastCursor(c);

		// both paths use the pendingintent
		Intent showIntent = new Intent(this, PodcastDetailActivity.class);
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
		Intent forwardIntent = new Intent(this, PlayerService.class);
		// use data to make intent unique
		forwardIntent.setData(Uri.parse("podax://playercommand/forward"));
		forwardIntent.putExtra(Constants.EXTRA_PLAYER_COMMAND, Constants.PLAYER_COMMAND_SKIPFORWARD);
		PendingIntent forwardPendingIntent = PendingIntent.getService(this, 0, forwardIntent, 0);

		try {
			long subscriptionId = podcast.getSubscriptionId();
			String imageFilename = SubscriptionCursor.getThumbnailFilename(subscriptionId);
			if (new File(imageFilename).exists()) {
				Bitmap subscriptionBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(imageFilename), 128, 128, false);
				builder.setLargeIcon(subscriptionBitmap);
			}
		} catch (OutOfMemoryError e) {
		}

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
		getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);

		notifyPositionChanged(position);
	}

	private void notifyPositionChanged(int currentPosition) {
		Intent intent = new Intent(Constants.ACTION_PLAYER_POSITIONCHANGED);
		intent.putExtra(Constants.EXTRA_POSITION, currentPosition);
		sendBroadcast(intent, Constants.PERMISSION_PLAYERCHANGES);
	}

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

	public static void skipForward(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_SKIPFORWARD);
	}

	public static void skipBack(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_SKIPBACK);
	}

	public static void restart(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_RESTART);
	}

	public static void skipToEnd(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_SKIPTOEND);
	}

	public static void skipTo(Context context, int secs) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_SKIPTO, secs);
	}

	public static void play(Context context, long podcastId) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAY_SPECIFIC_PODCAST, podcastId);
	}

	public static void play(Context context, PodcastCursor podcast) {
		if (podcast == null)
			return;
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAY_SPECIFIC_PODCAST, podcast.getId());
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

	private static void sendCommand(Context context, int command, long arg) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, arg);
		context.startService(intent);
	}
}
