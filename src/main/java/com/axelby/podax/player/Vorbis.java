package com.axelby.podax.player;

public class Vorbis implements IMediaDecoder {
	static {
		System.loadLibrary("vorbis");
	}

	private static native long openFile(String filename);
	private static native void delete(long handle);
	private static native int readFrame(long handle, short[] buffer);
	private static native boolean skipFrame(long handle);
	private static native int seek(long handle, float offset);
	private static native float getPosition(long handle);
	private static native int getNumChannels(long handle);
	private static native int getRate(long handle);
	private static native float getDuration(long handle);

	private boolean _streamComplete = false;

	private long _handle = 0;
	public Vorbis(String filename) { _handle = openFile(filename); _streamComplete = true; }

	@Override
	public void close() {
		if (_handle != 0)
			Vorbis.delete(_handle);
	}

	@Override public int readFrame(short[] buffer) { return Vorbis.readFrame(_handle, buffer); }
	@Override public boolean skipFrame() { return Vorbis.skipFrame(_handle); }
	@Override public void seek(float offset) { Vorbis.seek(_handle, offset); }
	@Override public float getPosition() { return Vorbis.getPosition(_handle); }
	@Override public int getNumChannels() { return Vorbis.getNumChannels(_handle); }
	@Override public int getRate() { return Vorbis.getRate(_handle); }
	@Override public float getDuration() { return Vorbis.getDuration(_handle); }

	// vorbis streaming not implemented
	@Override public int getSeekFrameOffset(float position) { return 0; }
	@Override public void feed(byte[] buffer, int count) {  }
	@Override public void completeStream() { _streamComplete = true; }
	@Override public boolean isStreamComplete() { return _streamComplete; }
}

