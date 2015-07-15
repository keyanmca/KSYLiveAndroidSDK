package com.ksy.recordlib.service.core;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;

import com.ksy.recordlib.service.exception.KsyRecordException;
import com.ksy.recordlib.service.recoder.RecoderAudioSource;
import com.ksy.recordlib.service.recoder.RecoderVideoSource;
import com.ksy.recordlib.service.recoder.RecoderVideoTempSource;
import com.ksy.recordlib.service.rtmp.KSYRtmpFlvClient;
import com.ksy.recordlib.service.util.Constants;

import java.io.IOException;
import java.util.List;

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

    private KsyRecordSender ksyRecordSender;

    private KsyRecordClient() {
    }

    private KsyRecordClient(Context context) {
        this.mContext = context;
        mRecordHandler = new RecordHandler();

        ksyRecordSender = KsyRecordSender.getRecordInstance();
    }


    public static KsyRecordClient getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new KsyRecordClient(context);
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

        try {
            ksyRecordSender.start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(Constants.LOG_TAG, "startRecord() : e =" + e);
        }

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
            mVideoTempSource = new RecoderVideoTempSource(mCamera, mConfig, mSurfaceView, mRecordHandler, mContext);
            mVideoTempSource.start();
        }
    }

    private void startRtmpFlvClient() throws KsyRecordException {
        mKsyRtmpFlvClient = new KSYRtmpFlvClient(mConfig.getUrl());
        try {
            mKsyRtmpFlvClient.start();
        } catch (IOException e) {
            throw new KsyRecordException("start muxer failed");
        }
    }

    private boolean checkConfig() throws KsyRecordException {
        if (mConfig == null) {
            throw new KsyRecordException("should set KsyRecordConfig first");
        }
        return mConfig.validateParam();
    }


    private void setUpCamera(boolean needPreview) {
        if (mCamera == null) {
            int numberOfCameras = Camera.getNumberOfCameras();
            if (numberOfCameras > 0) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < numberOfCameras; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == mConfig.getCameraType()) {
                        mCamera = Camera.open(i);
                    }
                }
            } else {
                mCamera = Camera.open();
            }
            mCamera.setDisplayOrientation(90);
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes,
                    mSurfaceView.getWidth(), mSurfaceView.getHeight());
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            if (parameters.getSupportedFocusModes().contains(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            mCamera.setParameters(parameters);
            if (needPreview) {
                try {
                    if (mSurfaceView != null) {
                        mCamera.setPreviewDisplay(mSurfaceView.getHolder());
                    } else if (mTextureView != null) {
                        mCamera.setPreviewTexture(mTextureView.getSurfaceTexture());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // Here we reuse camera, just unlock it
        mCamera.unlock();
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
            mVideoSource = new RecoderVideoSource(mCamera, mConfig, mSurfaceView, mRecordHandler, mContext);
            mVideoSource.start();
        }

        // Audio Source
        if (mAudioSource == null) {
            mAudioSource = new RecoderAudioSource(mConfig, mRecordHandler, mContext);
            mAudioSource.start();
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

    private int judgeEncodeMode(Context context) {
        // to do
        return Constants.ENCODE_MODE_MEDIA_RECORDER;
    }


    @Override
    public void stopRecord() {
        if (mVideoSource != null) {
            mVideoSource.stop();
            mVideoSource = null;
        }
        if (mVideoTempSource != null) {
            mVideoTempSource.stop();
            mVideoTempSource = null;
        }
        if (mAudioSource != null) {
            mAudioSource.stop();
            mAudioSource = null;
        }
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

        ksyRecordSender.disconnect();
    }

    @Override
    public void release() {
        if (mVideoSource != null) {
            mVideoSource.release();
            mCamera = null;
            mVideoSource = null;
        }
        if (mVideoTempSource != null) {
            mVideoTempSource.release();
            mCamera = null;
            mVideoTempSource = null;
        }
        if (mAudioSource != null) {
            mAudioSource.release();
            mAudioSource = null;
        }
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public int getNewtWorkStatusType() {
        return 0;
    }

    @Override
    public void setDisplayPreview(SurfaceView surfaceView) {
        if (mConfig == null) {
            throw new IllegalStateException("should set KsyRecordConfig before invoke setDisplayPreview");
        }
        this.mSurfaceView = surfaceView;
        this.mTextureView = null;
    }

    @Override
    public void setDisplayPreview(TextureView textureView) {
        if (mConfig == null) {
            throw new IllegalStateException("should set KsyRecordConfig before invoke setDisplayPreview");
        }
        this.mTextureView = textureView;
        this.mSurfaceView = null;
    }

    public class RecordHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.MESSAGE_MP4CONFIG_FINISH:
                    Log.d(Constants.LOG_TAG, "back to continue");
                    //release();
                    // just release tem
                    if (mVideoTempSource != null) {
                        // already release
                        mVideoTempSource.release();
                        mVideoTempSource = null;
                    }
                    startRecordStep();
                    break;
                case Constants.MESSAGE_MP4CONFIG_START_PREVIEW:
                    break;
                default:
                    break;
            }
        }
    }

    public static void setConfig(KsyRecordClientConfig mConfig) {
        KsyRecordClient.mConfig = mConfig;
    }
}
