package com.axelby.podax.player;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.support.annotation.NonNull;
import android.util.Log;

import com.axelby.podax.EpisodeCursor;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class MediaDecoderPlayer extends AudioPlayerBase {
	private final Context _context;
	private final EpisodeCursor _episode;
	private final MediaState _state;

	private AudioTrack _track;
	private float _seekbase = 0;
	private float _playbackRate = 1f;
	private boolean _isPlaying = false;

	// use this to change state in main loop
	private float _seekFlag;
	private boolean _stopFlag;

	public MediaDecoderPlayer(@NonNull Context context, @NonNull EpisodeCursor episode, float playbackRate) throws InterruptedException {
		_context = context;
		_episode = episode;
		_playbackRate = playbackRate;

		_state = new MediaState(_context, _episode);
	}

	private void createAudioTrack(int sampleRate, int numChannels) {
		if (_track != null) {
			_track.stop();
			_track.flush();
			_track.release();
		}

		_track = new AudioTrack(AudioManager.STREAM_MUSIC,
				sampleRate,
				numChannels == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				sampleRate * 2,
				AudioTrack.MODE_STREAM);
		_track.setPositionNotificationPeriod(sampleRate);
		_track.setPlaybackPositionUpdateListener(_playbackPositionListener);
		_track.play();
	}

	@Override public float getPosition() {
		if (_track != null)
			return _seekbase + _playbackRate * _track.getPlaybackHeadPosition() / _track.getSampleRate();
		return _state.extractor.getSampleTime() / 1000000.0f;
	}
	@Override public float getPlaybackRate() { return _playbackRate; }
	@Override public boolean isPlaying() { return _isPlaying; }

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


	@Override public void seekTo(float offsetInSeconds) { _seekFlag = offsetInSeconds; }
	@Override public void stop() { _stopFlag = true; }

	@Override
	public void run() {
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
		int bytesFed = 0;
		long lastPresentationUs = 0;

		SoundTouch soundtouch = new SoundTouch(_state.sampleRate, _state.numChannels, _playbackRate);
		short[] stretched = new short[1024 * 5];

		_isPlaying = true;

		try {
			while (!_stopFlag && !_state.isFinished()) {
				if (!_isPlaying) {
					Thread.sleep(50);
					continue;
				}

				if (_state.shouldRecycle()) {
					_state.regenerate();
					_state.extractor.seekTo(lastPresentationUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

					// does this need to be inside needsRecycle()?
					while (_state.extractor.getSampleTime() == -1) {
						// skipped past end?
						if (_state.wasCompleteFileLoaded())
							break;
						Thread.sleep(100);
						_state.regenerate();
						_state.extractor.seekTo(lastPresentationUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
					}
				}

				if (_seekFlag != -1f) {
					if (_track != null) {
						_track.pause();
						_track.flush();
					}
					_state.decoder.flush();

					lastPresentationUs = (long) (_seekFlag * 1000000);
					_state.extractor.seekTo(lastPresentationUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
					_seekbase = _state.extractor.getSampleTime() / 1000000.0f;
					_seekFlag = -1f;

					if (_state.extractor.getSampleTime() > -1) {
						if (_track != null) {
							_track.play();
							// not sure why this changes to 0 when flush() is called
							_track.setPositionNotificationPeriod(_track.getSampleRate());
						}
					} else if (_state.wasCompleteFileLoaded()) {
						// skipped past end
						break;
					} else {
						Thread.sleep(100);
						_state.recycleImmediately();
						continue;
					}
				}

				final int TIMEOUT_US = 10000;
				long byteMax = new File(_episode.getFilename(_context)).length();
				boolean readyForRead = _state.canRead() && byteMax >= bytesFed;

				// input buffers will fill up if decoding while paused
				if (readyForRead) {
					int inIndex = _state.decoder.dequeueInputBuffer(TIMEOUT_US);
					if (inIndex >= 0) {
						ByteBuffer buffer = _state.inputBuffers[inIndex];
						int sampleSize = _state.extractor.readSampleData(buffer, 0);

						if (sampleSize >= 0) {
							bytesFed += sampleSize;
							lastPresentationUs = _state.extractor.getSampleTime();
							_state.decoder.queueInputBuffer(inIndex, 0, sampleSize, lastPresentationUs, 0);
							_state.extractor.advance();
						} else {
							_state.decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
							// if file is finished streaming, input is complete
							if (_state.wasCompleteFileLoaded()) {
								_state.inEOS();
							} else {
								_state.recycle();
							}
						}
					}
				}

				int outIndex = _state.decoder.dequeueOutputBuffer(info, TIMEOUT_US);
				// track should not be null here
				// assuming mediadecoder sets new output format before spewing audio data
				if (outIndex > 0 && _track != null) {
					ByteBuffer buf = _state.outputBuffers[outIndex];
					ShortBuffer sbuf = buf.asShortBuffer();
					final short[] chunk = new short[info.size / 2];
					sbuf.get(chunk);
					sbuf.clear();

					int outSamples = soundtouch.stretch(chunk, chunk.length, stretched, stretched.length, _state.numChannels);
					_track.write(stretched, 0, outSamples);

					_state.decoder.releaseOutputBuffer(outIndex, false);
				} else if (outIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
					_state.outputBuffers = _state.decoder.getOutputBuffers();
				} else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					MediaFormat newFormat = _state.decoder.getOutputFormat();

					int sampleRateInHz = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
					int numChannels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
					createAudioTrack(sampleRateInHz, numChannels);
				}

				if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					Log.d("mediadecoder", "outEOS");
					_state.outEOS();
				}
			}

			if (_state.isFinished() && _completionListener != null)
				_completionListener.onCompletion();
		} catch (InterruptedException e1) {
			_state.interrupt();
			Log.e("mediadecoder", "interrupted", e1);
		} catch (Exception e) {
			Log.e("mediadecoder", "catchall", e);
		} finally {
			_state.release();
			_episode.closeCursor();

			if (_track != null) {
				try {
					_track.stop();
					while (_track.getPlaybackHeadPosition() != 0)
						Thread.sleep(10);
				} catch (InterruptedException e) {
					Log.e("mp3decoders", "InterruptedException", e);
				}
				_track.release();
				_track = null;
			}
		}
	}

	final AudioTrack.OnPlaybackPositionUpdateListener _playbackPositionListener =
			new AudioTrack.OnPlaybackPositionUpdateListener() {
		@Override
		public void onMarkerReached(AudioTrack audioTrack) { }

		@Override
		public void onPeriodicNotification(AudioTrack audioTrack) {
			if (!isPlaying())
				return;
			if (audioTrack == null || audioTrack.getPlayState() == AudioTrack.STATE_UNINITIALIZED)
				return;
			if (_periodicListener == null)
				return;
			// sometimes this causes exception (possibly when stopped shortly after starting)
			// java.lang.IllegalStateException: Unable to retrieve AudioTrack pointer for getPosition()
			try {
				_periodicListener.pulse(_seekbase + _playbackRate * audioTrack.getPlaybackHeadPosition() / audioTrack.getSampleRate());
			} catch (Exception ignored) { }
		}
	};

	// TODO: make recycle and regenerate verbiage consistant
	static class MediaState {
		private final Context _context;
		private final EpisodeCursor _episode;
		private final String _inputFilename;

		public MediaExtractor extractor;
		public MediaFormat format;
		public int sampleRate;
		public int numChannels;
		public MediaCodec decoder;
		public ByteBuffer[] inputBuffers;
		public ByteBuffer[] outputBuffers;
		public boolean completeFileLoaded;

		private boolean _finished = false;
		private boolean _recycling = false;
		private boolean _inEOS = false;
		private boolean _outEOS = false;

		public MediaState(Context context, EpisodeCursor episode) throws InterruptedException {
			_context = context;
			_episode = episode;
			_inputFilename = episode.getFilename(context);
			reset();
		}

		private void reset() throws InterruptedException {
			_finished = false;
			_recycling = false;
			_inEOS = false;
			_outEOS = false;

			completeFileLoaded = _episode.isDownloaded(_context);
			extractor = createMediaExtractor(_inputFilename);
			if (extractor == null)
				return;

			format = extractor.getTrackFormat(0);
			sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
			numChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

			decoder = createMediaCodec(format);
			if (decoder == null)
				return;

			inputBuffers = decoder.getInputBuffers();
			outputBuffers = decoder.getOutputBuffers();
		}

		public void regenerate() throws InterruptedException {
			reset();
		}

		private MediaExtractor createMediaExtractor(String inputFilename) throws InterruptedException {
			MediaExtractor extractor = new MediaExtractor();
			boolean dataSourceSet = false;
			while (!dataSourceSet) {
				try {
					extractor.setDataSource(inputFilename);
					dataSourceSet = true;
				} catch (IOException e) {
					Thread.sleep(100);
				}
			}
			extractor.selectTrack(0);
			return extractor;
		}

		private MediaCodec createMediaCodec(MediaFormat format) {
			MediaCodec decoder;

			try {
				String mime = format.getString(MediaFormat.KEY_MIME);
				decoder = MediaCodec.createDecoderByType(mime);
			} catch (IOException e) {
				Log.e("mp3decoders", "error creating media decoder", e);
				return null;
			}

			decoder.configure(format, null, null, 0);
			decoder.start();
			return decoder;
		}

		public void release() {
			extractor.release();
			decoder.stop();
			decoder.release();
		}

		public boolean wasCompleteFileLoaded() { return completeFileLoaded; }

		public void interrupt() { _finished = true; }
		public void recycle() { _recycling = true; }
		public void recycleImmediately() { _recycling = true; _outEOS = true; }

		public boolean isFinished() { return _finished; }
		public boolean shouldRecycle() { return _recycling && _outEOS; }
		public boolean canRead() { return !_inEOS && !_recycling; }

		public void inEOS() {
			_inEOS = true;
			if (_outEOS)
				_finished = true;
		}

		public void outEOS() {
			_outEOS = true;
			if (_inEOS)
				_finished = true;
		}
	}

}
