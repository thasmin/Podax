package com.axelby.podax;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;

public class PlayerService extends Service {
	public class PlayerBinder extends Binder {
		PlayerService getService() {
			return PlayerService.this;
		}
	}
	
	public class UpdateWidgetTimerTask extends TimerTask {
		public void run() {
			updateWidget();
		}
	}
	protected UpdateWidgetTimerTask _updateWidgetTask;
	
	public class UpdateCurrentPodcastTimerTask extends TimerTask {
		public void run() {
			DBAdapter.getInstance(PlayerService.this).updatePodcastPosition(_activePodcast.getId(), _player.getCurrentPosition());
		}
	}
	protected UpdateCurrentPodcastTimerTask _updateCurrentPodcast;

	protected MediaPlayer _player = new MediaPlayer();
	protected PlayerBinder _binder = new PlayerBinder();
	protected DBAdapter _dbAdapter;
	protected Podcast _activePodcast;
	protected boolean _onPhone = false;
	protected boolean _pausedForPhone = false;
	private TelephonyManager _telephony;
	protected Timer _updateTimer = new Timer();
	
	@Override
	public IBinder onBind(Intent intent) {
		handleStart(intent);
		return _binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Log.d("Podax", "PlayerService onCreate");
		_player.setOnCompletionListener(new OnCompletionListener() {
			public void onCompletion(MediaPlayer player) {
				// verify completion -- not sure why this is necessary
				if (player.getCurrentPosition() < player.getDuration()) {
					return;
				}

				// not sure how this would happen
				if (_activePodcast == null) {
					Log.d("Podax", "PlayerService stopSelf - bug");
					stopPlayerService();
					return;
				}
				
				DBAdapter dbAdapter = DBAdapter.getInstance(PlayerService.this);
				dbAdapter.updatePodcastPosition(_activePodcast.getId(), 0);
				dbAdapter.removePodcastFromQueue(_activePodcast.getId());
				Podcast nextPodcast = dbAdapter.getFirstInQueue();
				if (nextPodcast == null) {
					Log.d("Podax", "PlayerService stopSelf - queue finished");
					stopPlayerService();
					dbAdapter.clearLastPlayedPodcast();
					return;
				}
				
				play(nextPodcast);
			}
		});
	}

	@Override
	public void onStart(Intent intent, int startId) {
		_dbAdapter = DBAdapter.getInstance(this);
		handleStart(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("Podax", "Started PlayerService");
		_dbAdapter = DBAdapter.getInstance(this);
		handleStart(intent);
		return START_STICKY;
	}

	private void handleStart(Intent intent) {
		_player.setAudioStreamType(AudioManager.STREAM_MUSIC);

		if (_activePodcast == null) {
			// see if a specific podcast is supposed to be played
			int podcastId = intent.getIntExtra("com.axelby.podax.podcast", 0);
			if (podcastId != -1)
				play(DBAdapter.getInstance(this).loadPodcast(podcastId));
			else
				play();
		}
		
		if (intent.getExtras() != null && intent.getExtras().containsKey(Intent.EXTRA_KEY_EVENT)) {
			KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
			if (keyEvent != null) {
				if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
					return;
				if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
					if (isPlaying())
						pause();
					else
						play();
				}
				return;
			}
		}
				
		_telephony = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		_telephony.listen(new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				_onPhone = (state != TelephonyManager.CALL_STATE_IDLE);
				if (isPlaying() && _onPhone) {
					pause();
					_pausedForPhone = true;
				}
				if (!isPlaying() && !_onPhone && _pausedForPhone)
					play();
			}
		}, PhoneStateListener.LISTEN_CALL_STATE);
		_onPhone = (_telephony.getCallState() != TelephonyManager.CALL_STATE_IDLE);
	}

	public void pause() {
		_pausedForPhone = false;
		_player.pause();
		DBAdapter.getInstance(this).updatePodcastPosition(_activePodcast.getId(), _player.getCurrentPosition());
		updateWidget();
		Log.d("Podax", "PlayerService stopSelf - pause");
		stopPlayerService();
	}
	
	public void play() {
		play(null);
	}

	public void play(Podcast podcast) {
		if (_onPhone)
			return;
		
		// determine which podcast to play
		// priority: request, resume, queue
		_activePodcast = podcast;
		DBAdapter dbAdapter = DBAdapter.getInstance(this);
		if (_activePodcast == null)
			_activePodcast = dbAdapter.loadLastPlayedPodcast();
		if (_activePodcast == null)
			_activePodcast = dbAdapter.getFirstInQueue();
		if (_activePodcast == null)
		{
			stopPlayerService();
			return;
		}
		
		// prep the MediaPlayer
		try {
			_player.reset();
			_player.setDataSource(_activePodcast.getFilename());
			_player.prepare();
			_player.seekTo(_activePodcast.getLastPosition());
			_activePodcast.setDuration(_player.getDuration());
			DBAdapter.getInstance(this).savePodcast(_activePodcast);
		}
		catch (IOException ex) {
			stopPlayerService();
		}
		
		_pausedForPhone = false;
		_player.start();

		updateWidget();
		if (_updateWidgetTask != null)
			_updateWidgetTask.cancel();
		_updateWidgetTask = new UpdateWidgetTimerTask();
		_updateTimer.schedule(_updateWidgetTask, 250, 250);
		
		if (_updateCurrentPodcast != null)
			_updateCurrentPodcast.cancel();
		_updateCurrentPodcast = new UpdateCurrentPodcastTimerTask();
		_updateTimer.schedule(_updateCurrentPodcast, 250, 250);
	}

	public void skip(int secs) {
		_player.seekTo(_player.getCurrentPosition() + secs * 1000);
		updateWidget();
	}

	public void restart() {
		_player.seekTo(0);
		updateWidget();
	}

	public void skipToEnd() {
		_player.seekTo(_player.getDuration());
		updateWidget();
	}
	
	public boolean isPlaying() {
		return _player.isPlaying();
	}

	public int getDuration() {
		return _player.getDuration();
	}
	
	public int getPosition() {
		return _player.getCurrentPosition();
	}

	public Podcast getActivePodcast() {
		return _activePodcast;
	}

	public void updateWidget() {
		RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget);
		if (_activePodcast == null || !_player.isPlaying()) {
			views.setTextViewText(R.id.title, "");
			views.setTextViewText(R.id.position, "");
			views.setImageViewResource(R.id.play_btn, android.R.drawable.ic_media_play);
		} else {
			views.setTextViewText(R.id.title, _activePodcast.getTitle());
			views.setTextViewText(R.id.position, PlayerService.getPositionString(getDuration(), getPosition()));
			int imageRes = isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
			views.setImageViewResource(R.id.play_btn, imageRes);
		}
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
		appWidgetManager.updateAppWidget(new ComponentName(this, "com.axelby.podax.WidgetProvider"), views);
	}

	public String getPositionString() {
		if (this.getDuration() == 0) 
			return "";
		return PlayerActivity.getTimeString(this.getPosition()) + " / " + PlayerActivity.getTimeString(this.getDuration());
	}

	private void stopPlayerService() {
		if (_updateWidgetTask != null) {
			_updateWidgetTask.cancel();
		}
		if (_updateCurrentPodcast != null) {
			_updateCurrentPodcast.cancel();
		}

		stopSelf();
	}

	public static String getPositionString(int duration, int position) {
		if (duration == 0) 
			return "";
		return PlayerActivity.getTimeString(position) + " / " + PlayerActivity.getTimeString(duration);
	}
}
