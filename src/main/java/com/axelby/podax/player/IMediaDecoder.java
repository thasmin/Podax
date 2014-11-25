package com.axelby.podax.player;

public interface IMediaDecoder {
	void close();
	int readSamples(short[] buffer);
	int seek(float offsetInSeconds);
	float getPosition();
	int getNumChannels();
	int getRate();
	float getDuration();
}
