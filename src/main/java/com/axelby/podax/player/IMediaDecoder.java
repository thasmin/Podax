package com.axelby.podax.player;

public interface IMediaDecoder {
	void close();
	int readSamples(short[] buffer, int offset, int numSamples);
	int skipSamples(int numSamples);
	int seek(float offsetInSeconds);
	float getPosition();
	int getNumChannels();
	int getRate();
	float getDuration();
}
