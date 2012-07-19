package com.axelby.podax;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.axelby.podax.PlayerStatus.PlayerStates;
import com.axelby.podax.R.drawable;
import com.axelby.podax.ui.PodcastDetailActivity;

public class PlayerService extends Service {

	private class UpdatePositionTimerTask extends TimerTask {
		protected int _lastPosition = 0;
		public void run() {
			if (_player != null && !_player.isPlaying())
				return;
			int oldPosition = _lastPosition;
			_lastPosition = _player.getCurrentPosition();
			PlayerStatus.updatePosition(_lastPosition);
			if (oldPosition / 1000 != _lastPosition / 1000)
				updateActivePodcastPosition(_lastPosition);
		}
	};

	private final OnAudioFocusChangeListener _afChangeListener = new OnAudioFocusChangeListener() {
		public void onAudioFocusChange(int focusChange) {
			// focusChange could be AUDIOFOCUS_GAIN, AUDIOFOCUS_LOSS,
			// _LOSS_TRANSIENT or _LOSS_TRANSIENT_CAN_DUCK
			if (focusChange == AudioManager.AUDIOFOCUS_GAIN && !PlayerStatus.isPaused())
				resume();
			else if (focusChange == AudioManager.AUDIOFOCUS_LOSS)
				pause();
		}
	};

	private final PhoneStateListener _phoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			_onPhone = (state != TelephonyManager.CALL_STATE_IDLE);
			if (_onPhone) {
				_player.pause();
				PlayerStatus.updateState(PlayerStates.PAUSED);
				updateActivePodcastPosition(_player.getCurrentPosition());
				_pausedForPhone = true;
			}
			if (!_onPhone && _pausedForPhone) {
				_player.start();
				PlayerStatus.updateState(PlayerStates.PLAYING);
				updateActivePodcastPosition(_player.getCurrentPosition());
				_pausedForPhone = false;
			}
		}
	};

	protected MediaPlayer _player;
	protected boolean _onPhone;
	protected boolean _pausedForPhone = false;
	protected Timer _updateTimer;

	private final HeadsetConnectionReceiver _headsetConnectionReceiver = new HeadsetConnectionReceiver();
	private final BluetoothConnectionReceiver _bluetoothConnectionReceiver = new BluetoothConnectionReceiver();
	private final LockscreenManager _lockscreenManager = new LockscreenManager();

	@Override
	public IBinder onBind(Intent intent) {
		handleIntent(intent);
		return null;
	}

	private void createUpdateTimer() {
		if (_updateTimer != null)
			return;

		_updateTimer = new Timer();
		_updateTimer.schedule(new UpdatePositionTimerTask(), 250, 250);
	}

	private void stopUpdateTimer() {
		if (_updateTimer == null)
			return;

		_updateTimer.cancel();
		_updateTimer = null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		PlayerStatus.initialize(this);
		setupMediaPlayer();
		registerReceiver(_headsetConnectionReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		registerReceiver(_bluetoothConnectionReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
	}

	private void setupTelephony() {
		TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephony.listen(_phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		_onPhone = (telephony.getCallState() != TelephonyManager.CALL_STATE_IDLE);
	}

	private void setupMediaPlayer() {
		if (_player == null) {
			_player = new MediaPlayer();
			_player.setAudioStreamType(AudioManager.STREAM_MUSIC);

			// handle errors so the onCompletionListener doens't get called
			_player.setOnErrorListener(new OnErrorListener() {
				public boolean onError(MediaPlayer mp, int what, int extra) {
					PodaxLog.log(PlayerService.this, "mediaplayer error - what: %d, extra: %d", what, extra);
					return true;
				}
			});

			_player.setOnCompletionListener(new OnCompletionListener() {
				public void onCompletion(MediaPlayer player) {
					playNextPodcast();
				}
			});
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		this.unregisterReceiver(_headsetConnectionReceiver);
		this.unregisterReceiver(_bluetoothConnectionReceiver);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleIntent(intent);
		return START_STICKY;
	}

	private void handleIntent(Intent intent) {
		if (intent == null || intent.getExtras() == null)
			return;

		if (!intent.getExtras().containsKey(Constants.EXTRA_PLAYER_COMMAND)) 
			return;

		switch (intent.getIntExtra(Constants.EXTRA_PLAYER_COMMAND, -1)) {
		case -1:
			return;
		case Constants.PLAYER_COMMAND_SKIPTO:
			Log.d("Podax", "PlayerService got a command: skip to");
			skipTo(intent.getIntExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, 0));
			break;
		case Constants.PLAYER_COMMAND_SKIPTOEND:
			Log.d("Podax", "PlayerService got a command: skip to end");
			playNextPodcast();
			break;
		case Constants.PLAYER_COMMAND_RESTART:
			Log.d("Podax", "PlayerService got a command: restart");
			restart();
			break;
		case Constants.PLAYER_COMMAND_SKIPBACK:
			Log.d("Podax", "PlayerService got a command: skip back");
			skip(-15);
			break;
		case Constants.PLAYER_COMMAND_SKIPFORWARD:
			Log.d("Podax", "PlayerService got a command: skip forward");
			skip(30);
			break;
		case Constants.PLAYER_COMMAND_PLAYPAUSE:
			Log.d("Podax", "PlayerService got a command: playpause");
			if (_player.isPlaying()) {
				Log.d("Podax", "  pausing");
				pause();
			} else {
				Log.d("Podax", "  resuming");
				grabAudioFocusAndResume();
			}
			break;
		case Constants.PLAYER_COMMAND_PLAYSTOP:
			Log.d("Podax", "PlayerService got a command: playstop");
			if (_player.isPlaying()) {
				Log.d("Podax", "  stopping");
				stop();
			} else {
				Log.d("Podax", "  resuming");
				grabAudioFocusAndResume();
			}
			break;
		case Constants.PLAYER_COMMAND_PLAY:
			Log.d("Podax", "PlayerService got a command: play");
			grabAudioFocusAndResume();
			break;
		case Constants.PLAYER_COMMAND_PAUSE:
			Log.d("Podax", "PlayerService got a command: pause");
			pause();
			break;
		case Constants.PLAYER_COMMAND_STOP:
			Log.d("Podax", "PlayerService got a command: stop");
			stop();
			break;
		case Constants.PLAYER_COMMAND_PLAY_SPECIFIC_PODCAST:
			Log.d("Podax", "PlayerService got a command: play specific podcast");
			long podcastId = intent.getLongExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, -1);
			play(podcastId);
			break;
		}
	}

	private void resume() {
		PodaxLog.log(this, "PlayerService resume");

		if (_player != null && PlayerStatus.isPaused()) {
			_player.start();
			PlayerStatus.updateState(PlayerStates.PLAYING);
			_lockscreenManager.setLockscreenPlaying();
			return;
		}

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
		try {
			if (c.isAfterLast())
				return;

			PodcastCursor p = new PodcastCursor(c);
			if (!p.isDownloaded()) {
				Toast.makeText(this, R.string.podcast_not_downloaded, Toast.LENGTH_SHORT).show();
				return;
			}

			prepareMediaPlayer(p);

			// set this podcast as active -- it may not have been first in queue
			QueueManager.changeActivePodcast(this, p.getId());

			_lockscreenManager.setupLockscreenControls(this, p);
		} catch (IOException ex) {
			stop();
		} finally {
			c.close();
		}

		// the user will probably try this if the podcast is over and the next one didn't start
		if (_player.getCurrentPosition() >= _player.getDuration() - 1000) {
			playNextPodcast();
			return;
		}

		_player.start();
		PlayerStatus.updateState(PlayerStates.PLAYING);

		_pausedForPhone = false;
		setupTelephony();
		showNotification();
		createUpdateTimer();
		Helper.updateWidgets(this);
	}

	public void pause() {
		Log.d("Podax", "PlayerService pausing");
		_player.pause();
		updateActivePodcastPosition(_player.getCurrentPosition());
		PlayerStatus.updateState(PlayerStates.PAUSED);
		_lockscreenManager.setLockscreenPaused();

		TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephony.listen(_phoneStateListener, PhoneStateListener.LISTEN_NONE);
	}

	public void stop() {
		Log.d("Podax", "PlayerService stopping");

		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		am.abandonAudioFocus(_afChangeListener);

		stopUpdateTimer();

		TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephony.listen(_phoneStateListener, PhoneStateListener.LISTEN_NONE);

		removeNotification();

		_lockscreenManager.removeLockscreenControls();

		if (_player != null) {
			_player.pause();
			updateActivePodcastPosition(_player.getCurrentPosition());
			_player.stop();
			PlayerStatus.updateState(PlayerStates.STOPPED);
			_player = null;
		}
		stopSelf();

		// tell anything listening to the active podcast to refresh now that we're stopped
		ContentValues values = new ContentValues();
		getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);

		Helper.updateWidgets(this);
	}

	public boolean grabAudioFocus() {
		if (_onPhone)
			return false;

		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int result = am.requestAudioFocus(_afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
			stop();
			return false;
		}

		// grab the media button when we have audio focus
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audioManager.registerMediaButtonEventReceiver(new ComponentName(this, MediaButtonIntentReceiver.class));

		return true;
	}

	public void grabAudioFocusAndResume() {
		if (grabAudioFocus())
			resume();
	}

	private void prepareMediaPlayer(PodcastCursor p) throws IOException {
		setupMediaPlayer();
		_player.reset();
		_player.setDataSource(p.getFilename());
		_player.prepare();
		_player.seekTo(p.getLastPosition());
	}

	public void play(long podcastId) {
		QueueManager.changeActivePodcast(this, podcastId);
		grabAudioFocusAndResume();
	}

	public void skip(int secs) {
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

	public void skipTo(int secs) {
		if (_player.isPlaying()) {
			_player.seekTo(secs * 1000);
			updateActivePodcastPosition(_player.getCurrentPosition());
		} else {
			updateActivePodcastPosition(secs * 1000);
		}
	}

	public void restart() {
		if (_player.isPlaying()) {
			_player.seekTo(0);
			updateActivePodcastPosition(_player.getCurrentPosition());
		} else {
			updateActivePodcastPosition(0);
		}
	}

	public String getPositionString() {
		if (_player.getDuration() == 0)
			return "";
		return Helper.getTimeString(_player.getCurrentPosition())
				+ " / " + Helper.getTimeString(_player.getDuration() - _player.getCurrentPosition());
	}

	private void playNextPodcast() {
		Log.d("Podax", "moving to next podcast");

		// stop the player and the updating while we do some administrative stuff
		_player.pause();
		stopUpdateTimer();
		updateActivePodcastPosition(_player.getCurrentPosition());

		QueueManager.moveToNextInQueue(this);

		grabAudioFocusAndResume();
	}

	private void showNotification() {
		String[] projection = new String[] {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
		};
		Cursor c = getContentResolver().query(PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
		if (c.isAfterLast())
			return;
		PodcastCursor podcast = new PodcastCursor(c);

		Intent notificationIntent = new Intent(this, PodcastDetailActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		Notification notification = new NotificationCompat.Builder(this)
			.setSmallIcon(drawable.icon)
			.setWhen(System.currentTimeMillis())
			.setContentTitle(podcast.getTitle())
			.setContentText(podcast.getSubscriptionTitle())
			.setContentIntent(contentIntent)
			.setOngoing(true)
			.getNotification();

		startForeground(Constants.NOTIFICATION_PLAYING, notification);

		c.close();
	}

	private void removeNotification() {
		stopForeground(true);
	}

	public void updateActivePodcastPosition(int position) {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_LAST_POSITION, position);
		PlayerService.this.getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);

		PlayerStatus.updatePosition(position);
		Helper.updateWidgets(this);
	}

	// static functions for easier controls
	public static void play(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAY);
	}

	public static void pause(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PAUSE);
	}

	public static void stop(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_STOP);
	}

	public static void playpause(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAYPAUSE);
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
