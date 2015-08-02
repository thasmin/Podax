package com.axelby.podax.player;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class MediaPlayer extends AudioPlayerBase {
	private final String _audioFile;
	private final android.media.MediaPlayer _mediaPlayer;

	private boolean _ready = false;
	private int _seekTo = -1;

	public MediaPlayer(String audioFile) {
		_audioFile = audioFile;
		_mediaPlayer = new android.media.MediaPlayer();
		_mediaPlayer.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(android.media.MediaPlayer mediaPlayer) {
				if (_completionListener != null)
					_completionListener.onCompletion();
			}
		});
	}

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

	@Override
	public float getPosition() {
		if (!_ready)
			return 0.0f;
		return _mediaPlayer.getCurrentPosition() / 1000.0f;
	}

	@Override
	public float getPlaybackRate() { return 1.0f; }

	@Override
	public boolean isPlaying() {
		return _ready && _mediaPlayer.isPlaying();
	}

	@Override
	public void pause() {
		if (_ready)
			_mediaPlayer.pause();
	}

	@Override
	public void resume() {
		if (_ready)
			_mediaPlayer.start();
	}

	@Override
	public void seekTo(float offsetInSeconds) {
		if (_ready)
			_mediaPlayer.seekTo((int) (offsetInSeconds * 1000));
		else
			_seekTo = (int) (offsetInSeconds * 1000);
	}

	@Override
	public void stop() {
		if (!_ready)
			return;
		_ready = false;
		_mediaPlayer.pause();
		_mediaPlayer.release();
	}

	@Override
	public void run() {
		try {
			_mediaPlayer.setDataSource(_audioFile);
			_mediaPlayer.prepare();
		} catch (IOException e) {
			Log.e("MediaPlayer", "unable to setDataSource and prepare", e);
			return;
		}

		_ready = true;
		if (_seekTo != -1)
			_mediaPlayer.seekTo(_seekTo);
		resume();

		while (_ready) {
			if (_mediaPlayer.isPlaying() && _periodicListener != null) {
				int position = Math.min(_mediaPlayer.getCurrentPosition(), _mediaPlayer.getDuration());
				_periodicListener.pulse(position / 1000.0f);
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException ignored) {
				break;
			}
		}

		stop();
	}
}
