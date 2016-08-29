LOCAL_PATH := $(call my-dir)
CURRENT_PATH := $(LOCAL_PATH)

#include $(call all-subdir-makefiles)


OPENSSL_ROOT := openssl-1.0.2

# might need to include the gstreamer libs in the jni folder, similar to openssl
GSTREAMER_ROOT := /home/ssitwell/gstreamer-arm

include $(CLEAR_VARS)
LOCAL_MODULE := ssl
LOCAL_SRC_FILES := $(OPENSSL_ROOT)/$(APP_ABI)/lib/libssl.so
LOCAL_EXPORT_C_INCLUDES += $(LOCAL_PATH)/$(OPENSSL_ROOT)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := crypto
LOCAL_SRC_FILES := $(OPENSSL_ROOT)/$(APP_ABI)/lib/libcrypto.so
LOCAL_EXPORT_CFLAGS := -DWAZA
include $(PREBUILT_SHARED_LIBRARY)

ifndef GSTREAMER_ROOT
ifndef GSTREAMER_ROOT_ANDROID
$(error GSTREAMER_ROOT_ANDROID is not defined!)
endif
GSTREAMER_ROOT        := $(GSTREAMER_ROOT_ANDROID)
endif
GSTREAMER_NDK_BUILD_PATH  := $(GSTREAMER_ROOT)/share/gst-android/ndk-build/
include $(GSTREAMER_NDK_BUILD_PATH)/plugins.mk
GSTREAMER_PLUGINS         := $(GSTREAMER_PLUGINS_CORE) $(GSTREAMER_PLUGINS_SYS) $(GSTREAMER_PLUGINS_NET) $(GSTREAMER_PLUGINS_CODECS_RESTRICTED) nice
GSTREAMER_EXTRA_DEPS      := gstreamer-video-1.0
include $(GSTREAMER_NDK_BUILD_PATH)/gstreamer-1.0.mk

include $(CLEAR_VARS)
LOCAL_PATH := $(CURRENT_PATH)
LOCAL_MODULE    := tutorial-3
LOCAL_SRC_FILES := tutorial-3.c pb.c tcp_utils.c nice_utils.c
LOCAL_SHARED_LIBRARIES := gstreamer_android ssl crypto
LOCAL_LDLIBS := -llog -landroid
LOCAL_CFLAGS := -std=c11 -DANDROID -DPBSSL -DPBTHREADS
include $(BUILD_SHARED_LIBRARY)
