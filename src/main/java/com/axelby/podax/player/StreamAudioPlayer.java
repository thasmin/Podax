package com.axelby.podax.player;

import com.axelby.podax.UpdateService;

public class StreamAudioPlayer extends AudioPlayer {
	private final String _filename;

	private StreamFeeder _feeder;

	private IMediaDecoder _rabbitDecoder;
	private StreamSkipper _skipper;
	private StreamFeeder _rabbitFeeder;

	public StreamAudioPlayer(String filename, float playbackRate) {
		super(playbackRate);
		_filename = filename;

		_rabbitDecoder = _decoder = new MPG123();
		_rabbitFeeder = _feeder = new StreamFeeder(_filename, _decoder);
	}

	@Override protected void changeTrackOffset(float offsetInSeconds) {
		if (_decoder == null)
			return;

		try {
			closeAudioTrack(_track);
			_track = null;

			// keep feeding until the offset is available
			long fileOffset = findSeekFileOffset(_rabbitDecoder, offsetInSeconds);

			// switch the rabbit decoder to skip all frames to get future seek offsets
			if (_decoder == _rabbitDecoder)
				_skipper = new StreamSkipper(_rabbitDecoder);

			// start a new feeder at the offset
			_seekbase = offsetInSeconds;
			_decoder = new MPG123();
			_feeder = new StreamFeeder(_filename, _decoder, fileOffset);

			// create new track
			_track = createTrackFromDecoder(_decoder, _playbackPositionListener);
			while (_track == null) {
				Thread.sleep(50);
				_track = createTrackFromDecoder(_decoder, _playbackPositionListener);
			}
			_track.play();
		} catch (InterruptedException ignored) {
			// assume that this will be released by its interruptor
		}
	}

	private static long findSeekFileOffset(IMediaDecoder decoder, float seekToSeconds) throws InterruptedException {
		long fileOffset = decoder.getSeekFrameOffset(seekToSeconds);
		// keep trying to skip frame until offset is found or out of data
		while (fileOffset == -1) {
			boolean skippedFrame = decoder.skipFrame();
			if (skippedFrame) {
				fileOffset = decoder.getSeekFrameOffset(seekToSeconds);
				continue;
			}

			// check for out of data
			if (decoder.isStreamComplete())
				break;
			// sleep to wait for more data
			Thread.sleep(50);
		}
		return fileOffset;
	}

	@Override
	public void release() {
		if (_skipper != null)
			_skipper.close();
		if (_rabbitFeeder != null && _rabbitFeeder != _feeder)
			_rabbitFeeder.finish();
		if (_rabbitDecoder != null && _rabbitDecoder != _decoder)
			_rabbitDecoder.close();

		super.release();
	}
}
