package com.axelby.podax.player;

public interface IMediaDecoder {
	void close();
	int readFrame(short[] buffer);
	boolean skipFrame();
	void seek(float offsetInSeconds);
	float getPosition();
	int getNumChannels();
	int getRate();
	float getDuration();

	// for streaming
	public int getSeekFrameOffset(float position);
	public void feed(byte[] buffer, int count);
	public void completeStream();
	public boolean isStreamComplete();
}
