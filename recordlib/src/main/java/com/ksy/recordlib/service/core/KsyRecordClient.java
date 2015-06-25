package com.ksy.recordlib.service.core;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;

import com.ksy.recordlib.service.exception.KsyRecordException;
import com.ksy.recordlib.service.recoder.RecoderVideoSource;
import com.ksy.recordlib.service.recoder.RecoderVideoTempSource;
import com.ksy.recordlib.service.rtmp.KSYRtmpFlvClient;
import com.ksy.recordlib.service.util.Constants;

import java.io.IOException;

/**
 * Created by eflakemac on 15/6/17.
 */
public class KsyRecordClient implements KsyRecord {
    private static KsyRecordClient mInstance;
    private RecordHandler mRecordHandler;
    private Context mContext;
    private int mEncodeMode = Constants.ENCODE_MODE_MEDIA_RECORDER;
    private static KsyRecordClientConfig mConfig;
    private Camera mCamera;
    private KSYRtmpFlvClient mKsyRtmpFlvClient;
    private SurfaceView mSurfaceView;
    private TextureView mTextureView;
    private KsyMediaSource mVideoSource;
    private KsyMediaSource mAudioSource;
    private KsyMediaSource mVideoTempSource;

    private KsyRecordClient() {
    }

    private KsyRecordClient(Context context) {
        this.mContext = context;
        mRecordHandler = new RecordHandler();
    }


    public static KsyRecordClient getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new KsyRecordClient(context);
            mConfig = new KsyRecordClientConfig();
        }
        return mInstance;
    }

    /*
    *
    * Ks3 Record API
    * */

    @Override
    public void startRecord() throws KsyRecordException {
        mEncodeMode = judgeEncodeMode(mContext);
        if (checkConfig()) {
            // Here we begin
            if (mEncodeMode == Constants.ENCODE_MODE_MEDIA_RECORDER) {
                setUpMp4Config(mRecordHandler);
            } else {
                startRecordStep();
            }

        } else {
            throw new KsyRecordException("Check KsyRecordClient Configuration, param should be correct");
        }

    }

    private void startRecordStep() {
        // startRtmpFlvClient();
        setUpCamera(true);
        setUpEncoder();
    }


    private void setUpMp4Config(RecordHandler mRecordHandler) {
        setUpCamera(true);
        if (mVideoTempSource == null) {
            mVideoTempSource = new RecoderVideoTempSource(mCamera, mConfig, mSurfaceView, mRecordHandler,mContext);
            mVideoTempSource.start();
        }
    }

    private void startRtmpFlvClient() throws KsyRecordException {
        mKsyRtmpFlvClient = new KSYRtmpFlvClient(mConfig.getUrl());
        try {
            mKsyRtmpFlvClient.start();
        } catch (IOException e) {
            throw new KsyRecordException("Start muxer failed");
        }
    }

    private boolean checkConfig() throws KsyRecordException {
        return mConfig.validateParam();
    }


    private void setUpCamera(boolean needPreview) {
        if (mCamera == null) {
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(90);
            if (mConfig.getDisplayType() == Constants.DISPLAY_SURFACE_VIEW) {
                try {
                    if (needPreview) {
                        mCamera.setPreviewDisplay(mSurfaceView.getHolder());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    if (needPreview) {
                        mCamera.setPreviewTexture(mTextureView.getSurfaceTexture());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mCamera.unlock();
        }
    }

    private void setUpEncoder() {
        switch (mEncodeMode) {
            case Constants.ENCODE_MODE_MEDIA_RECORDER:
                DealWithMediaRecorder();
                break;
            case Constants.ENCODE_MODE_MEDIA_CODEC:
                DealWithMediaCodec();
                break;
            case Constants.ENCODE_MODE_WEBRTC:
                DealWithWebRTC();
                break;
            default:
                break;
        }
    }

    // Encode using MediaRecorder
    private void DealWithMediaRecorder() {
        Log.d(Constants.LOG_TAG, "DealWithMediaRecorder");
        // Video Source
        if (mVideoSource == null) {
            mVideoSource = new RecoderVideoSource(mCamera, mConfig, mSurfaceView,mRecordHandler,mContext);
            mVideoSource.start();
        }
        // Audio Source
        if (mAudioSource == null) {
//            mAudioSource = new RecoderAudioSource(mConfig);
//            mAudioSource.start();
        }

    }

    // Encode using MediaCodec
    // to do
    private void DealWithMediaCodec() {
        Log.d(Constants.LOG_TAG, "DealWithMediaCodec");

    }

    // Encode using WebRTC
    // to do
    private void DealWithWebRTC() {
        Log.d(Constants.LOG_TAG, "DealWithWebRTC");

    }

    private int judgeEncodeMode(Context mContext) {
        // to do
        return Constants.ENCODE_MODE_MEDIA_RECORDER;
    }


    @Override
    public void stopRecord() {
        if (mVideoSource != null) {
            mVideoSource.stop();
            mCamera = null;
            mVideoSource = null;
        }
        if (mVideoTempSource != null) {
            mVideoTempSource.stop();
            mCamera = null;
            mVideoTempSource = null;
        }
    }

    @Override
    public void release() {
        if (mVideoSource != null) {
            mVideoSource.release();
            mCamera = null;
            mVideoSource = null;
        }
        if (mVideoTempSource != null) {
            mVideoTempSource.stop();
            mCamera = null;
            mVideoTempSource = null;
        }
    }

    @Override
    public void setCameraType(int cameraType) {
        mConfig.setCameraType(cameraType);
    }

    @Override
    public void setVoiceType(int voiceType) {
        mConfig.setVoiceType(voiceType);
    }

    @Override
    public void setAudioEncodeConfig(int audioSampleRate, int audioBitRate) {
        mConfig.setAudioSampleRate(audioSampleRate);
        mConfig.setAudioBitRate(audioBitRate);
    }

    @Override
    public void setVideoEncodeConfig(int videoFrameRate, int videoBitRate) {
        mConfig.setVideoBitRate(videoBitRate);
        mConfig.setVideoFrameRate(videoFrameRate);
    }

    @Override
    public void setVideoResolution(int vResolutionType) {
        mConfig.setVideoResolutionType(vResolutionType);
    }

    @Override
    public void setUrl(String url) {
        mConfig.setUrl(url);
    }

    @Override
    public int getNewtWorkStatusType() {
        return 0;
    }

    @Override
    public void setDropFrameFrequency(int frequency) {
        mConfig.setDropFrameFrequency(frequency);
    }

    @Override
    public void setDisplayPreview(SurfaceView surfaceView) {
        this.mSurfaceView = surfaceView;
        mConfig.setDisplayType(Constants.DISPLAY_SURFACE_VIEW);
    }

    @Override
    public void setDisplayPreview(TextureView textureView) {
        this.mTextureView = textureView;
        mConfig.setDisplayType(Constants.DISPLAY_TEXTURE_VIEW);
    }

    public class RecordHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.MESSAGE_MP4CONFIG_FINISH:
                    Log.d(Constants.LOG_TAG, "back to continue");
                    release();
                    startRecordStep();
                    break;
                case Constants.MESSAGE_MP4CONFIG_START_PREVIEW:
                    break;
                default:
                    break;
            }
        }
    }

}
