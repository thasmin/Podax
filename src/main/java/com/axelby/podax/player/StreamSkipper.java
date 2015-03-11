package com.axelby.podax.player;

class StreamSkipper {
	private final MPG123 _decoder;
	private final Thread _thread;

	public StreamSkipper(MPG123 decoder) {
		Runnable _runnable = new Runnable() {
			@Override
			public void run() {
				try {
					while (!_decoder.isStreamComplete())
						if (!_decoder.skipFrame())
							Thread.sleep(50);
				} catch (InterruptedException ignored) {
				}
			}
		};

		_decoder = decoder;
		_thread = new Thread(_runnable, "StreamSkipper");
		_thread.start();
	}

	public void close() {
		_thread.interrupt();
		try {
			_thread.join();
		} catch (InterruptedException ignored) {
		}
	}
}

