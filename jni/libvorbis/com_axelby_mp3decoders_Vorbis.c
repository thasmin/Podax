#include <jni.h>
#include "ogg/ogg.h"
#include "ivorbiscodec.h"
#include "ivorbisfile.h"
#include <stdlib.h>
#include <stdio.h>
#include <android/log.h>

typedef struct
{
	OggVorbis_File* ogg;
	int channels;
	int rate;
	float length;
	int bitstream;
} OggFile;

static char buffer[10000];

JNIEXPORT jlong JNICALL Java_com_axelby_mp3decoders_Vorbis_openFile
		(JNIEnv* env, jclass c, jstring obj_filename) {
	OggVorbis_File* ogg = (OggVorbis_File*) malloc(sizeof(OggVorbis_File));

	char* filename = (char*)(*env)->GetStringUTFChars(env, obj_filename, 0);
	FILE* file = fopen(filename, "rb");
	(*env)->ReleaseStringUTFChars(env, obj_filename, filename);
	if (file == 0)
	{
		__android_log_write(ANDROID_LOG_INFO, "mp3decoders-jni", "didn't open file");
		free(ogg);
		return 0;
	}

	if (ov_open(file, ogg, NULL, 0) != 0)
	{
		__android_log_write(ANDROID_LOG_INFO, "mp3decoders-jni", "failed to ov_open file");
		fclose(file);
		free(ogg);
		return 0;
	}

	vorbis_info *info = ov_info(ogg, -1);
	int channels = info->channels;
	int rate = info->rate;
	float length = (float)ov_time_total(ogg, -1) / 1000.0f;

	OggFile* oggFile = (OggFile*) malloc(sizeof(OggFile));
	oggFile->ogg = ogg;
	oggFile->channels = channels;
	oggFile->rate = rate;
	oggFile->length = length;

	return (jlong)oggFile;	
}

JNIEXPORT jint JNICALL Java_com_axelby_mp3decoders_Vorbis_getNumChannels
		(JNIEnv* env, jclass c, jlong handle) {
	OggFile* file = (OggFile*)handle;
	return file->channels;
}

JNIEXPORT jint JNICALL Java_com_axelby_mp3decoders_Vorbis_getRate
		(JNIEnv* env, jclass c, jlong handle) {
	OggFile* file = (OggFile*)handle;
	return file->rate;
}

JNIEXPORT jfloat JNICALL Java_com_axelby_mp3decoders_Vorbis_getLength
		(JNIEnv* env, jclass c, jlong handle) {
	OggFile* file = (OggFile*)handle;
	return file->length;
}

JNIEXPORT jlong JNICALL Java_com_axelby_mp3decoders_Vorbis_getRawLength
		(JNIEnv* env, jclass c, jlong handle) {
	OggFile* file = (OggFile*)handle;
	return ov_raw_total(file->ogg, -1);
}

JNIEXPORT jlong JNICALL Java_com_axelby_mp3decoders_Vorbis_getPCMLength
		(JNIEnv* env, jclass c, jlong handle) {
	OggFile* file = (OggFile*)handle;
	return ov_pcm_total(file->ogg, -1);
}

JNIEXPORT jlong JNICALL Java_com_axelby_mp3decoders_Vorbis_getTimeLength
		(JNIEnv* env, jclass c, jlong handle) {
	OggFile* file = (OggFile*)handle;
	return ov_time_total(file->ogg, -1);
}

JNIEXPORT jint JNICALL Java_com_axelby_mp3decoders_Vorbis_readSamples
		(JNIEnv* env, jclass c, jlong handle, jshortArray obj_samples, jint offset, jint numSamples) {
	short* samples = (short*)(*env)->GetPrimitiveArrayCritical(env, obj_samples, 0);

	OggFile* file = (OggFile*)handle;
	int toRead = 2 * numSamples;
	int read = 0;

	samples += offset;

	while (read != toRead)
	{
		int ret = ov_read(file->ogg, (char*)samples + read, toRead - read, &file->bitstream);
		if (ret == OV_HOLE)
			continue;
		if (ret == OV_EBADLINK || ret == OV_EINVAL || ret == 0)
			break;
		read+=ret;
	}

	(*env)->ReleasePrimitiveArrayCritical(env, obj_samples, samples, 0);
	return read / 2;
}

JNIEXPORT jint JNICALL Java_com_axelby_mp3decoders_Vorbis_skipSamples
		(JNIEnv* env, jclass c, jlong handle, jint numSamples) {
	OggFile* file = (OggFile*)handle;
	int toRead = 2 * numSamples;
	int read = 0;

	while (read != toRead)
	{
		int ret = ov_read(file->ogg, buffer, (toRead - read)>10000?10000:(toRead-read), &file->bitstream);
		if (ret == OV_HOLE)
			continue;
		if (ret == OV_EBADLINK || ret == OV_EINVAL || ret == 0)
			break;
		read += ret;
	}

	return read / 2;
}

JNIEXPORT void JNICALL Java_com_axelby_mp3decoders_Vorbis_delete
		(JNIEnv* env, jclass c, jlong handle) {
	OggFile* file = (OggFile*)handle;
	ov_clear(file->ogg);
	free(file->ogg);
	free(file);
}

JNIEXPORT jint JNICALL Java_com_axelby_mp3decoders_Vorbis_isSeekable
		(JNIEnv* env, jclass c, jlong handle) {
	OggFile* file = (OggFile*)handle;
	return file->ogg->seekable;
}

JNIEXPORT jfloat JNICALL Java_com_axelby_mp3decoders_Vorbis_getPosition
		(JNIEnv* env, jclass c, jlong handle) {
	OggFile* file = (OggFile*)handle;
	return 0.001f * (float)ov_time_tell(file->ogg);
}

JNIEXPORT jint JNICALL Java_com_axelby_mp3decoders_Vorbis_seek
		(JNIEnv* env, jclass c, jlong handle, jfloat time) {
	OggFile* file = (OggFile*)handle;
	return ov_time_seek(file->ogg, (ogg_int64_t)(time * 1000.f));
}

/*
JNIEXPORT jint JNICALL Java_com_axelby_mp3decoders_Vorbis_openStream
		(JNIEnv *env, jclass c, jbyteArray bytes)
{
	ogg_sync_state ogg_sync;
	ogg_page ogg_page;
	ogg_stream_state ogg_stream;

	vorbis_info vi;
	vorbis_comment vc;

	// open an ogg sync state
	ogg_sync_init(&ogg_sync);

	// turn jbyteArray into char[]

	// read 4096 bytes from the stream into the ogg sync state and tell it how many bytes we read
	buffer = ogg_sync_buffer(&oy,4096);
	bytes = fread(buffer, 1, 4096, stdin);
	ogg_sync_wrote(&oy, bytes);

	// move the bytes from the syncstream to a page
	if (ogg_sync_pageout(&ogg_sync, &ogg_page) != 1) {
		// simply run out of data
		if (bytes < 4096)
			break;

		// must not be Vorbis data
		return 2;
    }	  

	// set up an ogg stream with the proper serial number (?)
	ogg_stream_init(&ogg_stream, ogg_page_serialno(&ogg_page));

	// set up a vorbis stream
	vorbis_info_init(&vi);
	vorbis_comment_init(&vc);
	if(ogg_stream_pagein(&os, &og) < 0) { 
		// stream version mismatch?
		return 3;
	}

	if(ogg_stream_packetout(&os, &op) != 1) { 
		// must not be vorbis
		return 4;
	}
}
*/
