package com.axelby.podax.player;

public abstract class AudioPlayerBase implements Runnable {

	// returns seconds
	public static float determineDuration(String filename) {
		if (MP3Player.supports(filename))
			return MPG123.determineDuration(filename);
		else
			return MediaPlayer.determineDuration(filename);
	}

	public interface OnCompletionListener { void onCompletion(); }
	protected OnCompletionListener _completionListener = null;
	public void setOnCompletionListener(OnCompletionListener completionListener) {
		this._completionListener = completionListener;
	}

	public interface PeriodicListener { void pulse(float position); }
	protected PeriodicListener _periodicListener = null;
	public void setPeriodicListener(PeriodicListener periodicListener) {
		this._periodicListener = periodicListener;
	}

	public abstract float getPosition();
	public abstract float getPlaybackRate();
	public abstract boolean isPlaying();

	public abstract void pause();
	public abstract void resume();
	public abstract void seekTo(float offsetInSeconds);
	public abstract void stop();
}
