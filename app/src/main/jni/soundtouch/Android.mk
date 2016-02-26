# Copyright (C) 2010 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# $Id: Android.mk 165 2012-12-28 19:55:23Z oparviai $

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := soundtouch

LOCAL_ARM_MODE  := arm

LOCAL_SRC_FILES := soundtouch-jni.cpp
LOCAL_SRC_FILES += AAFilter.cpp
LOCAL_SRC_FILES += BPMDetect.cpp
LOCAL_SRC_FILES += FIFOSampleBuffer.cpp
LOCAL_SRC_FILES += FIRFilter.cpp
LOCAL_SRC_FILES += InterpolateCubic.cpp
LOCAL_SRC_FILES += InterpolateLinear.cpp
LOCAL_SRC_FILES += InterpolateShannon.cpp
LOCAL_SRC_FILES += PeakFinder.cpp
LOCAL_SRC_FILES += RateTransposer.cpp
LOCAL_SRC_FILES += SoundTouch.cpp
LOCAL_SRC_FILES += TDStretch.cpp
LOCAL_SRC_FILES += cpu_detect_x86.cpp
LOCAL_SRC_FILES += sse_optimized.cpp
LOCAL_SRC_FILES += mmx_optimized.cpp

# for logging
LOCAL_LDLIBS    += -llog
# for native asset manager
#LOCAL_LDLIBS    += -landroid

#LOCAL_CFLAGS == -Wall
# don't export all symbols
LOCAL_CFLAGS += -fvisibility=hidden -D ST_NO_EXCEPTION_HANDLING

# necessary until min API is 21 and we can write floats to the audio track
LOCAL_CFLAGS += -DSOUNDTOUCH_INTEGER_SAMPLES


include $(BUILD_SHARED_LIBRARY)
