package com.axelby.podax.player;

public class MPG123 implements IMediaDecoder {
	static {
		System.loadLibrary("mpg123");
		MPG123.init();
	}

	protected static native int init();
	protected static native long openFile(String filename);
	protected static native void delete(long handle);
	protected static native boolean skipFrame(long handle);
	protected static native int seek(long handle, float offsetInSeconds);
	protected static native float getPosition(long handle);
	protected static native int getNumChannels(long handle);
	protected static native int getRate(long handle);
	protected static native float getDuration(long handle);

	protected static native long openStream();
	protected static native void feed(long handle, byte[] buffer, int count);
	protected static native int readFrame(long handle, short[] buffer);
	protected static native int getSeekFrameOffset(long handle, float position);

	private boolean _streamComplete = false;

	private long _handle = 0;
	public MPG123() { _handle = openStream(); }
	public MPG123(String filename) { _handle = openFile(filename); }

	public void close() {
		if (_handle != 0)
			MPG123.delete(_handle);
	}

	public int readFrame(short[] buffer) { return MPG123.readFrame(_handle, buffer); }
	public boolean skipFrame() { return MPG123.skipFrame(_handle); }
	public void seek(float offset) { MPG123.seek(_handle, offset); }
	public float getPosition() { return MPG123.getPosition(_handle); }
	public int getNumChannels() { return MPG123.getNumChannels(_handle); }
	public int getRate() { return MPG123.getRate(_handle); }
	public float getDuration() { return MPG123.getDuration(_handle); }
	public int getSeekFrameOffset(float position) { return MPG123.getSeekFrameOffset(_handle, position); }
	public void feed(byte[] buffer, int count) { MPG123.feed(_handle, buffer, count); }
	public void completeStream() { _streamComplete = true; }
	public boolean isStreamComplete() { return _streamComplete; }
}

