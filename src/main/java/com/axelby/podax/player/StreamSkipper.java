package com.axelby.podax.player;

public class StreamSkipper {
	private final IMediaDecoder _decoder;
	private final Thread _thread;
	private boolean _isDone = false;

	public StreamSkipper(IMediaDecoder decoder) {
		Runnable _runnable = new Runnable() {
			@Override
			public void run() {
				try {
					while (!_decoder.isStreamComplete())
						if (!_decoder.skipFrame())
							Thread.sleep(50);
				} catch (InterruptedException ignored) {
				} finally {
					_isDone = true;
				}
			}
		};

		_decoder = decoder;
		_thread = new Thread(_runnable, "StreamSkipper");
		_thread.start();
	}

	public boolean isDone() { return _isDone; }

	public void close() {
		_thread.interrupt();
		try {
			_thread.join();
		} catch (InterruptedException ignored) {
		}
	}
}

