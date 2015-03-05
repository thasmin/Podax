package com.axelby.podax.player;

public class SoundTouch {
	static {
		System.loadLibrary("soundtouch");
	}

	protected static native long init(int sampleRate, int numChannels, float speedRatio);
	protected static native int timeStretch(long handle, short[] inBuffer, int inSize, short[] outBuffer, int outSize, int numChannels);
	protected static native void close(long handle);

	private long _handle = 0;
	public SoundTouch(int sampleRate, int numChannels, float speedRatio) {
		_handle = init(sampleRate, numChannels, speedRatio);
	}

	public int stretch(short[] inBuffer, int inSize, short[] outBuffer, int outSize, int numChannels) {
		return timeStretch(_handle, inBuffer, inSize, outBuffer, outSize, numChannels);
	}

	public void close() { close(_handle); }
}
