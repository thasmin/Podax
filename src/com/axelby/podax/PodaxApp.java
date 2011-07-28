package com.axelby.podax;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class PodaxApp extends Application {
	static private PodaxApp _instance;
	private Podcast _activePodcast;
	private PlayerService _player;
	
	protected ServiceConnection _playerConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			_player = ((PlayerService.PlayerBinder)service).getService();
		}
		public void onServiceDisconnected(ComponentName name) {
			_player = null;
		}		
	};

	@Override
	public void onCreate() {
		super.onCreate();

		_instance = this;
		_activePodcast = DBAdapter.getInstance(this).loadLastPlayedPodcast();

		Intent intent = new Intent(this, UpdateService.class);
		intent.setAction("com.axelby.podax.STARTUP");
		startService(intent);
	}

	public static PodaxApp getApp() {
		return _instance;
	}

	public Podcast getActivePodcast() {
		if (_player != null)
			_activePodcast = _player.getActivePodcast();
		return _activePodcast;
	}
	public boolean isPlaying() {
		return _player != null && _player.isPlaying();
	}
	public void pause() {
		if (_player != null)
			_player.pause();
	}
	public void play() {
		if (_player == null)
			startPlayerService(null);
		else
			_player.play();
	}
	public int getPosition() {
		if (_activePodcast != null)
			return _activePodcast.getLastPosition();
		return -1;
	}
	public int getDuration() {
		if (_activePodcast.getDuration() == 0)
			_activePodcast = DBAdapter.getInstance(this).loadPodcast(_activePodcast.getId());
		return _activePodcast.getDuration();
	}
	public void skip(int secs) {
		if (_player != null)
			_player.skip(secs);
	}
	public void restart() {
		if (_player != null)
			_player.restart();
	}
	public void skipToEnd() {
		if (_player != null)
			_player.skipToEnd();
	}
	
	public void playPodcast(Podcast podcast) {
		_activePodcast = podcast;
		startPlayerService(_activePodcast);
	}

	private void startPlayerService(Podcast podcast) {
		Intent intent = new Intent(this, PlayerService.class);
		if (podcast != null)
			intent.putExtra("com.axelby.podax.podcast", podcast.getId());
		else
			intent.putExtra("com.axelby.podax.podcast", -1);
		startService(intent);
		intent.removeExtra("com.axelby.podax.podcast");
		bindService(intent, _playerConnection, BIND_AUTO_CREATE);
	}
}
