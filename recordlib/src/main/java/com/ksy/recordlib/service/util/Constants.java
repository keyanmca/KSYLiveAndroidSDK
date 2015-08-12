package com.ksy.recordlib.service.util;

/**
 * Created by eflakemac on 15/6/17.
 */
public class Constants {
    public static final String LOG_TAG = "ksy-record-sdk";
    // Voice Encode Default
//    public static final String URL_DEFAULT = "rtmp://xiaoyi.uplive.ksyun.com:80/live/hansen2?public=1&expire=1710333230&nonce=12341234&accesskey=2HITWMQXL2VBB3XMAEHQ&signature=1wgD2F56CDUizTp0%2fj3DJ%2fasSsY%3d";
//    public static final String URL_DEFAULT = "rtmp://115.231.96.121:80/live/hansen223?public=1&expire=1710333230&nonce=12341234&accesskey=2HITWMQXL2VBB3XMAEHQ&signature=1wgD2F56CDUizTp0%2fj3DJ%2fasSsY%3d";
    public static final String URL_DEFAULT = "rtmp://192.168.135.185/myTestLive/eflakee2330";
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

    //voice config
    public static final int CONFIG_AUDIO_SAMPLERATE_44100 = 44100;

    public static final int CONFIG_AUDIO_BITRATE_32K = 32 * 1000;
    public static final int CONFIG_AUDIO_BITRATE_48K = 48 * 1000;
    public static final int CONFIG_AUDIO_BITRATE_64K = 64 * 1000;

    public static final int CONFIG_VOICE_TYPE_MIC = 0;

    //video config
    public static final int CONFIG_CAMERA_TYPE_BACK = 0;
    public static final int CONFIG_CAMERA_TYPE_FRONT = 1;

    public static final int CONFIG_VIDEO_BITRATE_250K = 250 * 1000;
    public static final int CONFIG_VIDEO_BITRATE_500K = 500 * 1000;
    public static final int CONFIG_VIDEO_BITRATE_750K = 750 * 1000;
    public static final int CONFIG_VIDEO_BITRATE_1000K = 1000 * 1000;
    public static final int CONFIG_VIDEO_BITRATE_1500K = 1500 * 1000;

    public static final int CONFIG_VIDEO_FRAME_RATE_10 = 10;
    public static final int CONFIG_VIDEO_FRAME_RATE_15 = 15;
    public static final int CONFIG_VIDEO_FRAME_RATE_21 = 21;
    public static final int CONFIG_VIDEO_FRAME_RATE_30 = 30;

    public static final String NETWORK_STATE_CHANGED = "net_work_changed";

    public static final int SETTING_URL = 0;
    public static final int SETTING_CAMERY_TYPE = 1;
    public static final int SETTING_VIDEO_SIZE = 2;
    public static final int SETTING_VIDEO_BITRATE = 3;
    public static final int SETTING_AUDIO_BITRATE = 4;
    public static final int SETTING_AUDIO_SAMPLE_RATE = 5;

}
