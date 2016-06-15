
LOCAL_PATH := $(call my-dir)
CURRENT_PATH := $(LOCAL_PATH)

#include $(call all-subdir-makefiles)

GSTREAMER_ROOT := /home/ssitwell/gstreamer-arm

include $(CLEAR_VARS)
LOCAL_MODULE := ssl
LOCAL_SRC_FILES := /home/ssitwell/openssl-1.0.1g/libssl.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := crypto
LOCAL_SRC_FILES := /home/ssitwell/openssl-1.0.1g/libcrypto.so
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

$(info "???")
$(info $(LOCAL_C_INCLUDES))
$(info $(LOCAL_EXPORT_C_INCLUDES))
$(info "???")

include $(CLEAR_VARS)
$(info "!!!")
$(info $(LOCAL_CFLAGS))
$(info $(LOCAL_C_INCLUDES))
$(info $(LOCAL_EXPORT_C_INCLUDES))
$(info "!!!")
LOCAL_PATH := $(CURRENT_PATH)
LOCAL_MODULE    := tutorial-3
LOCAL_SRC_FILES := tutorial-3.c tcp_utils.c nice.c
LOCAL_SHARED_LIBRARIES := gstreamer_android ssl crypto
LOCAL_LDLIBS := -llog -landroid
LOCAL_CFLAGS := -DPBSSL -DPBTHREADS -I /usr/local/ssl/android-23/include
include $(BUILD_SHARED_LIBRARY)

