package com.axelby.podax;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class PlayerService extends Service {
	public class PlayerBinder extends Binder {
		PlayerService getService() {
			return PlayerService.this;
		}
	}
	
	public class UpdatePlayerPositionTimerTask extends TimerTask {
		public void run() {
			int _oldPosition = _lastPosition;
			_lastPosition = _player.getCurrentPosition();
			if (_oldPosition / 1000 != _lastPosition / 1000)
			{
				_activePodcast.setLastPosition(_player.getCurrentPosition());
				_dbAdapter.updatePodcastPosition(_activePodcast, _player.getCurrentPosition());
				PodaxApp.updateWidgets(PlayerService.this);
			}
		}
	}
	protected UpdatePlayerPositionTimerTask _updatePlayerPositionTimerTask;

	protected MediaPlayer _player;
	protected PlayerBinder _binder;
	protected DBAdapter _dbAdapter;
	protected static Podcast _activePodcast;
	protected boolean _onPhone;
	protected boolean _pausedForPhone;
	private TelephonyManager _telephony;
	protected Timer _updateTimer;
	
	// static methods
	protected static boolean _isPlaying = false;
	public static boolean isPlaying() {
		return _isPlaying;
	}
	
	public static Podcast getActivePodcast(Context context) {
		if (_activePodcast == null) {
			DBAdapter dbAdapter = DBAdapter.getInstance(context);
			_activePodcast = dbAdapter.loadLastPlayedPodcast();
			if (_activePodcast == null)
				_activePodcast = findFirstDownloadedInQueue(context);
		}
		return _activePodcast;
	}

	public static Podcast findFirstDownloadedInQueue(Context context) {
		// make sure the active podcast has been downloaded
		Vector<Podcast> queue = DBAdapter.getInstance(context).getQueue();
		for (Podcast p : queue) {
			if (p.isDownloaded())
				return p;
		}
		return null;
	}

	protected static int _lastPosition = 0;
	public static int getLastPosition() {
		return _lastPosition;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		handleIntent(intent);
		return _binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		_player = new MediaPlayer();
		_binder = new PlayerBinder();
		_onPhone = false;
		_pausedForPhone = false;
		_updateTimer = new Timer();
		_dbAdapter = DBAdapter.getInstance(this);

		getActivePodcast(this);

		_player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		
		_isPlaying = true;
		
		// may or may not be creating the service
		if (_telephony == null) {
			_telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			_telephony.listen(new PhoneStateListener() {
				@Override
				public void onCallStateChanged(int state, String incomingNumber) {
					_onPhone = (state != TelephonyManager.CALL_STATE_IDLE);
					if (_player.isPlaying() && _onPhone) {
						_isPlaying = false;
						_player.pause();
						_dbAdapter.updatePodcastPosition(_activePodcast,
										_player.getCurrentPosition());
						PodaxApp.updateWidgets(PlayerService.this);
						_pausedForPhone = true;
					}
					if (!_player.isPlaying() && !_onPhone && _pausedForPhone) {
						_isPlaying = true;
						_player.start();
						_pausedForPhone = false;
						PodaxApp.updateWidgets(PlayerService.this);
					}
				}
			}, PhoneStateListener.LISTEN_CALL_STATE);
			_onPhone = (_telephony.getCallState() != TelephonyManager.CALL_STATE_IDLE);
		}

		// handle errors so the onCompletionListener doens't get called
		_player.setOnErrorListener(new OnErrorListener() {
			public boolean onError(MediaPlayer mp, int what, int extra) {
				return true;
			}
		});

		_player.setOnCompletionListener(new OnCompletionListener() {
			public void onCompletion(MediaPlayer player) {
				playNextPodcast();
			}
		});
	}
	
	

	@Override
	public void onDestroy() {
		super.onDestroy();
		
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
				skipToEnd();
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
				Log.d("Podax", "PlayerService got a command: pause");
				Podcast p = _dbAdapter.loadPodcast(intent.getIntExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, -1));
				if (p.isDownloaded())
					play(p);
				else
					Toast.makeText(this, R.string.podcast_not_downloaded, Toast.LENGTH_SHORT);
				break;
			}
		}
	}

	public void stop() {
		Log.d("Podax", "PlayerService stopping");
		_isPlaying = false;
		if (_updatePlayerPositionTimerTask != null)
			_updatePlayerPositionTimerTask.cancel();
		if (_activePodcast != null)
			_dbAdapter.updatePodcastPosition(_activePodcast, _player.getCurrentPosition());
		PodaxApp.updateWidgets(this);
		_player.stop();
		stopSelf();
	}
	
	public void resume() {
		if (_onPhone)
			return;
		Podcast p = getActivePodcast(this);
		if (p == null)
			return;

		// prep the MediaPlayer
		try {
			_player.reset();
			_player.setDataSource(p.getFilename());
			_player.prepare();
			_player.seekTo(p.getLastPosition());
			p.setDuration(_player.getDuration());
			_dbAdapter.savePodcast(p);
		}
		catch (IOException ex) {
			stop();
		}
				
		// the user will probably try this if the podcast is over and the next one didn't start
		if (_player.getCurrentPosition() >= _player.getDuration() - 1000) {
			playNextPodcast();
		}

		_pausedForPhone = false;
		_player.start();

		PodaxApp.updateWidgets(this);
		
		if (_updatePlayerPositionTimerTask != null)
			_updatePlayerPositionTimerTask.cancel();
		_updatePlayerPositionTimerTask = new UpdatePlayerPositionTimerTask();
		_updateTimer.schedule(_updatePlayerPositionTimerTask, 250, 250);
	}

	public void play(Podcast podcast) {
		_activePodcast = podcast;
		if (_activePodcast == null)
		{
			stop();
			return;
		}

		resume();
	}

	public void skip(int secs) {
		_player.seekTo(_player.getCurrentPosition() + secs * 1000);
		_dbAdapter.updatePodcastPosition(getActivePodcast(this), _player.getCurrentPosition());
		PodaxApp.updateWidgets(this);
	}

	public void skipTo(int secs) {
		_player.seekTo(secs * 1000);
		_dbAdapter.updatePodcastPosition(getActivePodcast(this), _player.getCurrentPosition());
		PodaxApp.updateWidgets(this);
	}

	public void restart() {
		_player.seekTo(0);
		_dbAdapter.updatePodcastPosition(getActivePodcast(this), _player.getCurrentPosition());
		PodaxApp.updateWidgets(this);
	}

	public void skipToEnd() {
		_player.seekTo(_player.getDuration());
		_dbAdapter.updatePodcastPosition(getActivePodcast(this), _player.getCurrentPosition());
		PodaxApp.updateWidgets(this);
	}

	public String getPositionString() {
		if (_player.getDuration() == 0)
			return "";
		return PlayerActivity.getTimeString(_player.getCurrentPosition())
				+ " / " + PlayerActivity.getTimeString(_player.getDuration());
	}

	private void playNextPodcast() {
		Log.d("Podax", "moving to next podcast");

		if (_activePodcast != null) {
			_dbAdapter.updatePodcastPosition(_activePodcast, 0);
			_dbAdapter.removePodcastFromQueue(_activePodcast);
		}

		_activePodcast = findFirstDownloadedInQueue(this);

		if (_activePodcast == null) {
			Log.d("Podax", "PlayerService queue finished");
			_dbAdapter.clearLastPlayedPodcast();
			stop();
			return;
		}

		resume();
	}

	public static String getPositionString(int duration, int position) {
		if (duration == 0)
			return "";
		return PlayerActivity.getTimeString(position) + " / "
				+ PlayerActivity.getTimeString(duration);
	}
}
