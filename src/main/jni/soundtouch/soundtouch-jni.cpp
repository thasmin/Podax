#include <jni.h>
#include <android/log.h>

#include "SoundTouch.h"

#define LOGV(...)   __android_log_print((int)ANDROID_LOG_INFO, "SOUNDTOUCH", __VA_ARGS__)
//#define LOGV(...)


#define DLL_PUBLIC __attribute__ ((visibility ("default")))

using namespace soundtouch;

extern "C" DLL_PUBLIC jstring Java_com_axelby_podax_player_SoundTouch_getVersionString(JNIEnv *env, jobject thiz)
{
    return env->NewStringUTF(SoundTouch::getVersionString());
}

extern "C" DLL_PUBLIC jlong Java_com_axelby_podax_player_SoundTouch_init(JNIEnv *env, jobject thiz,
		jint sampleRate, jint numChannels, jfloat speedRatio)
{
    SoundTouch* pSoundTouch = new SoundTouch();
	if (pSoundTouch == 0)
		return 0;

	pSoundTouch->setSampleRate(sampleRate);
	pSoundTouch->setChannels(numChannels);
	pSoundTouch->setTempoChange(100 * (speedRatio - 1));

	// use settings for speech processing
	pSoundTouch->setSetting(SETTING_SEQUENCE_MS, 40);
	pSoundTouch->setSetting(SETTING_SEEKWINDOW_MS, 15);
	pSoundTouch->setSetting(SETTING_OVERLAP_MS, 8);

	return (jlong)pSoundTouch;
}

extern "C" DLL_PUBLIC jint Java_com_axelby_podax_player_SoundTouch_timeStretch(JNIEnv *env, jobject thiz,
		jlong handle, jshortArray inBuffer, jint inSize, jshortArray outBuffer, jint outSize, jint numChannels)
{
	if (handle == 0)
		return 0;

	short *inBuf = env->GetShortArrayElements(inBuffer, NULL);
	if (inBuf == NULL)
		return 0;
	short *outBuf = env->GetShortArrayElements(outBuffer, NULL);
	if (outBuf == NULL) {
		env->ReleaseShortArrayElements(inBuffer, inBuf, 0);
		return 0;
	}

	SoundTouch* pSoundTouch = (SoundTouch*)handle;
	pSoundTouch->putSamples(inBuf, inSize / numChannels);
	int outSamples = pSoundTouch->receiveSamples(outBuf, outSize / numChannels);

	env->ReleaseShortArrayElements(inBuffer, inBuf, JNI_ABORT);
	env->ReleaseShortArrayElements(outBuffer, outBuf, 0);

	return outSamples * numChannels;
}

extern "C" DLL_PUBLIC void Java_com_axelby_podax_player_SoundTouch_close(JNIEnv *env, jobject thiz, jlong handle) {
	if (handle == 0)
		return;
	SoundTouch* pSoundTouch = (SoundTouch*)handle;
	delete pSoundTouch;
}
