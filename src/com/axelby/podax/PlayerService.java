package com.axelby.podax;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
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
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.axelby.podax.R.drawable;
import com.axelby.podax.ui.LargeWidgetProvider;
import com.axelby.podax.ui.PodcastDetailActivity;
import com.axelby.podax.ui.SmallWidgetProvider;

public class PlayerService extends Service {
	public class PlayerBinder extends Binder {
		PlayerService getService() {
			return PlayerService.this;
		}
	}

	protected int _lastPosition = 0;
	public class UpdatePlayerPositionTimerTask extends TimerTask {
		public void run() {
			if (!_player.isPlaying())
				return;
			int oldPosition = _lastPosition;
			_lastPosition = _player.getCurrentPosition();
			if (oldPosition / 1000 != _lastPosition / 1000)
				updateActivePodcastPosition();
		}
	}
	protected UpdatePlayerPositionTimerTask _updatePlayerPositionTimerTask;

	protected MediaPlayer _player;
	protected PlayerBinder _binder;
	protected boolean _onPhone;
	protected boolean _pausedForPhone;
	protected Timer _updateTimer;
	private static final Uri _activePodcastUri = Uri.withAppendedPath(PodcastProvider.URI, "active");
	
	private OnAudioFocusChangeListener _afChangeListener = new OnAudioFocusChangeListener() {
		public void onAudioFocusChange(int focusChange) {
			// focusChange could be AUDIOFOCUS_GAIN, AUDIOFOCUS_LOSS,
			// _LOSS_TRANSIENT or _LOSS_TRANSIENT_CAN_DUCK
			if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
				doResume();
			} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
				doStop();
			}
		}
	};

	private final HeadsetConnectionReceiver _headsetConnectionReceiver = new HeadsetConnectionReceiver();
	private final BluetoothConnectionReceiver _bluetoothConnectionReceiver = new BluetoothConnectionReceiver();

	private PhoneStateListener _phoneStateListener;

	@Override
	public IBinder onBind(Intent intent) {
		handleIntent(intent);
		return _binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		_updateTimer = new Timer();
		_binder = new PlayerBinder();

		verifyPodcastReady();
		setupMediaPlayer();

		// may or may not be creating the service
		TelephonyManager _telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		_phoneStateListener = new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				_onPhone = (state != TelephonyManager.CALL_STATE_IDLE);
				if (_onPhone) {
					_player.pause();
					updateActivePodcastPosition();
					_pausedForPhone = true;
				}
				if (!_onPhone && _pausedForPhone) {
					_player.start();
					updateActivePodcastPosition();
					_pausedForPhone = false;
				}
			}
		};
		_telephony.listen(_phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		_onPhone = (_telephony.getCallState() != TelephonyManager.CALL_STATE_IDLE);

		// hook our headset connection and disconnection
		this.registerReceiver(_headsetConnectionReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		// hook our bluetooth headset connection and disconnection
		//this.registerReceiver(_bluetoothConnectionReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
		this.registerReceiver(_bluetoothConnectionReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
	}

	private void setupMediaPlayer() {
		if (_player == null) {
			_player = new MediaPlayer();
			_player.setAudioStreamType(AudioManager.STREAM_MUSIC);

			_pausedForPhone = false;

			// handle errors so the onCompletionListener doens't get called
			_player.setOnErrorListener(new OnErrorListener() {
				public boolean onError(MediaPlayer mp, int what, int extra) {
					PodaxLog.log(PlayerService.this, "mediaplayer error - what: %d, extra: %d", what, extra);
					return true;
				}
			});

			_player.setOnCompletionListener(new OnCompletionListener() {
				public void onCompletion(MediaPlayer player) {
					removeActivePodcastFromQueue();
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

		Log.d("Podax", "destroying PlayerService");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		handleIntent(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleIntent(intent);
		return START_STICKY;
	}

	private void handleIntent(Intent intent) {
		if (intent == null || intent.getExtras() == null)
			return;
		if (intent.getExtras().containsKey(Constants.EXTRA_PLAYER_COMMAND)) {
			switch (intent.getIntExtra(Constants.EXTRA_PLAYER_COMMAND, -1)) {
			case -1:
				return;
			case Constants.PLAYER_COMMAND_SKIPTO:
				Log.d("Podax", "PlayerService got a command: skip to");
				skipTo(intent.getIntExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, 0));
				break;
			case Constants.PLAYER_COMMAND_SKIPTOEND:
				Log.d("Podax", "PlayerService got a command: skip to end");
				removeActivePodcastFromQueue();
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
					Log.d("Podax", "  stopping the player");
					stop();
				} else {
					Log.d("Podax", "  resuming a podcast");
					resume();
				}
				break;
			case Constants.PLAYER_COMMAND_PLAY:
				Log.d("Podax", "PlayerService got a command: play");
				resume();
				break;
			case Constants.PLAYER_COMMAND_PAUSE:
				Log.d("Podax", "PlayerService got a command: pause");
				stop();
				break;
			case Constants.PLAYER_COMMAND_PLAY_SPECIFIC_PODCAST:
				Log.d("Podax", "PlayerService got a command: play specific podcast");
				int podcastId = intent.getIntExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, -1);
				play((long)podcastId);
				break;
			}
		}
	}

	public void stop() {
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		am.abandonAudioFocus(_afChangeListener);

		doStop();
	}

	private void doStop() {
		Log.d("Podax", "PlayerService stopping");
		if (_updatePlayerPositionTimerTask != null)
			_updatePlayerPositionTimerTask.cancel();
		if (_updateTimer != null)
			_updateTimer.cancel();

		TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephony.listen(_phoneStateListener, PhoneStateListener.LISTEN_NONE);

		removeNotification();
		_player.pause();

		updateActivePodcastPosition();

		_player.stop();
		_player = null;
		stopSelf();

		// tell anything listening to the active podcast to refresh now that we're stopped
		ContentValues values = new ContentValues();
		getContentResolver().update(_activePodcastUri, values, null, null);

		updateWidgets();
	}
	
	public void resume() {
		if (_onPhone)
			return;

		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int result = am.requestAudioFocus(_afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
		if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED)
			stop();

		doResume();
	}

	private void doResume() {
		PodaxLog.log(this, "PlayerService doResume");

		String[] projection = new String[] {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_MEDIA_URL,
				PodcastProvider.COLUMN_LAST_POSITION,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_FILE_SIZE,
		};
		Cursor c = getContentResolver().query(_activePodcastUri, projection, null, null, null);
		try {
			PodcastCursor p = new PodcastCursor(c);
			if (p.isNull())
				return;
			if (!p.isDownloaded()) {
				Toast.makeText(this, R.string.podcast_not_downloaded, Toast.LENGTH_SHORT).show();
				return;
			}

			// don't update the podcast position while the player is reset
			if (_updatePlayerPositionTimerTask != null) {
				_updatePlayerPositionTimerTask.cancel();
				_updatePlayerPositionTimerTask = null;
			}

			if (_player == null)
				_player = new MediaPlayer();
			_player.reset();
			_player.setDataSource(p.getFilename());
			_player.prepare();
			_player.seekTo(p.getLastPosition());
			
			// set this podcast as active -- it may have been first in queue
			changeActivePodcast(p.getId());
		}
		catch (IOException ex) {
			stop();
		} finally {
			c.close();
		}

		// the user will probably try this if the podcast is over and the next one didn't start
		if (_player.getCurrentPosition() >= _player.getDuration() - 1000) {
			removeActivePodcastFromQueue();
			playNextPodcast();
			return;
		}

		_pausedForPhone = false;
		_player.start();

		showNotification();

		_updatePlayerPositionTimerTask = new UpdatePlayerPositionTimerTask();
		_updateTimer.schedule(_updatePlayerPositionTimerTask, 250, 250);

		updateWidgets();
	}

	public void play(Long podcastId) {
		changeActivePodcast(podcastId);
		if (podcastId == null) {
			stop();
			return;
		}
		resume();
	}

	public void skip(int secs) {
		if (_player.isPlaying()) {
			_player.seekTo(_player.getCurrentPosition() + secs * 1000);
			updateActivePodcastPosition();
		} else {
			String[] projection = { PodcastProvider.COLUMN_LAST_POSITION };
			Cursor c = getContentResolver().query(_activePodcastUri, projection, null, null, null);
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
			updateActivePodcastPosition();
		} else {
			updateActivePodcastPosition(secs * 1000);
		}
	}

	public void restart() {
		if (_player.isPlaying()) {
			_player.seekTo(0);
			updateActivePodcastPosition();
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
		if (_updatePlayerPositionTimerTask != null) {
			_updatePlayerPositionTimerTask.cancel();
			_updatePlayerPositionTimerTask = null;
		}
		updateActivePodcastPosition();

		Long activePodcastId = moveToNextInQueue();

		if (activePodcastId == null) {
			Log.d("Podax", "PlayerService queue finished");
			stop();
			return;
		}

		resume();
	}

	public Long findFirstDownloadedInQueue() {
		// make sure the active podcast has been downloaded
		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_FILE_SIZE,
				PodcastProvider.COLUMN_MEDIA_URL,
		};
		Uri queueUri = Uri.withAppendedPath(PodcastProvider.URI, "queue");
		Cursor c = getContentResolver().query(queueUri, projection, null, null, null);
		try {
			while (c.moveToNext()) {
				PodcastCursor podcast = new PodcastCursor(c);
				if (podcast.isDownloaded())
					return podcast.getId();
			}
			return null;
		} finally {
			c.close();
		}
	}

	public Long moveToNextInQueue() {
		Long activePodcastId = findFirstDownloadedInQueue();
		if (activePodcastId == null)
			stop();
		changeActivePodcast(activePodcastId);
		return activePodcastId;
	}

	public void changeActivePodcast(Long activePodcastId) {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_ID, activePodcastId);
		getContentResolver().update(_activePodcastUri, values, null, null);

		// if the podcast has ended and it's back in the queue, restart it
		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_LAST_POSITION,
		};
		Cursor c = getContentResolver().query(_activePodcastUri, projection, null, null, null);
		try {
			if (c.moveToNext()) {
				PodcastCursor podcast = new PodcastCursor(c);
				if (podcast.getDuration() > 0 && podcast.getLastPosition() > podcast.getDuration() - 1000)
					podcast.setLastPosition(this, 0);
			}
		} finally {
			c.close();
		}
	}

	public Long verifyPodcastReady() {
		String[] projection = new String[] { PodcastProvider.COLUMN_ID };
		Cursor c = getContentResolver().query(_activePodcastUri, projection, null, null, null);
		try {
			if (c.moveToNext())
				return c.getLong(0);
			else
				return moveToNextInQueue();
		} finally {
			c.close();
		}
	}

	private void showNotification() {
		String[] projection = new String[] {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
		};
		Cursor c = getContentResolver().query(_activePodcastUri, projection, null, null, null);
		if (c.isAfterLast())
			return;
		PodcastCursor podcast = new PodcastCursor(c);

		Intent notificationIntent = new Intent(this, PodcastDetailActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		Notification notification = new NotificationCompat.Builder(this)
			.setSmallIcon(drawable.icon)
			.setTicker(podcast.getTitle())
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

	private void updateWidgets() {
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);

		int[] widgetIds;

		widgetIds = widgetManager.getAppWidgetIds(new ComponentName(this, LargeWidgetProvider.class));
		if (widgetIds.length > 0) {
			AppWidgetProvider provider = (AppWidgetProvider) new LargeWidgetProvider();
			provider.onUpdate(this, widgetManager, widgetIds);
		}

		widgetIds = widgetManager.getAppWidgetIds(new ComponentName(this, SmallWidgetProvider.class));
		if (widgetIds.length > 0) {
			AppWidgetProvider provider = (AppWidgetProvider) new SmallWidgetProvider();
			provider.onUpdate(this, widgetManager, widgetIds);
		}
	}

	public void updateActivePodcastPosition() {
		if (!_player.isPlaying())
			return;

		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_LAST_POSITION, _player.getCurrentPosition());
		PlayerService.this.getContentResolver().update(_activePodcastUri, values, null, null);

		// update widgets
		updateWidgets();
	}

	public void updateActivePodcastPosition(int position) {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_LAST_POSITION, position);
		PlayerService.this.getContentResolver().update(_activePodcastUri, values, null, null);

		// update widgets
		updateWidgets();
	}

	public void removeActivePodcastFromQueue() {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_LAST_POSITION, 0);
		values.put(PodcastProvider.COLUMN_QUEUE_POSITION, (Integer)null);
		PlayerService.this.getContentResolver().update(_activePodcastUri, values, null, null);
	}

	// static functions for easier controls
	public static void play(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAY);
	}

	public static void pause(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PAUSE);
	}

	public static void playpause(Context context) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAYPAUSE);
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

	public static void play(Context context, int podcastId) {
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAY_SPECIFIC_PODCAST, podcastId);
	}

	public static void play(Context context, PodcastCursor podcast) {
		if (podcast == null)
			return;
		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAY_SPECIFIC_PODCAST, (int)(long)podcast.getId());
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
}
