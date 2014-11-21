LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_MODULE     := mpg123
LOCAL_ARM_MODE   := arm
LOCAL_LDLIBS     := -llog

LOCAL_SRC_FILES := 	podax_MPG123.c
LOCAL_SRC_FILES +=  libmpg123.c
LOCAL_SRC_FILES +=  compat.c
LOCAL_SRC_FILES +=  frame.c
LOCAL_SRC_FILES +=  id3.c
LOCAL_SRC_FILES +=  format.c
LOCAL_SRC_FILES +=  stringbuf.c
LOCAL_SRC_FILES +=  readers.c
LOCAL_SRC_FILES +=  icy.c icy2utf8.c
LOCAL_SRC_FILES +=  index.c
LOCAL_SRC_FILES +=  layer1.c layer2.c layer3.c
LOCAL_SRC_FILES +=  parse.c
LOCAL_SRC_FILES +=  optimize.c
LOCAL_SRC_FILES +=  synth.c synth_8bit.c
LOCAL_SRC_FILES +=  ntom.c
LOCAL_SRC_FILES +=  dct64.c
LOCAL_SRC_FILES +=  equalizer.c
LOCAL_SRC_FILES +=  tabinit.c
LOCAL_SRC_FILES +=  feature.c

ifeq ($(TARGET_ARCH_ABI),armeabi)
LOCAL_CFLAGS     := -DACCURATE_ROUNDING \
					-DOPT_ARM \
					-DREAL_IS_FIXED \
					-DNO_REAL \
					-DNO_32BIT \
					-DHAVE_STRERROR \
					-DASMALIGN_BYTE \
					-Wno-int-to-pointer-cast \
					-Wno-pointer-to-int-cast \
					-ffast-math -O3

LOCAL_SRC_FILES +=  synth_arm.S
LOCAL_SRC_FILES +=  synth_arm_accurate.S
endif
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_CFLAGS     := -DACCURATE_ROUNDING \
					-DOPT_NEON \
					-DHAVE_STRERROR \
					-Wno-int-to-pointer-cast \
					-Wno-pointer-to-int-cast \
					-ffast-math -O3

LOCAL_SRC_FILES +=  synth_real.c synth_s32.c
LOCAL_SRC_FILES +=  synth_neon.S synth_neon_accurate.S synth_neon_float.S synth_neon_s32.S
LOCAL_SRC_FILES +=  dct36_neon.S dct64_neon_float.S synth_stereo_neon_accurate.S synth_stereo_neon_float.S synth_stereo_neon_s32.S
endif

ifeq ($(TARGET_ARCH_ABI),x86)
LOCAL_CFLAGS     := -DACCURATE_ROUNDING \
					-DHAVE_STRERROR \
					-DOPT_SSE \
					-Wno-int-to-pointer-cast \
					-Wno-pointer-to-int-cast \
					-ffast-math -O3

LOCAL_SRC_FILES +=  synth_real.c synth_s32.c
LOCAL_SRC_FILES +=  synth_sse.S synth_sse_accurate.S synth_sse_float.S synth_sse_s32.S
LOCAL_SRC_FILES +=  synth_stereo_sse_accurate.S synth_stereo_sse_float.S synth_stereo_sse_s32.S
LOCAL_SRC_FILES +=  dct64_i386.c dct36_sse.S dct64_sse.S dct64_sse_float.S
LOCAL_SRC_FILES +=  tabinit_mmx.S
endif

ifeq ($(TARGET_ARCH_ABI),x86_64)
LOCAL_CFLAGS     := -DACCURATE_ROUNDING \
					-DHAVE_STRERROR \
					-DOPT_X86_64 \
					-Wno-int-to-pointer-cast \
					-Wno-pointer-to-int-cast \
					-ffast-math -O3

LOCAL_SRC_FILES +=  synth_real.c synth_s32.c
LOCAL_SRC_FILES +=  getcpuflags_x86_64.S
LOCAL_SRC_FILES +=  synth_x86_64.S synth_x86_64_s32.S synth_x86_64_accurate.S synth_x86_64_float.S
LOCAL_SRC_FILES +=  synth_stereo_x86_64_float.S synth_stereo_x86_64.S synth_stereo_x86_64_s32.S synth_stereo_x86_64_accurate.S
LOCAL_SRC_FILES +=  dct36_x86_64.S dct64_x86_64.S dct64_x86_64_float.S
endif


include $(BUILD_SHARED_LIBRARY)
