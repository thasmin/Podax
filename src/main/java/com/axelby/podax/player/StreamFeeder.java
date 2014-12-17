package com.axelby.podax.player;

import android.os.FileObserver;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * pipes data from a file to the decoder
 */
public class StreamFeeder {
	final String _filename;
	final IMediaDecoder _decoder;

	Thread _feederThread = null;
	long _fileOffset = 0;

	private boolean _doneDownloading = false;
	private FileObserver _observer = null;

	public StreamFeeder(String filename, String downloadingIndicatorFilename, IMediaDecoder decoder) {
		this(filename, downloadingIndicatorFilename, decoder, 0);
	}

	public StreamFeeder(String filename, String downloadingIndicatorFilename, IMediaDecoder decoder, long initialOffset) {
		_filename = filename;
		_decoder = decoder;
		_fileOffset = initialOffset;

		watchDownloadingIndicatorFile(downloadingIndicatorFilename);

		_feederThread = new Thread(_feederRunnable, "feeder");
		_feederThread.start();
	}

	// watches the download indicator file and marks when downloading is finished
	// assumes download indicator file is created before feeder
	private void watchDownloadingIndicatorFile(String indicatorFile) {
		// if the downloading indicator file doesn't exist by now, we're not downloading it
		if (!new File(indicatorFile).exists()) {
			_doneDownloading = true;
		} else {
			_observer = new FileObserver(indicatorFile, FileObserver.DELETE_SELF) {
				@Override
				public void onEvent(int event, String path) {
					_doneDownloading = true;
					_observer.stopWatching();
					_observer = null;
				}
			};
			_observer.startWatching();
		}
	}

	public String getFilename() { return _filename; }

	public void finish() {
		if (_observer != null)
			_observer.stopWatching();

		_feederThread.interrupt();
		try {
			_feederThread.join();
		} catch (InterruptedException ignored) {}
	}

	Runnable _feederRunnable = new Runnable() {
		@Override
		public void run() {
			RandomAccessFile file = null;
			try {
				while (file == null) {
					// new RandomAccessFile keeps throwing FileNotFoundExceptions, not sure why
					try {
						// make sure the file exists
						if (!new File(_filename).exists())
							Thread.sleep(50);
						file = new RandomAccessFile(_filename, "r");
					} catch (FileNotFoundException ignored) {
					}
				}

				file.seek(_fileOffset);
				while (file.getFilePointer() != _fileOffset)
					Thread.sleep(50);

				while (true) {
					// read the available bytes from the file and feed them to the mp3 decoder
					long length = file.length();
					int size = (int) (length - file.getFilePointer());
					if (size == 0 && _doneDownloading)
						break;
					if (size == 0) {
						Thread.sleep(50);
						continue;
					}

					byte[] c = new byte[size];
					int read = file.read(c);
					if (read > 0)
						_decoder.feed(c, read);

					Thread.sleep(50);
				}

				_decoder.completeStream();
			} catch (IOException e) {
				Log.e("StreamFeeder", "unable to feed decoder", e);
			} catch (InterruptedException ignored) {
			} finally {
				try {
					if (file != null)
						file.close();
				} catch (IOException e) {
					Log.e("StreamFeeder", "unable to close feed decoder file", e);
				}
			}
		}
	};
}

