package com.axelby.podax.player;

public class MPG123Stream extends MPG123 {
	static {
		MPG123.initializeLibrary();
	}

	public MPG123Stream() { _handle = openStream(); }
	public void feed(byte[] buffer) { MPG123.feed(_handle, buffer); }
	@Override public int readSamples(short[] buffer) { return MPG123.readFrame(_handle, buffer); }
}
