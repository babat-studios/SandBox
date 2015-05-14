LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include /ssd/sdk/OpenCV-2.4.10-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := detector
LOCAL_SRC_FILES := detector.cpp

include $(BUILD_SHARED_LIBRARY)
