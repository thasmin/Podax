package com.axelby.podax.player;

public class MPG123 implements IMediaDecoder {
	static {
		System.loadLibrary("mpg123");
		MPG123.init();
	}

	private static native int init();
	private static native String getErrorMessage(int error);
	private static native long openFile(String filename);
	private static native void delete(long handle);
	private static native int readSamples(long handle, short[] buffer, int offset, int numSamples);
	private static native int skipSamples(long handle, int numSamples);
	private static native int seek(long handle, float offsetInSeconds);
	private static native float getPosition(long handle);
	private static native long getPositionInFrames(long handle);
	private static native int getNumChannels(long handle);
	private static native int getRate(long handle);
	private static native long getNumFrames(long handle);
	private static native float getDuration(long handle);
	private static native int getFramesPerSecond(long handle);

	long _handle = 0;
	public MPG123(String filename) { _handle = openFile(filename); }

	public void close() {
		if (_handle != 0)
			MPG123.delete(_handle);
	}

	public int readSamples(short[] buffer, int offset, int numSamples) {
		return MPG123.readSamples(_handle, buffer, offset, numSamples);
	}
	public int skipSamples(int numSamples) { return MPG123.skipSamples(_handle, numSamples); }
	public int seek(float offset) { return MPG123.seek(_handle, offset); }
	public float getPosition() { return MPG123.getPosition(_handle); }
	public int getNumChannels() { return MPG123.getNumChannels(_handle); }
	public int getRate() { return MPG123.getRate(_handle); }
	public float getDuration() { return MPG123.getDuration(_handle); }
}
