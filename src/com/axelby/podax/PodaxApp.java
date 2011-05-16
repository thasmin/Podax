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
	
	public class PlayerConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName name, IBinder service) {
			_player = ((PlayerService.PlayerBinder)service).getService();
			synchronized (_playerConnection) {
				_playerConnection.notify();
			}
		}
		public void onServiceDisconnected(ComponentName name) {
			_player = null;
		}		
	}
	protected PlayerConnection _playerConnection = new PlayerConnection();

	@Override
	public void onCreate() {
		_instance = this;
		_activePodcast = DBAdapter.getInstance(this).loadLastPlayedPodcast();

		super.onCreate();

		startService(new Intent(this, UpdateService.class));
	}

	public static PodaxApp getApp() {
		return _instance;
	}

	public Podcast getActivePodcast() {
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
		if (_player != null)
			_player.play();
	}
	public int getPosition() {
		if (_player != null)
			return _player.getPosition();
		return -1;
	}
	public int getDuration() {
		if (_player != null)
			return _player.getDuration();
		return -1;
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
		startPlayerService();
		_player.load(_activePodcast);
		_player.play();
	}

	private void startPlayerService() {
		Intent intent = new Intent(this, PlayerService.class);
		startService(intent);
		bindService(intent, _playerConnection, 0);
		try {
			synchronized(_playerConnection) {
				_playerConnection.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
