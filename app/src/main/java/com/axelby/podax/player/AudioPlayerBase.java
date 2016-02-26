package com.axelby.podax.player;

import java.io.IOException;

public abstract class AudioPlayerBase implements Runnable {

	// returns seconds
	public static float determineDuration(String filename) {
		for (int att = 0; att < 3; att++) {
			android.media.MediaPlayer mp = new android.media.MediaPlayer();
			try {
				mp.setDataSource(filename);
				mp.prepare();
				return mp.getDuration() / 1000.0f;
			} catch (IOException ignored) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					return 0;
				}
			} finally {
				mp.release();
			}
		}
		return 0;
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
