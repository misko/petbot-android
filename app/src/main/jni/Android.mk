LOCAL_PATH := $(call my-dir)
CURRENT_PATH := $(LOCAL_PATH)

#include $(call all-subdir-makefiles)


OPENSSL_ROOT := openssl-1.0.2

# might need to include the gstreamer libs in the jni folder, similar to openssl
GSTREAMER_ANDROID_DIR := /home/ssitwell/gstreamer-android-bak
#GSTREAMER_ROOT := /Users/miskodzamba/research/petbot/petbot_2015/gstreamer-1.0-android-$(TARGET_ARCH)-1.9.1
#GSTREAMER_ANDROID_DIR := /Users/miskodzamba/research/petbot/petbot_2015/gstreamer-1.0-android-universal-1.9.90

#include $(CLEAR_VARS)
#LOCAL_MODULE := ssl
#LOCAL_SRC_FILES := $(OPENSSL_ROOT)/$(APP_ABI)/lib/libssl.so
#LOCAL_EXPORT_C_INCLUDES += $(LOCAL_PATH)/$(OPENSSL_ROOT)/include
#include $(PREBUILT_SHARED_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := crypto
#LOCAL_SRC_FILES := $(OPENSSL_ROOT)/$(APP_ABI)/lib/libcrypto.so
#LOCAL_EXPORT_CFLAGS := -DWAZA
#include $(PREBUILT_SHARED_LIBRARY)

# Prepare the SSL static library
include $(CLEAR_VARS)
LOCAL_MODULE := libssl-prebuilt
LOCAL_SRC_FILES := $(OPENSSL_ROOT)/$(APP_ABI)/lib/libssl.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(OPENSSL_ROOT)/include
include $(PREBUILT_STATIC_LIBRARY)

# Prepare the CRYPTO static library
include $(CLEAR_VARS)
LOCAL_MODULE := libcrypto-prebuilt
LOCAL_SRC_FILES := $(OPENSSL_ROOT)/$(APP_ABI)/lib/libcrypto.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(OPENSSL_ROOT)/include
include $(PREBUILT_STATIC_LIBRARY)

#GSTREAMER
# GSTREAMER_ROOT must be set for the gstreamer-android makefiles to work
ifndef GSTREAMER_ANDROID_DIR
$(error GSTREAMER_ANDROID_DIR is not defined!)
endif
ifeq ($(TARGET_ARCH_ABI),armeabi)
GSTREAMER_ROOT        := $(GSTREAMER_ANDROID_DIR)/arm
else ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
GSTREAMER_ROOT        := $(GSTREAMER_ANDROID_DIR)/armv7
else ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
GSTREAMER_ROOT        := $(GSTREAMER_ANDROID_DIR)/arm64
else ifeq ($(TARGET_ARCH_ABI),x86)
GSTREAMER_ROOT        := $(GSTREAMER_ANDROID_DIR)/x86
else ifeq ($(TARGET_ARCH_ABI),x86_64)
GSTREAMER_ROOT        := $(GSTREAMER_ANDROID_DIR)/x86_64
else
$(error Target arch ABI not supported: $(TARGET_ARCH_ABI))
endif
GSTREAMER_NDK_BUILD_PATH  := $(GSTREAMER_ROOT)/share/gst-android/ndk-build/
include $(GSTREAMER_NDK_BUILD_PATH)/plugins.mk
GSTREAMER_PLUGINS         := nice $(GSTREAMER_PLUGINS_CORE) $(GSTREAMER_PLUGINS_PLAYBACK) $(GSTREAMER_PLUGINS_CODECS) $(GSTREAMER_PLUGINS_NET) $(GSTREAMER_PLUGINS_SYS) $(GSTREAMER_CODECS_GPL) $(GSTREAMER_PLUGINS_ENCODING) $(GSTREAMER_PLUGINS_VIS) $(GSTREAMER_PLUGINS_EFFECTS) $(GSTREAMER_PLUGINS_NET_RESTRICTED) $(GSTREAMER_PLUGINS_CODECS_RESTRICTED)
GSTREAMER_EXTRA_DEPS      := gstreamer-player-1.0 gstreamer-video-1.0 glib-2.0
include $(GSTREAMER_NDK_BUILD_PATH)/gstreamer-1.0.mk

include $(CLEAR_VARS)
LOCAL_MODULE := PBConnector
LOCAL_SRC_FILES := PBConnector.c  tcp_utils.c nice_utils.c pb.h #pb.c
LOCAL_SHARED_LIBRARIES := gstreamer_android
#ssl crypto
LOCAL_STATIC_LIBRARIES := libssl-prebuilt libcrypto-prebuilt
LOCAL_LDLIBS := -llog -landroid
LOCAL_CFLAGS := -std=c11 -DANDROID -DPBSSL -DPBTHREADS
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_PATH := $(CURRENT_PATH)
LOCAL_MODULE    := pb_gst
LOCAL_SRC_FILES := pb_gst.c pb.h #pb.c
LOCAL_SHARED_LIBRARIES := gstreamer_android
LOCAL_LDLIBS := -llog -landroid
LOCAL_CFLAGS := -std=c11 -DANDROID
include $(BUILD_SHARED_LIBRARY)
