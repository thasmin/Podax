package com.axelby.podax.player;

public class MPG123 implements IMediaDecoder {
	static boolean _initted = false;
	static {
		MPG123.initializeLibrary();
	}

	public static void initializeLibrary() {
		if (!_initted) {
			System.loadLibrary("mpg123");
			MPG123.init();
			_initted = true;
		}
	}

	protected static native int init();
	protected static native long openFile(String filename);
	protected static native void delete(long handle);
	protected static native int readSamples(long handle, short[] buffer);
	protected static native int seek(long handle, float offsetInSeconds);
	protected static native float getPosition(long handle);
	protected static native int getNumChannels(long handle);
	protected static native int getRate(long handle);
	protected static native float getDuration(long handle);

	protected static native long openStream();
	protected static native void feed(long handle, byte[] buffer);
	protected static native int readFrame(long handle, short[] buffer);

	long _handle = 0;
	protected MPG123() { }
	public MPG123(String filename) { _handle = openFile(filename); }

	public void close() {
		if (_handle != 0)
			MPG123.delete(_handle);
	}

	public int readSamples(short[] buffer) {
		return MPG123.readSamples(_handle, buffer);
	}
	public int seek(float offset) { return MPG123.seek(_handle, offset); }
	public float getPosition() { return MPG123.getPosition(_handle); }
	public int getNumChannels() { return MPG123.getNumChannels(_handle); }
	public int getRate() { return MPG123.getRate(_handle); }
	public float getDuration() { return MPG123.getDuration(_handle); }
}

