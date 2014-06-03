//
//	Slaut - Slamnig Audio Utilities project
//
//	wsola.c
//
//	WSOLA audio time stretching functions
//
//	Created: 2014/05/18 D.Slamnig
// Modified: 2014/05/29 D.Slamnig
// Modified: 2014/06/01 D.Slamnig

#include <math.h>
#include <stdbool.h>
#include <malloc.h>
#include <sys/limits.h>
#include <android/log.h>

#include "errorcodes.h"

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "wsola.c", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "wsola.c", __VA_ARGS__))

// Overlap-and-add window size in milliseconds.
#define OLA_WIN_MS	20
// Hann window copy flags:
#define HANN_FULL	1
#define HANN_1HALF	2
#define HANN_2HALF	3

int g_inRate;
bool g_stereo;
float g_speedRatio;
int g_olaWin, g_olaStep;
short *g_inBuf, *g_outBuf;
float *g_calcBuf;
int g_inBufLen, g_outBufLen;
int g_inBufFrames, g_outBufFrames;
float *g_hann;
int g_decimate;

// Initialize WSOLA variables to null values:
void WSOLA_Init(void)
{
	g_inRate = 0;
	g_stereo = false;
	g_speedRatio = -1.0f;
	g_olaWin = g_olaStep = 0;
	g_inBuf = g_outBuf = NULL;
	g_calcBuf = NULL;
	g_inBufLen = g_outBufLen;
	g_hann = NULL;
	g_decimate = 5;
}

// Free stuff - call when finished using WSOLA:
void WSOLA_Close(void)
{
	if (g_calcBuf != NULL)
		free(g_calcBuf);
	if (g_hann != NULL)
		free(g_hann);

	WSOLA_Init();
}

// calculate Hann window factors
float *WSOLA_MakeHann(int nLen)
{
	int k;
	float fScale, *pf, *hann = NULL;

	if ((hann = (float*)malloc(sizeof(float) * nLen)) == NULL)
	{
		LOGW("MakeHann - malloc failed");
		return NULL;
	}

	fScale = 2.0f * M_PI / (float)nLen;
	for (pf = hann, k = 0; k < nLen; ++k, ++pf)
		*pf = 0.5f * (1.0f - cosf((float)k * fScale));
	return hann;
}

// Copy and mix down PCM data to float array,
// to be used in WSOLA similarity calculation:
// Modified: 2014/06/01 D.Slamnig - no division
bool WSOLA_PrepareCalcBuf(void)
{
	int k;
	short *ps;
	float *pf;

	if (g_stereo == true) {
		for (k = 0, ps = g_inBuf, pf = g_calcBuf; 
			k < g_inBufFrames;
			++k, ps += 2, ++pf)
		{
			// mix to mono:
			*pf = (float)(*ps + *(ps + 1));
		}
	} else {
		for (k = 0, ps = g_inBuf, pf = g_calcBuf; 
			k < g_inBufFrames;
			++k, ++ps, ++pf)
		{
			*pf = (float)*ps;
		}
	}
}

// Copy in new values and allocate buffers as needed:
bool WSOLA_Prepare(short *inBuf, int inBufLen, 
	short *outBuf, int outBufLen, 
	int inRate, bool stereo, 
	float speedRatio, int decimate)
{
	// if rate changed, recalc windows
	if (inRate != g_inRate) {
		// calc OLA window size:
		g_olaWin = (OLA_WIN_MS * inRate) / 1000;
		g_olaStep = g_olaWin / 2;
		// make even:
		g_olaWin = g_olaStep * 2;
		
		if (g_hann != NULL)
			free(g_hann);
		if ((g_hann = WSOLA_MakeHann(g_olaWin)) == NULL)
			return false;
		
		g_inRate = inRate;
	}

	g_speedRatio = speedRatio;
	g_decimate = decimate;
	g_inBuf = inBuf;
	g_inBufFrames = (stereo == true) ? inBufLen / 2 : inBufLen;
	
	if (g_calcBuf == NULL || stereo != g_stereo || inBufLen != g_inBufLen) {
		g_stereo = stereo;
		g_inBufLen = inBufLen;
	
		if (g_calcBuf != NULL)
			free(g_calcBuf);
		if ((g_calcBuf = (float *)malloc(sizeof(float) * g_inBufFrames)) == NULL)
			return false;
	}

	WSOLA_PrepareCalcBuf();
	
	g_outBuf = outBuf;
	g_outBufLen = outBufLen;
	g_outBufFrames = (g_stereo == true) ? g_outBufLen / 2 : g_outBufLen;

	return true;
}

// Copies a windowed block from input to output audio buffer.
// Returns number of frames copied:
int HannCopy(int inStartFrame, int outStartFrame, int mode)
{
	int k, copyFrames, inLeft, outLeft;
	float *hann;
	short *in, *out;

	// check bounds:
	copyFrames = (mode == HANN_FULL) ? g_olaWin : g_olaStep;
	inLeft = g_inBufFrames - inStartFrame;
	outLeft = g_outBufFrames - outStartFrame;

	if (copyFrames > inLeft)
		copyFrames = inLeft;
	if (copyFrames > outLeft)
		copyFrames = outLeft;

	if (copyFrames == 0)
		return 0;

	// set Hann start:
	hann = (mode == HANN_2HALF) ? g_hann + g_olaStep : g_hann;

	// copy frames:
	if (g_stereo == true) {
		for (in = g_inBuf + inStartFrame * 2, 
				out = g_outBuf + outStartFrame * 2, k = 0; 
				k < copyFrames; 
				++k, in += 2, out += 2, ++hann)
		{
			float h = *hann;
			*out += (short)((float)*in * h);
			*(out + 1) += (short)((float)*(in + 1) * h);
		}
	} else {
		for (in = g_inBuf + inStartFrame, 
				out = g_outBuf + outStartFrame, k = 0; 
				k < copyFrames; 
				++k, ++in, ++out, ++hann)
		{
			float h = *hann;
			*out += (short)((float)*in * h);
		}
	}

	return copyFrames;
}

// Copies a direct block from input to output audio buffer.
// Stops on end of input or output buffer.
// Returns number of frames copied:
int EndCopy(int inStartFrame, int outStartFrame)
{
	int k, copyFrames, inLeft, outLeft;
	short *in, *out;

	// check bounds:
	inLeft = g_inBufFrames - inStartFrame;
	outLeft = g_outBufFrames - outStartFrame;

	copyFrames = inLeft;
	if (copyFrames > outLeft)
		copyFrames = outLeft;

	if (copyFrames == 0)
		return 0;

	// copy frames:
	if (g_stereo == true) {
		for (in = g_inBuf + inStartFrame * 2, 
				out = g_outBuf + outStartFrame * 2, k = 0; 
				k < copyFrames; 
				++k, in += 2, out +=2)
		{
			*out = *in;
			*(out + 1) = *(in + 1);
		}
	} else {
		for (in = g_inBuf + inStartFrame, 
				out = g_outBuf + outStartFrame, k = 0; 
				k < copyFrames; 
				++k, ++in, ++out)
		{
			*out = *in;
		}
	}

	return copyFrames;
}


// Get a simplified correlation coefficient for source and target buffers:
float WSOLA_Compare(int like, int win)
{
	int k;
	float sum, *pfLike, *pfWin;

	for (pfLike = g_calcBuf + like,
		pfWin = g_calcBuf + win,
		sum = 0.0f, k = 0; 
		k < g_olaWin;
		// Modified: 2014/06/01 D.Slamnig
		// moved decimation to here:
		k += g_decimate, pfLike += g_decimate, pfWin += g_decimate)
		// ++k, ++pfLike, ++pfWin)
		//
	{
		sum += *pfLike * *pfWin; // just multiply and add to sum
	}

	return sum;
}

// get best target match for source buffer:
int WSOLA_Match(int like, int center)
{
	int start, end, best, k;
	float val, bestval;

	if (like > g_inBufFrames - g_olaWin)
		return center; // give up

	start = center - g_olaStep;
	if (start < 0)
		start = 0;

	end = center + g_olaStep;
	if (end > g_inBufFrames - g_olaWin)
		end = g_inBufFrames - g_olaWin;

	if (start >= end)
		return center; // give up

	// Modified: 2014/06/01 D.Slamnig
	// removed decimation from here:
	for (k = start, best = center, bestval = -1; k < end; ++k) {
	//for (k = start, best = center, bestval = -1; k < end; k += g_decimate) {
		val = WSOLA_Compare(like, k);
		if (val > bestval) {
			bestval = val;
			best = k;
		}
	}

	return best;
}

// returns negative error codes or 0 for success:
int WSOLA_TimeStretch(short *inBuf, int inBufLen, 
	short *outBuf, int outBufLen, 
	int inRate, bool stereo, float speedRatio, int decimate)
{
	int k, out, like, center, winCnt, cnt, endSeg;
	short *ps, *in;

	//LOGI("TimeStretch inlen:%d outlen:%d rate:%d stereo:%s ratio:%f",
	//	inBufLen, outBufLen, inRate, ((stereo == true) ? "yes" : "no"), speedRatio);

	if (inBuf == NULL || inBufLen <= 0
		|| outBuf == NULL || outBufLen <= 0
		|| speedRatio <= 0.0f
		|| decimate < 1) {
		LOGW("TimeStretch - invalid params");
		return ERR_INVALIDPARAMS;
	}

	// if speed 1.0 just copy in to out and return:
	if (speedRatio == 1.0) {
		for (in = inBuf, ps = outBuf, k = 0; k < inBufLen; ++k, ++ps, ++in)
			*ps = *in;

		return SUCCESS;
	}

	if (WSOLA_Prepare(inBuf, inBufLen, outBuf, outBufLen,
						inRate, stereo, speedRatio, decimate) == false) {
		WSOLA_Close();
		LOGW("TimeStretch - prepare failed");
		return ERR_ALLOC;
	}

	// zero outbuf:
	for (ps = g_outBuf, k = 0; k < g_outBufLen; ++k, ++ps)
		*ps = 0;

	// calc total number of windows to copy:
	winCnt = g_outBufFrames / g_olaStep;

	//LOGI("TimeStretch inframes:%d outframes:%d window:%d wincnt:%d",
	//	g_inBufFrames, g_outBufFrames, g_olaWin, winCnt);

	// copy second Hann half of inbuf start directly:
	HannCopy(0, 0, HANN_2HALF);
	
	// Modified: 2014/05/29 D.Slamnig
	// Copy only full length WSOLA windows:
	for (k = out = like = 0; 
		k < winCnt && out < g_outBufFrames - g_olaWin; 
		++k, out += g_olaStep) {
		// find target center:
		center = (int)((float)out * g_speedRatio);
		// get best target match:
		int match = WSOLA_Match(like, center);
		// copy target to output:
		cnt = HannCopy(match, out, HANN_FULL);
		// LOGI("TimeStretch - Hann copied: %d", cnt);
		if (cnt < g_olaWin) // should not happen now
			break;
		// increment source:
		like = match + g_olaStep;
	}

	// do something about end of buffer:
	// Added: 2014/05/29 D.Slamnig
	// Fade in and copy original till the end of output buffer.
	// This ensures smooth transition between buffers on playback:
	endSeg = g_outBufFrames - out;
	HannCopy(g_inBufFrames - endSeg, out, HANN_1HALF);
	EndCopy(g_inBufFrames - (endSeg - g_olaStep), out + g_olaStep);

	return SUCCESS;
}


