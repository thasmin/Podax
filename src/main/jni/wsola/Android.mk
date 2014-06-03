#
#	Slaut - Slamnig Audio Utilities project
#
#	Android.mk
#
#	Created: 2014/05/18 D.Slamnig
#

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LDLIBS := -llog
LOCAL_MODULE  := wsola

LOCAL_SRC_FILES := wsola-jni.c wsola.c

include $(BUILD_SHARED_LIBRARY)

