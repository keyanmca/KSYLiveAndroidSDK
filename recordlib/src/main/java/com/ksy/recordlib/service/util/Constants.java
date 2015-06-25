package com.ksy.recordlib.service.util;

/**
 * Created by eflakemac on 15/6/17.
 */
public class Constants {
    public static final String LOG_TAG = "ksy-record-sdk";
    // Camera Type
    public static final int CAMERA_TYPE_BACK = 0;
    public static final int CAMERA_TYPE_FRONT = 1;
    // Voice Type
    public static final int VOICE_TYPE_MIC = 0;
    // Voice Encode Default
    public static final int AUDIO_SAMPLE_RATE_DEFAULT = 8000;
    public static final int AUDIO_BIT_RATE_DEFAULT = 32000;
    public static final int VIDEO_FRAME_RATE_DEFAULT = 20;
    public static final int VIDEO_BIT_RATE_DEFAULT = 5000000;
    public static final int QUALITY_240P = 7;
    public static final int QUALITY_480P = 4;
    public static final int QUALITY_720P = 5;
    public static final int QUALITY_1080P = 6;
    public static final String URL_DEFAULT = "192.168.135.242/ksy/eflake";
    public static final int ENCODE_MODE_MEDIA_RECORDER = 0;
    public static final int ENCODE_MODE_MEDIA_CODEC = 1;
    public static final int ENCODE_MODE_WEBRTC = 2;
    public static final int DISPLAY_SURFACE_VIEW = 0;
    public static final int DISPLAY_TEXTURE_VIEW = 1;
    public static final int MEDIA_TYPE_IMAGE = 0;
    public static final int MEDIA_TYPE_VIDEO = 1;
    public static final int MEDIA_TYPE_TXT = 2;
    public static final int MESSAGE_MP4CONFIG_FINISH = 0;
    public static final int MESSAGE_MP4CONFIG_START_PREVIEW = 1;
    public static final String PREFERENCE_KEY_MP4CONFIG_PROFILE_LEVEL = "profile_level";
    public static final String PREFERENCE_KEY_MP4CONFIG_B64PPS = "b64pps";
    public static final String PREFERENCE_KEY_MP4CONFIG_B64SPS = "b64sps";
    public static final String PREFERENCE_NAME = "preference_key";
}
