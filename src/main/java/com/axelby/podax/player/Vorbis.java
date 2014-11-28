package com.axelby.podax.player;

public class Vorbis implements IMediaDecoder {
	static {
		System.loadLibrary("vorbis");
	}

	private static native long openFile(String filename);
	private static native void delete(long handle);
	private static native int readSamples(long handle, short[] buffer);
	private static native int seek(long handle, float offset);
	private static native float getPosition(long handle);
	private static native int getNumChannels(long handle);
	private static native int getRate(long handle);
	private static native float getDuration(long handle);

	long _handle = 0;
	public Vorbis(String filename) {
		_handle = openFile(filename);
	}

	@Override
	public void close() {
		if (_handle != 0)
			Vorbis.delete(_handle);
	}

	@Override public int readSamples(short[] buffer) { return Vorbis.readSamples(_handle, buffer); }
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
