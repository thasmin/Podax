package com.axelby.podax.player;

public class Vorbis implements IMediaDecoder {
	static {
		System.loadLibrary("vorbis");
	}

	private static native long openFile(String filename);
	private static native void delete(long handle);
	private static native int readSamples(long handle, short[] buffer, int offset, int numSamples);
	private static native int skipSamples(long handle, int numSamples);
	private static native int isSeekable(long handle);
	private static native int seek(long handle, float offset);
	private static native float getPosition(long handle);
	private static native int getNumChannels(long handle);
	private static native int getRate(long handle);
	private static native float getDuration(long handle);
	private static native long getRawLength(long handle);
	private static native long getPCMLength(long handle);
	private static native long getTimeLength(long handle);

	long _handle = 0;
	public Vorbis(String filename) {
		_handle = openFile(filename);
	}

	@Override
	public void close() {
		if (_handle != 0)
			Vorbis.delete(_handle);
	}

	@Override
	public int readSamples(short[] buffer, int offset, int numSamples) {
		return Vorbis.readSamples(_handle, buffer, offset, numSamples);
	}
	public int skipSamples(int numSamples) { return Vorbis.skipSamples(_handle, numSamples); }
	public boolean isSeekable() { return Vorbis.isSeekable(_handle) != 0; }
	@Override public int seek(float offset) { return Vorbis.seek(_handle, offset); }
	@Override public float getPosition() {
		return Vorbis.getPosition(_handle);
	}
	@Override public int getNumChannels() {
		return Vorbis.getNumChannels(_handle);
	}
	@Override public int getRate() {
		return Vorbis.getRate(_handle);
	}
	@Override public float getDuration() { return Vorbis.getDuration(_handle); }
}
