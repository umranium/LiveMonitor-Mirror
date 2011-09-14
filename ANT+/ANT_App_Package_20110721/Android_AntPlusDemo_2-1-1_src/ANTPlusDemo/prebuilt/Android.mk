LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := user # should be optional, but launcher crashes without this

LOCAL_MODULE := ANTPlusDemo
LOCAL_SRC_FILES := bin/$(LOCAL_MODULE)-release.apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)

include $(BUILD_PREBUILT)
