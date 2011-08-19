package com.axelby.podax;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.Intent;

public class PodaxApp extends Application {
	static private PodaxApp _instance;

	@Override
	public void onCreate() {
		super.onCreate();

		_instance = this;

		Intent intent = new Intent(this, UpdateService.class);
		intent.setAction("com.axelby.podax.STARTUP");
		startService(intent);
		
		PlayerService.updateWidget(this, getActivePodcast(), isPlaying());
	}

	public static PodaxApp getApp() {
		return _instance;
	}

	public Podcast getActivePodcast() {
		return DBAdapter.getInstance(this).loadLastPlayedPodcast();
	}
	public boolean isPlaying() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
	        if (PlayerService.class.getName().equals(service.service.getClassName()))
	            return true;
	    return false;
	}
	
	public int getPosition() {
		return getActivePodcast().getLastPosition();
	}

	public int getDuration() {
		return getActivePodcast().getDuration();
	}

	public void play() {
		sendPlayerCommand(Constants.PLAYER_COMMAND_PLAY);
	}

	public void pause() {
		sendPlayerCommand(Constants.PLAYER_COMMAND_PAUSE);
	}

	public void playpause() {
		sendPlayerCommand(Constants.PLAYER_COMMAND_PLAYPAUSE);
	}

	public void skipForward() {
		sendPlayerCommand(Constants.PLAYER_COMMAND_SKIPFORWARD);
	}

	public void skipBack() {
		sendPlayerCommand(Constants.PLAYER_COMMAND_SKIPBACK);
	}

	public void restart() {
		sendPlayerCommand(Constants.PLAYER_COMMAND_RESTART);
	}
	
	public void skipToEnd() {
		sendPlayerCommand(Constants.PLAYER_COMMAND_SKIPTOEND);
	}
	
	public void skipTo(int secs) {
		sendPlayerCommand(Constants.PLAYER_COMMAND_SKIPTO, secs);
	}

	private void sendPlayerCommand(int command, int arg) {
		Intent intent = new Intent(this, PlayerService.class);
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, arg);
		startService(intent);
	}

	private void sendPlayerCommand(int command) {
		Intent intent = new Intent(this, PlayerService.class);
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
		startService(intent);
	}

	public void play(Podcast podcast) {
		if (podcast == null)
			return;
		sendPlayerCommand(Constants.PLAYER_COMMAND_PLAY_SPECIFIC_PODCAST, podcast.getId());
	}
}
