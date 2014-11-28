//Slaut - Slamnig Audio Utilities project
// podax-jni.c
// JNI code
// Created: 2014/05/18 D.Slamnig

#include <jni.h>
#include <string.h>
#include <math.h>
#include <stdbool.h>

#include "wsola.h"
#include "errorcodes.h"

JNIEXPORT void JNICALL
Java_com_axelby_podax_player_WSOLA_init(JNIEnv* env, jobject thiz)
{
	WSOLA_Init();
}

JNIEXPORT void JNICALL
Java_com_axelby_podax_player_WSOLA_close(JNIEnv* env, jobject thiz)
{
	WSOLA_Close();
}

JNIEXPORT jint JNICALL
Java_com_axelby_podax_player_WSOLA_wsolaStretchJNI(JNIEnv* env, jobject thiz, 
	jshortArray plainBuffer, jint inSize, jshortArray stretchBuffer,
	jint sampleRate, jboolean stereo, jfloat speedRatio, jint quality)
{
	short *inBuf, *outBuf;
	int outSize;
	int errCode = ERR_INVALIDPARAMS;

	if ((inBuf = (*env)->GetShortArrayElements(env, plainBuffer, NULL)) != NULL) {
		if ((outBuf = (*env)->GetShortArrayElements(env, stretchBuffer, NULL)) != NULL) {
			outSize = (*env)->GetArrayLength(env, stretchBuffer);
			
			errCode = WSOLA_TimeStretch(inBuf, inSize, outBuf, outSize,
									sampleRate, stereo, speedRatio, quality);

			(*env)->ReleaseShortArrayElements(env, stretchBuffer, outBuf, 0);
		}
		(*env)->ReleaseShortArrayElements(env, plainBuffer, inBuf, 0);
	}
	
	return errCode;
}
