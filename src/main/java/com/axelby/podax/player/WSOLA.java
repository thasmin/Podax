package com.axelby.podax.player;

//	Slamnig Audio Utilities project
//	Created: 2014/05/18 D.Slamnig

public class WSOLA
{
	public static class Error {
		public static final int SUCCESS = 0;
		public static final int ERR_INVALIDPARAMS = -1;
		public static final int ERR_ALLOC = -2;

		public int code;

		public String toString(){
			switch(code){
				case 0:
					return "Success";
				case -1:
					return "Invalid parameters";
				case -2:
					return "Allocation error";
			}
			return "Unknown error";
		}
	}

	public WSOLA() {
		// initialize NDK part:
		init();
	}

	// wsolaStretch
	//
	// Checks parameters, allocates the output buffer and calls the NDK:
	//
	// Parameters:
	// -----------
	// short[] audioBuffer - 16-bit input PCM audio data, stereo or mono.
	// 						If stereo, has to be frame bounded. One stereo frame is 2 shorts, L and R channel.
	// int sampleRate - Input sample rate in samples/second.
	// boolean stereo - Stereo flag.
	// float speedRatio - Output playback speed multiplier.
	// int quality - WSOLA processing quality. Less is better, 1 is best. Maximum is about 50 for 44.1 kHz.
	// SlautError err - err.code is returned error code, or SUCCESS.
	//
	// Returns:
	// --------
	// On success, returns a short array with time-stretched audio data. The length of the output array is input length / speedRatio.
	// On failure, returns null. The err parameter holds error information.
	//
	public short[] stretch(short[] audioBuffer, int plainSize, int sampleRate, boolean stereo, float speedRatio, int quality, Error err) {
		short[] stretchBuffer;
		int stretchSize;
		int errCode;

		if (audioBuffer == null
				|| plainSize == 0
				|| sampleRate <= 0
				|| speedRatio <= 0.0f) {
			if (err != null)
				err.code = Error.ERR_INVALIDPARAMS;
			return null;
		}

		if (quality < 1)
			quality = 1;

		stretchSize = (int)Math.floor((float)plainSize / speedRatio + 0.5f);

		if (stereo)
			stretchSize = (stretchSize / 2) * 2;

		if ((stretchBuffer = new short[stretchSize]) == null) {
			if (err != null)
				err.code = Error.ERR_ALLOC;
			return null;
		}

		errCode = wsolaStretchJNI(audioBuffer, plainSize, stretchBuffer, sampleRate, stereo, speedRatio, quality);

		if (errCode != Error.SUCCESS)
			stretchBuffer = null;

		if (err != null)
			err.code = errCode;

		return stretchBuffer;
	}

	// Initalizes the NDK part:
	public native void init();
	// Frees NDK allocated buffers - call when done using the library:
	public native void close();

	// NDK WSOLA time-stretch function:
	private native int wsolaStretchJNI(short[] plainBuffer, int sampleCount, short[] stretchBuffer,
									   int sampleRate, boolean stereo, float speedRatio, int quality);

	static {
		System.loadLibrary("wsola");
	}
}
