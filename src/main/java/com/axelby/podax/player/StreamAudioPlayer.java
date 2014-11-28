package com.axelby.podax.player;

import android.os.FileObserver;
import android.util.Log;

import com.axelby.podax.EpisodeCursor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamAudioPlayer extends AudioPlayer {
	private final Thread _watcher;
	private final String _filename;
	private final MPG123Stream _audioStream;

	private boolean _fileClosed = false;
	private FileObserver _observer = null;

	public StreamAudioPlayer(String filename, float positionInSeconds, float playbackRate) {
		super(playbackRate);
		_filename = filename;
		_decoder = _audioStream = new MPG123Stream();
		_watcher = new Thread(_watchFile, "StreamAudioPlayer-filewatcher");
		_watcher.start();
	}

	// stream from the file to the audio stream
	// handle cases when file is completely downloaded, partially downloaded, and not created yet
	private final Runnable _watchFile = new Runnable() {
		@Override public void run() {
			try {
				InputStream stream = null;
				File streamingFile = new File(_filename);

				// read everything we can from the file
				if (streamingFile.exists()) {
					stream = new BufferedInputStream(new FileInputStream(_filename));
					int available;
					while ((available = stream.available()) > 0) {
						byte[] b = new byte[available];
						stream.read(b);
						_audioStream.feed(b);
					}
				}

				// wait for the android downloader to create the file
				while (!streamingFile.exists())
					Thread.sleep(100);
				Thread.sleep(100);
Log.d("Podax", "watched streaming file exists");

				// alternatively, send the context here and use the content resolver
				String externalPath = EpisodeCursor.extractExternalStorageDirectory(_filename);
				long id = EpisodeCursor.extractIdFromFilename(_filename);
				String downloadingIndicatorFile = EpisodeCursor.getDownloadingIndicatorFilename(externalPath, id);

				// if the downloading indicator file doesn't exist by now, we're not downloading it
				if (!new File(downloadingIndicatorFile).exists())
					return;

				_observer = new FileObserver(downloadingIndicatorFile, FileObserver.DELETE_SELF) {
					@Override
					public void onEvent(int event, String path) {
Log.d("Podax", "watched streaming file closed");
						_fileClosed = true;
					}
				};
				_observer.startWatching();

				if (stream == null)
					stream = new BufferedInputStream(new FileInputStream(_filename));

				int read;
				byte[] c = new byte[1024*100]; // default bufferedinputstream buffer size
				while (true) {
					int available = stream.available();
					if (_fileClosed && available == 0)
						break;
					if (available == 0) {
						Thread.sleep(100);
						continue;
					}

//Log.d("Podax", "feeding data from watched file");
					read = stream.read(c);

					// shouldn't happen because data is available, but handle it gracefully anyway
					if (read == -1)
						continue;
					if (read == c.length)
						_audioStream.feed(c);
					else {
						byte[] d = new byte[read];
						System.arraycopy(c, 0, d, 0, read);
						_audioStream.feed(d);
					}
				}
			} catch (IOException e) {
				Log.d("Podax", "IOException while watching streaming file", e);
			} catch (InterruptedException ignored) {
Log.d("Podax", "interrupted - stopped reading");
			}
Log.d("Podax", "done watching file");
		}
	};

	@Override
	public void release() {
		super.release();

		try {
			if (_observer != null)
				_observer.stopWatching();
			_watcher.interrupt();
			_watcher.join();
		} catch (InterruptedException ignored) { }
	}
}
