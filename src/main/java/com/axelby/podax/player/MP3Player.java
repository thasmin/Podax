package com.axelby.podax.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class MP3Player extends AudioPlayerBase {
	MPG123 _decoder;
	AudioTrack _track;

	// avoid tying up main thread by having thread check these variables to make changes
	private boolean _stopping = false;
	private Float _seekToSeconds = null;

	private boolean _isPlaying = false;
	float _seekbase = 0;
	private float _playbackRate = 1f;

	MP3Player(float playbackRate) {
		_playbackRate = playbackRate;
	}
	public MP3Player(String audioFile, float playbackRate) {
		this(playbackRate);
		_decoder = loadFile(audioFile);
		if (_decoder == null)
			throw new IllegalArgumentException("audioFile must be .mp3, .ogg, or .oga");
		_track = createTrackFromDecoder(_decoder, _playbackPositionListener);
	}

	public static boolean supports(String audioFile) {
		int lastDot = audioFile.lastIndexOf('.');
		if (lastDot <= 0)
			return false;
		String extension = audioFile.substring(lastDot + 1);
		return extension.equals("mp3");
	}

	public static MPG123 loadFile(String audioFile) {
		int lastDot = audioFile.lastIndexOf('.');
		if (lastDot <= 0)
			throw new IllegalArgumentException("audioFile must be .mp3");

		String extension = audioFile.substring(lastDot + 1);
		if (extension.equals("mp3"))
			return new MPG123(audioFile);
		return null;
	}

	static AudioTrack createTrackFromDecoder(MPG123 decoder, AudioTrack.OnPlaybackPositionUpdateListener playbackPositionListener) {
		// streaming decoder will return rate as 0 if not enough data has been loaded
		if (decoder.getRate() == 0)
			return null;

		AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC,
				decoder.getRate(),
				decoder.getNumChannels() == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				decoder.getRate() * 2,
				AudioTrack.MODE_STREAM);
		track.setPositionNotificationPeriod(decoder.getRate());
		track.setPlaybackPositionUpdateListener(playbackPositionListener);
		return track;
	}

	@Override
	public float getPlaybackRate() {
		return _playbackRate;
	}

	final AudioTrack.OnPlaybackPositionUpdateListener _playbackPositionListener =
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

	@Override
	public boolean isPlaying() {
		return _isPlaying;
	}

	@Override
	public void pause() {
		if (_track == null)
			return;
		_track.pause();
		_isPlaying = false;
	}

	@Override
	public void resume() {
		if (_track == null)
			return;
		_track.play();
		_isPlaying = true;
	}

	@Override
	public void seekTo(float offsetInSeconds) { _seekToSeconds = offsetInSeconds; }
	@Override
	public void stop() { _stopping = true; }

	@Override
	public float getPosition() {
		if (_track == null)
			return _seekbase;
		if (_decoder == null)
			return 0;
		return _seekbase + _playbackRate * _track.getPlaybackHeadPosition() / _track.getSampleRate();
	}

	void changeTrackOffset(float offsetInSeconds) {
		if (_decoder == null)
			return;

		closeAudioTrack(_track);
		_track = null;

		_seekbase = offsetInSeconds;
		_decoder.seek(offsetInSeconds);

		// create new track
		_track = createTrackFromDecoder(_decoder, _playbackPositionListener);
		if (_track != null)
			_track.play();
	}

	void closeAudioTrack(AudioTrack track) {
		// close the current AudioTrack
		if (track != null) {
			track.pause();
			track.flush();
			track.release();
		}
	}

	@Override
	public void run() {
		try {
			while (_track == null) {
				Thread.sleep(50);
				_track = createTrackFromDecoder(_decoder, _playbackPositionListener);
				if (_track != null)
					break;
			}
		} catch (InterruptedException e) {
			Log.d("Podax", "interrupted while creating track", e);
			return;
		}

		_track.play();
		_isPlaying = true;

		SoundTouch soundtouch = new SoundTouch(_decoder.getRate(), _decoder.getNumChannels(), _playbackRate);

		try {
			short[] pcm = new short[1024 * 5];
			short[] stretched = new short[1024 * 5];
			do {
				if (_seekToSeconds != null) {
					changeTrackOffset(_seekToSeconds);
					_seekToSeconds = null;
				}
				if (_stopping)
					return;
				if (!_isPlaying) {
					Thread.sleep(50);
					continue;
				}
				int sampleCount = _decoder.readFrame(pcm);
				// handle need more data
				if (sampleCount == -1 && _decoder.isStreamComplete())
					break;
				if (sampleCount == -1) {
					Thread.sleep(50);
					continue;
				}
				if (sampleCount == 0)
					break;
				int outSamples = soundtouch.stretch(pcm, sampleCount, stretched, stretched.length, _decoder.getNumChannels());
				_track.write(stretched, 0, outSamples);
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

			soundtouch.close();

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

	void waitAndCloseTrack() {
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

		release();
	}

	void release() {
	}
}
