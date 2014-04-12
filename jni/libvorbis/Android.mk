LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_MODULE     := vorbis
LOCAL_ARM_MODE   := arm
LOCAL_LDLIBS     := -llog
LOCAL_CFLAGS     := -D_ARM_ASSEM_ \
					-Wno-int-to-pointer-cast \
					-Wno-pointer-to-int-cast

LOCAL_SRC_FILES  := com_axelby_mp3decoders_Vorbis.c
LOCAL_SRC_FILES  += block.c
LOCAL_SRC_FILES  += codebook.c
LOCAL_SRC_FILES  += floor0.c
LOCAL_SRC_FILES  += floor1.c
LOCAL_SRC_FILES  += info.c
LOCAL_SRC_FILES  += mapping0.c
LOCAL_SRC_FILES  += mdct.c
LOCAL_SRC_FILES  += registry.c
LOCAL_SRC_FILES  += res012.c
LOCAL_SRC_FILES  += sharedbook.c
LOCAL_SRC_FILES  += synthesis.c
LOCAL_SRC_FILES  += vorbisfile.c
LOCAL_SRC_FILES  += window.c
LOCAL_SRC_FILES  += ogg/framing.c
LOCAL_SRC_FILES  += ogg/bitwise.c

include $(BUILD_SHARED_LIBRARY)

