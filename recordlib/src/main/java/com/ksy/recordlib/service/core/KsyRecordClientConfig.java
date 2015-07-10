package com.ksy.recordlib.service.core;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;

import com.ksy.recordlib.service.exception.KsyRecordException;

/**
 * Created by eflakemac on 15/6/17.
 */
public class KsyRecordClientConfig {

    public static int MEDIA_TEMP = 1;
    public static int MEDIA_SETP = 2;

    private int mCameraType;
    private int mVoiceType;
    private int mAudioSampleRate;
    private int mAudioBitRate;
    private int mAudioEncorder = MediaRecorder.AudioEncoder.AAC;
    private int mVideoFrameRate;
    private int mVideoBitRate;
    private int mDropFrameFrequency;
    private int mVideoWidth;
    private int mVideoHeigh;
    private int mDisplayType;
    private int mVideoEncorder = MediaRecorder.VideoEncoder.H264;
    private int mVideoProfile = -1;

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

    public int getDropFrameFrequency() {
        return mDropFrameFrequency;
    }

    public int getDisplayType() {
        return mDisplayType;
    }

    public int getmVideoWidth() {
        return mVideoWidth;
    }

    public void setmVideoWidth(int mVideoWidth) {
        this.mVideoWidth = mVideoWidth;
    }

    public int getmVideoHeigh() {
        return mVideoHeigh;
    }

    public void setmVideoHeigh(int mVideoHeigh) {
        this.mVideoHeigh = mVideoHeigh;
    }

    public int getmAudioEncorder() {
        return mAudioEncorder;
    }

    public void setmAudioEncorder(int mAudioEncorder) {
        this.mAudioEncorder = mAudioEncorder;
    }

    public String getUrl() {
        return mUrl;
    }

    public boolean validateParam() throws KsyRecordException {
        //to do
        return true;
    }

    public void configMediaRecorder(MediaRecorder mediaRecorder, int type) {
        if (mediaRecorder == null) {
            throw new IllegalArgumentException("mediaRecorder is null");
        }
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        if (type == MEDIA_SETP) {
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        } else if (type == MEDIA_TEMP) {
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        }
        mediaRecorder.setVideoEncoder(mVideoEncorder);
        if (mVideoProfile >= 0) {
            int cameraId = -1;
            int numberOfCameras = Camera.getNumberOfCameras();
            if (numberOfCameras > 0) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < numberOfCameras; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == mCameraType) {
                        cameraId = i;
                        break;
                    }
                }
            }
            if (cameraId < 0) {
                throw new IllegalArgumentException("camera unsupported quality level");
            }
            if (CamcorderProfile.hasProfile(cameraId, mVideoProfile)) {
                CamcorderProfile camcorderProfile = CamcorderProfile.get(cameraId, mVideoProfile);
                mediaRecorder.setVideoFrameRate(camcorderProfile.videoFrameRate);
                mediaRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
                mediaRecorder.setVideoSize(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
            }
        }
        if (mVideoBitRate > 0) {
            mediaRecorder.setVideoEncodingBitRate(mVideoBitRate);
        }
        if (mVideoWidth > 0 && mVideoHeigh > 0) {
            mediaRecorder.setVideoSize(mVideoWidth, mVideoHeigh);
        }
    }


    public void setVideoProfile(int profileID) {
        this.mVideoProfile = profileID;

    }


}
