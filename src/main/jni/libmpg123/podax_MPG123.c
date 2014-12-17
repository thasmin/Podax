#include "mpg123.h"
#include <jni.h>
#include <fcntl.h>
#include <android/log.h>

#include <string.h>
#include <stdio.h>

void printerr(const char* message, int err) {
	__android_log_print(ANDROID_LOG_ERROR, "podax-jni", "error - %s: %d %s", message, err, mpg123_plain_strerror(err));
}

typedef struct _MP3File
{
	mpg123_handle* handle;
	int channels;
	long rate;
	long num_samples;
	int samples_per_frame;
	double secs_per_frame;
	long num_frames;
	float duration;
} MP3File;

MP3File* mp3file_init(mpg123_handle *handle) {
	MP3File* mp3file = malloc(sizeof(MP3File));
	memset(mp3file, 0, sizeof(MP3File));
	mp3file->handle = handle;
	return mp3file;
}

void mp3file_delete(MP3File *mp3file) {
	mpg123_close(mp3file->handle);
	mpg123_delete(mp3file->handle);
	free(mp3file);
}

int mp3file_determineStats(MP3File *mp3) {
	if (mp3 == NULL)
		return;

	int encoding;
	mpg123_handle* mh = mp3->handle;
	int err = mpg123_getformat(mh, &mp3->rate, &mp3->channels, &encoding);
	if (err == MPG123_NEED_MORE)
		return err;
	if (err != MPG123_OK) {
		printerr("mpg123_getformat", err);
		return err;
	}

	mp3->num_samples = mpg123_length(mh);
	mp3->samples_per_frame = mpg123_spf(mh);
	mp3->secs_per_frame = mpg123_tpf(mh);

	if (mp3->num_samples == MPG123_ERR || mp3->samples_per_frame < 0)
		mp3->num_frames = 0;
	else
		mp3->num_frames = mp3->num_samples / mp3->samples_per_frame;

	if (mp3->num_samples == MPG123_ERR || mp3->samples_per_frame < 0 || mp3->secs_per_frame < 0)
		mp3->duration = 0;
	else
		mp3->duration = mp3->num_samples / mp3->samples_per_frame * mp3->secs_per_frame;
	return err;
}

JNIEXPORT jint JNICALL Java_com_axelby_podax_player_MPG123_init
	(JNIEnv *env, jclass c)
{
	return mpg123_init();
}

JNIEXPORT jstring JNICALL Java_com_axelby_podax_player_MPG123_getErrorMessage
	(JNIEnv *env, jclass c, jint error)
{
	return (*env)->NewStringUTF(env, mpg123_plain_strerror(error));
}

JNIEXPORT jlong JNICALL Java_com_axelby_podax_player_MPG123_openStream
	(JNIEnv *env, jclass c)
{
	// init mpg123 handle
    int err = MPG123_OK;
    mpg123_handle *mh = mpg123_new(NULL, &err);
	if (err != MPG123_OK) {
		__android_log_print(ANDROID_LOG_INFO, "podax-jni", "mpg123_new error: %s", mpg123_plain_strerror(err));
		return 0;
	}

	// set handle up as stream
	err = mpg123_open_feed(mh);
	if (err != MPG123_OK) {
		__android_log_print(ANDROID_LOG_INFO, "podax-jni", "mpg123_open_feed error: %s", mpg123_plain_strerror(err));
		return 0;
	}

	MP3File* stream = mp3file_init(mh);
	return (jlong)stream;
}

JNIEXPORT void JNICALL Java_com_axelby_podax_player_MPG123_feed
	(JNIEnv *env, jclass c, jlong handle, jbyteArray in_buffer, jint in_size)
{
	MP3File *mp3 = (MP3File*)handle;
	mpg123_handle *mh = mp3->handle;
	jboolean isCopy;
	jbyte* b = (*env)->GetByteArrayElements(env, in_buffer, &isCopy);

	int err = mpg123_feed(mh, b, in_size);
	if (err != MPG123_OK)
		__android_log_print(ANDROID_LOG_INFO, "podax-jni", "mpg123_feed error: %s", mpg123_plain_strerror(err));
	(*env)->ReleaseByteArrayElements(env, in_buffer, b, JNI_ABORT);

	if (mp3->rate == 0) {
		off_t frame_offset;
		unsigned char* audio;
		size_t bytes_done;
		err = mpg123_decode_frame(mh, &frame_offset, &audio, &bytes_done);
		if (err == MPG123_NEW_FORMAT) {
			int encoding;
			err = mpg123_getformat(mh, &mp3->rate, &mp3->channels, &encoding);
			if (err != MPG123_NEED_MORE && err != MPG123_OK) {
				printerr("mpg123_getformat", err);
				return;
			}
			
			mp3->samples_per_frame = mpg123_spf(mh);
			mp3->secs_per_frame = mpg123_tpf(mh);
		}
		if (err != MPG123_OK && err != MPG123_NEED_MORE)
			__android_log_print(ANDROID_LOG_INFO, "podax-jni", "cannot get rate: %s", mpg123_plain_strerror(err));
	}
}


JNIEXPORT jlong JNICALL Java_com_axelby_podax_player_MPG123_openFile
	(JNIEnv *env, jclass c, jstring filename)
{
    int err = MPG123_OK;
    mpg123_handle *mh = mpg123_new(NULL, &err);
	if (err != MPG123_OK) {
		printerr("mpg123_new", err);
		return 0;
	}
	if (mh == NULL) {
		__android_log_write(ANDROID_LOG_INFO, "podax-jni", "mpg123_handle is NULL");
		return 0;
	}

	MP3File* mp3 = mp3file_init(mh);
	const char* fileString = (*env)->GetStringUTFChars(env, filename, NULL);
	err = mpg123_open(mh, fileString);
	if (err != MPG123_OK)
	{
		printerr("mpg123_open", err);
		(*env)->ReleaseStringUTFChars(env, filename, fileString);
		mp3file_delete(mp3);
		return err;
	}

	char* index_fn = malloc(strlen(fileString));
	strcpy(index_fn, fileString);
	// remove last path (Podcasts/)
	char* lastSlash = strrchr(index_fn, '/');
	char* lastDir = strstr(index_fn, "/Podcasts/");
	memmove(lastDir, lastSlash, index_fn + strlen(index_fn) - lastSlash + 1);
	strcat(index_fn, ".index");
	(*env)->ReleaseStringUTFChars(env, filename, fileString);

	// save / restore index for fast seeking
	size_t idx_fill;
	off_t idx_step;
	off_t* idx_offsets;
	FILE* index_file = fopen(index_fn, "rb");
	if (index_file != NULL) {
		fread(&idx_fill, sizeof(size_t), 1, index_file);
		fread(&idx_step, sizeof(off_t), 1, index_file);
		idx_offsets = malloc(idx_fill * sizeof(off_t));
		fread(idx_offsets, sizeof(off_t), idx_fill, index_file);
		fclose(index_file);
		mpg123_set_index(mh, idx_offsets, idx_step, idx_fill);
	} else {
		mpg123_scan(mh);
		mpg123_index(mh, &idx_offsets, &idx_step, &idx_fill);
		index_file = fopen(index_fn, "wb");
		fwrite(&idx_fill, sizeof(size_t), 1, index_file);
		fwrite(&idx_step, sizeof(off_t), 1, index_file);
		fwrite(idx_offsets, sizeof(off_t), idx_fill, index_file);
		fclose(index_file);
	}
	free(index_fn);

	// determine format and length
	mp3file_determineStats(mp3);
	return (jlong)mp3;
}

JNIEXPORT void JNICALL Java_com_axelby_podax_player_MPG123_delete
	(JNIEnv *env, jclass c, jlong handle)
{
	MP3File *mp3 = (MP3File*)handle;
	mp3file_delete(mp3);
}

JNIEXPORT jint JNICALL Java_com_axelby_podax_player_MPG123_readFrame
	(JNIEnv *env, jclass c, jlong handle, jshortArray out_buffer)
{
    MP3File *mp3 = (MP3File *)handle;
	mpg123_handle *mh = mp3->handle;

	off_t frame_offset;
	unsigned char* audio;
	size_t bytes_done;
	int err = mpg123_decode_frame(mh, &frame_offset, &audio, &bytes_done);
	if (err == MPG123_NEED_MORE)
		return -1;
	if (err == MPG123_DONE)
		return 0;
	if (err != MPG123_OK) {
		printerr("mpg123_decode_frame", err);
		return -2;
	}

	if (out_buffer == NULL || (*env)->GetArrayLength(env, out_buffer) < bytes_done / 2)
		out_buffer = (*env)->NewShortArray(env, bytes_done/2);
	short* c_array = (*env)->GetShortArrayElements(env, out_buffer, 0);
	memcpy(c_array, audio, bytes_done);
	(*env)->ReleaseShortArrayElements(env, out_buffer, c_array, 0);

	return bytes_done / 2;
}

JNIEXPORT jboolean JNICALL Java_com_axelby_podax_player_MPG123_skipFrame
	(JNIEnv *env, jclass c, jlong handle)
{
    MP3File *mp3 = (MP3File *)handle;
	mpg123_handle *mh = mp3->handle;

	off_t frame_offset;
	unsigned char* audio;
	size_t bytes_done;
	int err = mpg123_decode_frame(mh, &frame_offset, &audio, &bytes_done);
	return (err == MPG123_OK) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_com_axelby_podax_player_MPG123_seek
	(JNIEnv *env, jclass c, jlong handle, jfloat seconds)
{
    MP3File *mp3 = (MP3File *)handle;
    int err = mpg123_seek_frame(mp3->handle, (int) (seconds / mp3->secs_per_frame), SEEK_SET);
	if (err < 0)
		printerr("mpg123_seek_frame", err);
    return err;
}

JNIEXPORT float JNICALL Java_com_axelby_podax_player_MPG123_getPosition
	(JNIEnv *env, jclass c, jlong handle)
{
    MP3File *mp3 = (MP3File *)handle;
    return mpg123_tellframe(mp3->handle) * mp3->secs_per_frame;
}

JNIEXPORT jint JNICALL Java_com_axelby_podax_player_MPG123_getNumChannels
	(JNIEnv *env, jclass c, jlong handle)
{
    MP3File *mp3 = (MP3File *)handle;
	if (mp3->channels == 0)
		mp3file_determineStats(mp3);
    return mp3->channels;
}

JNIEXPORT jint JNICALL Java_com_axelby_podax_player_MPG123_getRate
	(JNIEnv *env, jclass c, jlong handle)
{
    MP3File *mp3 = (MP3File *)handle;
	if (mp3->rate == 0)
		mp3file_determineStats(mp3);
    return mp3->rate;
}

JNIEXPORT jlong JNICALL Java_com_axelby_podax_player_MPG123_getNumFrames
	(JNIEnv *env, jclass c, jlong handle)
{
    MP3File *mp3 = (MP3File *)handle;
    return mp3->num_frames;
}

JNIEXPORT jfloat JNICALL Java_com_axelby_podax_player_MPG123_getDuration
	(JNIEnv *env, jclass c, jlong handle)
{
    MP3File *mp3 = (MP3File *)handle;
	if (mp3->duration == 0)
		mp3file_determineStats(mp3);
    return mp3->duration;
}

JNIEXPORT jdouble JNICALL Java_com_axelby_podax_player_MPG123_getSecondsPerFrame
	(JNIEnv *env, jclass c, jlong handle)
{
    MP3File *mp3 = (MP3File *)handle;
    return mp3->secs_per_frame;
}

JNIEXPORT jint JNICALL Java_com_axelby_podax_player_MPG123_getSeekFrameOffset
	(JNIEnv *env, jclass c, jlong mp3file, jfloat seconds)
{
    MP3File *mp3 = (MP3File *)mp3file;
	mpg123_handle *mh = mp3->handle;

	size_t fill;
	off_t step;
	off_t* offsets;
	mpg123_index(mh, &offsets, &step, &fill);

	int target_frame = (int) (seconds / mp3->secs_per_frame); // the frame number to seek to
	int target_index = target_frame / step; // the closest index to the target frame
	// say so if there aren't enough entries in the index
	if (target_index >= fill)
		return -1;
	return offsets[target_index];
}

