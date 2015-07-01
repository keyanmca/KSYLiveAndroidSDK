package com.ksy.recordlib.service.core;

import com.ksy.recordlib.service.exception.KsyRecordException;

/**
 * Created by eflakemac on 15/6/17.
 */
public class KsyRecordClientConfig {
    private int mCameraType;
    private int mVoiceType;
    private int mAudioSampleRate;
    private int mAudioBitRate;
    private int mVideoFrameRate = 30;
    private int mVideoBitRate;
    private int mVideoResolutionType;
    private int mDropFrameFrequency;
    private int mDisplayType;
    private String mUrl;

    public void setCameraType(int CameraType) {
        this.mCameraType = CameraType;
    }

    public void setVoiceType(int VoiceType) {
        this.mVoiceType = VoiceType;
    }

    public void setAudioSampleRate(int AudioSampleRate) {
        this.mAudioSampleRate = AudioSampleRate;
    }

    public void setAudioBitRate(int AudioBitRate) {
        this.mAudioBitRate = AudioBitRate;
    }

    public void setVideoFrameRate(int VideoFrameRate) {
        this.mVideoFrameRate = VideoFrameRate;
    }

    public void setVideoBitRate(int VideoBitRate) {
        this.mVideoBitRate = VideoBitRate;
    }

    public void setVideoResolutionType(int VideoResolutionType) {
        this.mVideoResolutionType = VideoResolutionType;
    }

    public void setDropFrameFrequency(int DropFrameFrequency) {
        this.mDropFrameFrequency = DropFrameFrequency;
    }

    public void setDisplayType(int DisplayType) {
        this.mDisplayType = DisplayType;
    }

    public void setUrl(String Url) {
        this.mUrl = Url;
    }

    public int getCameraType() {
        return mCameraType;
    }

    public int getVoiceType() {
        return mVoiceType;
    }

    public int getAudioSampleRate() {
        return mAudioSampleRate;
    }

    public int getAudioBitRate() {
        return mAudioBitRate;
    }

    public int getVideoFrameRate() {
        return mVideoFrameRate;
    }

    public int getVideoBitRate() {
        return mVideoBitRate;
    }

    public int getVideoResolutionType() {
        return mVideoResolutionType;
    }

    public int getDropFrameFrequency() {
        return mDropFrameFrequency;
    }

    public int getDisplayType() {
        return mDisplayType;
    }

    public String getUrl() {
        return mUrl;
    }

    public boolean validateParam() throws KsyRecordException {
        //to do
        return true;
    }


}
