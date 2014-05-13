LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_MODULE     := mpg123
LOCAL_ARM_MODE   := arm
LOCAL_LDLIBS     := -llog

ifeq ($(TARGET_ARCH),arm)
LOCAL_CFLAGS     := -DACCURATE_ROUNDING \
					-DOPT_ARM \
					-DREAL_IS_FIXED \
					-DNO_REAL \
					-DNO_32BIT \
					-DHAVE_STRERROR \
					-DASMALIGN_BYTE \
					-Wno-int-to-pointer-cast \
					-Wno-pointer-to-int-cast
endif
ifeq ($(TARGET_ARCH),x86)
LOCAL_CFLAGS     := -DACCURATE_ROUNDING \
					-DHAVE_STRERROR \
					-DOPT_SSE \
					-DREAL_IS_FLOAT \
					-DNO_REAL \
					-DNO_32BIT \
					-DASMALIGN_BYTE \
					-Wno-int-to-pointer-cast \
					-Wno-pointer-to-int-cast
endif

LOCAL_SRC_FILES := 	podax_MPG123.c
LOCAL_SRC_FILES +=  compat.c
LOCAL_SRC_FILES +=  frame.c
LOCAL_SRC_FILES +=  id3.c
LOCAL_SRC_FILES +=  format.c
LOCAL_SRC_FILES +=  stringbuf.c
LOCAL_SRC_FILES +=  libmpg123.c
LOCAL_SRC_FILES +=  readers.c
LOCAL_SRC_FILES +=  icy.c
LOCAL_SRC_FILES +=  icy2utf8.c
LOCAL_SRC_FILES +=  index.c
LOCAL_SRC_FILES +=  layer1.c
LOCAL_SRC_FILES +=  layer2.c
LOCAL_SRC_FILES +=  layer3.c
LOCAL_SRC_FILES +=  parse.c
LOCAL_SRC_FILES +=  optimize.c
LOCAL_SRC_FILES +=  synth.c
LOCAL_SRC_FILES +=  synth_8bit.c
LOCAL_SRC_FILES +=  ntom.c
LOCAL_SRC_FILES +=  dct64.c
LOCAL_SRC_FILES +=  equalizer.c
LOCAL_SRC_FILES +=  tabinit.c
LOCAL_SRC_FILES +=  feature.c

ifeq ($(TARGET_ARCH),arm)
LOCAL_SRC_FILES +=  synth_arm.S
LOCAL_SRC_FILES +=  synth_arm_accurate.S
endif
ifeq ($(TARGET_ARCH),x86)
LOCAL_SRC_FILES +=  synth_sse.S
LOCAL_SRC_FILES +=  synth_sse_accurate.S
LOCAL_SRC_FILES +=  synth_sse_float.S
LOCAL_SRC_FILES +=  dct64_sse.S
LOCAL_SRC_FILES +=  dct64_sse_float.S

LOCAL_SRC_FILES +=  tabinit_mmx.S
LOCAL_SRC_FILES +=  dct64_i386.c
LOCAL_SRC_FILES +=  dct36_sse.S
LOCAL_SRC_FILES +=  synth_stereo_sse_accurate.S
endif

include $(BUILD_SHARED_LIBRARY)
