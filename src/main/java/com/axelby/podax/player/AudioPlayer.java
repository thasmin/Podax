package com.axelby.podax.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioPlayer implements Runnable {
	IMediaDecoder _decoder;
	AudioTrack _track;

	boolean _isPlaying = false;
	private float _seekbase = 0;

	public AudioPlayer(String audioFile, float positionInSeconds, float playbackRate) {
		loadFile(audioFile);
		if (positionInSeconds != 0)
			seekTo(positionInSeconds);
		createTrackFromDecoder(playbackRate);
	}

	public void loadFile(String audioFile) {
		int lastDot = audioFile.lastIndexOf('.');
		if (lastDot <= 0)
			throw new IllegalArgumentException("audioFile must be .mp3, .ogg, or .oga");

		String extension = audioFile.substring(lastDot + 1);
		if (extension.equals("mp3"))
			_decoder = new MPG123(audioFile);
		else if (extension.equals("ogg") || extension.equals("oga"))
			_decoder = new Vorbis(audioFile);
		else
			throw new IllegalArgumentException("audioFile must be .mp3, .ogg, or .oga");
	}

	private void createTrackFromDecoder(float playbackRate) {
		_track = new AudioTrack(AudioManager.STREAM_MUSIC,
				_decoder.getRate(),
				_decoder.getNumChannels() == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				_decoder.getRate() * 2,
				AudioTrack.MODE_STREAM);
		_track.setPlaybackRate((int) (_decoder.getRate() * playbackRate));
		_track.setPositionNotificationPeriod(_decoder.getRate());
		_track.setPlaybackPositionUpdateListener(_playbackPositionListener);
	}

	public static interface OnCompletionListener { public void onCompletion(); }
	private OnCompletionListener _completionListener = null;
	public void setOnCompletionListener(OnCompletionListener completionListener) {
		this._completionListener = completionListener;
	}

	public static interface PeriodicListener { public void pulse(float position); }
	private PeriodicListener _periodicListener = null;
	public void setPeriodicListener(PeriodicListener periodicListener) {
		this._periodicListener = periodicListener;
	}

	private AudioTrack.OnPlaybackPositionUpdateListener _playbackPositionListener =
			new AudioTrack.OnPlaybackPositionUpdateListener() {
		@Override
		public void onMarkerReached(AudioTrack audioTrack) {
			if (_completionListener != null)
				_completionListener.onCompletion();
		}

		@Override
		public void onPeriodicNotification(AudioTrack audioTrack) {
			if (audioTrack != null && _periodicListener != null)
				_periodicListener.pulse(_seekbase + (float) audioTrack.getPlaybackHeadPosition() / audioTrack.getSampleRate());
		}
	};

	public boolean isPlaying() {
		return _isPlaying;
	}

	public void pause() {
		if (_track == null)
			return;
		_track.pause();
		_isPlaying = false;
	}

	public void resume() {
		if (_track == null)
			return;
		_track.play();
		_isPlaying = true;
	}

	public void seekTo(float offsetInSeconds) {
		if (_decoder == null)
			return;
		_seekbase = offsetInSeconds;
		_decoder.seek(offsetInSeconds);
		if (_track == null)
			return;
		_track.pause();
		_track.flush();
		_track.play();
	}

	public void stop() {
		if (_track == null)
			return;
		_isPlaying = false;
		_track.pause();
		_track.flush();
		_track.stop();
	}

	public float getDuration() {
		if (_decoder == null)
			return 0;
		return _decoder.getDuration();
	}

	public float getPosition() {
		if (_decoder == null)
			return 0;
		return _seekbase + (float) _track.getPlaybackHeadPosition() / _track.getSampleRate();
	}

	@Override
	public void run() {
		_track.play();
		_isPlaying = true;

		try {
			short[] pcm = new short[1024 * 5];
			do {
				if (_track.getPlayState() == AudioTrack.PLAYSTATE_STOPPED)
					return;
				if (!_isPlaying) {
					Thread.sleep(50);
					continue;
				}
				if (_decoder.readSamples(pcm, 0, pcm.length) == 0)
					return;
				_track.write(pcm, 0, pcm.length);
			} while (_track != null);
		} catch (InterruptedException e) {
			Log.e("Podax", "InterruptedException", e);
		/* uncomment when routine catches are debugged
		} catch (IllegalStateException e) {
			Log.e("Podax", "IllegalStateException", e);
		*/
		} finally {
			waitAndCloseTrack();
		}
	}

	private void waitAndCloseTrack() {
		if (_decoder != null) {
			_decoder.close();
			_decoder = null;
		}

		if (_track == null)
			return;

		try {
			_track.stop();
			while (_track.getPlaybackHeadPosition() != 0)
				Thread.sleep(10);
		} catch (InterruptedException e) {
			Log.e("Podax", "InterruptedException", e);
		}

		_track.release();
		_track = null;
	}
}
