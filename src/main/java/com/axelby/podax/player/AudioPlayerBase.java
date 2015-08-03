package com.axelby.podax.player;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public abstract class AudioPlayerBase implements Runnable {

	// returns seconds
	public static float determineDuration(String filename) {
		float duration = 0;
		android.media.MediaPlayer mp = new android.media.MediaPlayer();
		final CountDownLatch latch = new CountDownLatch(1);
		try {
			mp.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(android.media.MediaPlayer mp) {
					latch.countDown();
				}
			});
			mp.setDataSource(filename);
			mp.prepare();
			latch.await();
			duration = mp.getDuration() / 1000.0f;
		} catch (IOException | InterruptedException ignored) {
		}
		mp.release();
		return duration;
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
