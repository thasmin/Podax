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
			if (!PlayerService.this.isPlaying())
				this.cancel();
		}
	}
	protected UpdateWidgetTimerTask _updateWidgetTask = new UpdateWidgetTimerTask();

	protected MediaPlayer _player = new MediaPlayer();
	protected PlayerBinder _binder = new PlayerBinder();
	protected DBAdapter _dbAdapter;
	protected Podcast _podcast;
	protected boolean _onPhone = false;
	protected boolean _pausedForPhone = false;
	private TelephonyManager _telephony;
	protected Timer _updateTimer = new Timer();

	public Podcast getActivePodcast() {
		return _podcast;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		handleStart(intent);
		return _binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		_dbAdapter = DBAdapter.getInstance(this);
		handleStart(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		_dbAdapter = DBAdapter.getInstance(this);
		handleStart(intent);
		return START_STICKY;
	}

	private void handleStart(Intent intent) {
		_player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		_player.setOnCompletionListener(new OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
			}
		});

		if (intent.getExtras() != null) {
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
		
		int podcastId = intent.getIntExtra("com.axelby.podax.PodcastId", -1);
		Podcast podcast;
		if (podcastId != -1)
			podcast = _dbAdapter.loadPodcast(podcastId);
		else
			podcast = _dbAdapter.loadLastPlayedPodcast();
		load(podcast);
		
		_telephony = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		_telephony.listen(new PhoneStateListener() {

			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				_onPhone = (state != TelephonyManager.CALL_STATE_IDLE);
				if (isPlaying() && _onPhone)
				{
					pause();
					_pausedForPhone = true;
				}
				if (!isPlaying() && !_onPhone && _pausedForPhone)
					play();
			}
			
		}, PhoneStateListener.LISTEN_CALL_STATE);
		_onPhone = (_telephony.getCallState() != TelephonyManager.CALL_STATE_IDLE);
	}

	public void load(Podcast podcast) {
		_podcast = podcast;
		
		if (_podcast == null)
			return;
		
		try {
			_player.reset();
			_player.setDataSource(_podcast.getFilename());
			_player.prepare();
			_player.seekTo(_podcast.getLastPosition());
		} catch (IOException e) {
			e.printStackTrace();
		}

		updateWidget();
	}

	public void pause() {
		_pausedForPhone = false;
		_player.pause();
		updateWidget();
	}

	public void play() {
		if (_onPhone)
			return;
		_pausedForPhone = false;
		_player.start();

		updateWidget();
		_updateWidgetTask.cancel();
		_updateWidgetTask = new UpdateWidgetTimerTask();
		_updateTimer.schedule(_updateWidgetTask, 250, 250);
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

	public void updateWidget() {
		RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget);
		views.setTextViewText(R.id.title, _podcast.getTitle());
		views.setTextViewText(R.id.position, PlayerService.getPositionString(getDuration(), getPosition()));
		int imageRes = isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
		views.setImageViewResource(R.id.play_btn, imageRes);
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
		appWidgetManager.updateAppWidget(new ComponentName(this, "com.axelby.podax.WidgetProvider"), views);
	}

	public String getPositionString() {
		if (this.getDuration() == 0) 
			return "";
		return PlayerActivity.getTimeString(this.getPosition()) + " / " + PlayerActivity.getTimeString(this.getDuration());
	}

	public static String getPositionString(int duration, int position) {
		if (duration == 0) 
			return "";
		return PlayerActivity.getTimeString(position) + " / " + PlayerActivity.getTimeString(duration);
	}
}
