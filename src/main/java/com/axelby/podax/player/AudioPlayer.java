package com.axelby.podax.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioPlayer implements Runnable {
	IMediaDecoder _decoder;
	AudioTrack _track;

	// avoid tying up main thread by having thread check these variables to make changes
	boolean _stopping = false;
	Float _seekTo = null;

	private boolean _isPlaying = false;
	private float _seekbase = 0;
	private float _playbackRate = 1f;

	public AudioPlayer(String audioFile, float positionInSeconds, float playbackRate) {
		_playbackRate = playbackRate;
		_decoder = loadFile(audioFile);
		if (_decoder == null)
			throw new IllegalArgumentException("audioFile must be .mp3, .ogg, or .oga");
		if (positionInSeconds != 0) {
			_seekbase = positionInSeconds;
			_decoder.seek(positionInSeconds);
		}
		_track = createTrackFromDecoder(_decoder);
		_track.setPlaybackPositionUpdateListener(_playbackPositionListener);
	}

	public static boolean supports(String audioFile) {
		int lastDot = audioFile.lastIndexOf('.');
		if (lastDot <= 0)
			return false;
		String extension = audioFile.substring(lastDot + 1);
		return extension.equals("mp3") || extension.equals("ogg") || extension.equals("oga");
	}

	public static IMediaDecoder loadFile(String audioFile) {
		int lastDot = audioFile.lastIndexOf('.');
		if (lastDot <= 0)
			throw new IllegalArgumentException("audioFile must be .mp3, .ogg, or .oga");

		String extension = audioFile.substring(lastDot + 1);
		if (extension.equals("mp3"))
			return new MPG123(audioFile);
		else if (extension.equals("ogg") || extension.equals("oga"))
			return new Vorbis(audioFile);
		return null;
	}

	private static AudioTrack createTrackFromDecoder(IMediaDecoder decoder) {
		AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC,
				decoder.getRate(),
				decoder.getNumChannels() == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				decoder.getRate() * 2,
				AudioTrack.MODE_STREAM);
		track.setPositionNotificationPeriod(decoder.getRate());
		return track;
	}

	public float getPlaybackRate() {
		return _playbackRate;
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
		public void onMarkerReached(AudioTrack audioTrack) { }

		@Override
		public void onPeriodicNotification(AudioTrack audioTrack) {
			if (!_isPlaying)
				return;
			try {
				if (audioTrack != null && _periodicListener != null)
					_periodicListener.pulse(_seekbase + _playbackRate * audioTrack.getPlaybackHeadPosition() / audioTrack.getSampleRate());
			} catch (Exception e) {
				Log.e("Podax", "exception during periodic notification", e);
			}
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
		_seekTo = offsetInSeconds;
	}

	public void stop() {
		_stopping = true;
	}

	public float getDuration() {
		if (_decoder == null)
			return 0;
		return _decoder.getDuration();
	}

	public float getPosition() {
		// not sure why track is null but it's happening
		if (_track == null)
			return _seekbase;
		if (_decoder == null)
			return 0;
		return _seekbase + _playbackRate * _track.getPlaybackHeadPosition() / _track.getSampleRate();
	}

	private void changeTrackOffset(float offsetInSeconds) {
		if (_decoder == null)
			return;

		// close the current AudioTrack
		_track.pause();
		_track.flush();

		_seekbase = offsetInSeconds;
		_decoder.seek(offsetInSeconds);

		AudioTrack toRelease = _track;
		_track = createTrackFromDecoder(_decoder);
		_track.setPlaybackPositionUpdateListener(_playbackPositionListener);
		_track.play();
		toRelease.release();
	}

	@Override
	public void run() {
		_track.play();
		_isPlaying = true;

		WSOLA wsola = new WSOLA();
		wsola.init();
		WSOLA.Error wsolaError = new WSOLA.Error();

		try {
			short[] pcm = new short[1024 * 5];
			do {
				if (_seekTo != null) {
					changeTrackOffset(_seekTo);
					_seekTo = null;
				}
				if (_stopping)
					return;
				if (!_isPlaying) {
					Thread.sleep(50);
					continue;
				}
				int sampleCount = _decoder.readSamples(pcm, 0, pcm.length);
				if (sampleCount == 0)
					break;
				short[] wsolapcm = wsola.stretch(pcm, _decoder.getRate(), _decoder.getNumChannels() == 2, _playbackRate, 1, wsolaError);
				if (wsolaError.code == WSOLA.Error.SUCCESS)
					_track.write(wsolapcm, 0, wsolapcm.length);
			} while (_track != null);

			waitAndCloseTrack();

			if (_completionListener != null)
				_completionListener.onCompletion();
		} catch (InterruptedException e) {
			Log.e("Podax", "InterruptedException", e);
		} catch (IllegalStateException e) {
			Log.e("Podax", "IllegalStateException", e);
		} finally {
			_decoder.close();
			_decoder = null;

			wsola.close();

			// close track without waiting
			if (_track != null) {
				_track.pause();
				// store stop point in case something asks for position
				_seekbase = _seekbase + _playbackRate * _track.getPlaybackHeadPosition() / _track.getSampleRate();
				_track.flush();
				_track.release();
				_track = null;
			}
		}
	}

	private void waitAndCloseTrack() {
		if (_track == null)
			return;

		try {
			_track.stop();
			while (_track.getPlaybackHeadPosition() != 0)
				Thread.sleep(10);
		} catch (InterruptedException e) {
			Log.e("Podax", "InterruptedException", e);
		}

		// store stop point in case something asks for position
		_seekbase = _seekbase + _playbackRate * _track.getPlaybackHeadPosition() / _track.getSampleRate();

		_track.release();
		_track = null;
	}
}
