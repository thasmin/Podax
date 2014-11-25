package com.axelby.podax.player;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamAudioPlayer extends AudioPlayer {
	private final Thread _watcher;
	private final String _filename;
	private final MPG123Stream _stream;
	private boolean _stopReading = false;

	public StreamAudioPlayer(String filename, float positionInSeconds, float playbackRate) {
		super(playbackRate);
		_filename = filename;
		_decoder = _stream = new MPG123Stream();
		_watcher = new Thread(_watchFile, "StreamAudioPlayer-filewatcher");
		_watcher.start();
	}

	private final Runnable _watchFile = new Runnable() {
		@Override public void run() {
			try {
				InputStream stream = new BufferedInputStream(new FileInputStream(_filename));

				int read;
				byte[] c = new byte[1000];
				while (!_stopReading && (read = stream.read(c)) != -1) {
					if (read == 1000)
						_stream.feed(c);
					else {
						byte[] d = new byte[read];
						System.arraycopy(c, 0, d, 0, read);
						_stream.feed(d);
					}
				}
			} catch (IOException ignored) { }
		}
	};

	@Override
	public void release() {
		super.release();

		try {
			_stopReading = true;
			_watcher.join();
		} catch (InterruptedException ignored) { }
	}
}
