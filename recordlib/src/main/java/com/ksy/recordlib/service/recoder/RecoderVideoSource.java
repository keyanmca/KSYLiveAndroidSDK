package com.ksy.recordlib.service.recoder;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.SurfaceView;

import com.ksy.recordlib.service.core.KsyMediaSource;
import com.ksy.recordlib.service.core.KsyRecordClientConfig;
import com.ksy.recordlib.service.util.Constants;
import com.ksy.recordlib.service.util.FileUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by eflakemac on 15/6/19.
 */
public class RecoderVideoSource extends KsyMediaSource implements MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener {
    private Camera mCamera;
    private SurfaceView mSurefaceView;
    private MediaRecorder mRecorder;
    private KsyRecordClientConfig mConfig;
    private ParcelFileDescriptor[] piple;
    private FileInputStream is;
    private boolean mRunning = false;
    private String path;

    public RecoderVideoSource(Camera mCamera, KsyRecordClientConfig mConfig, SurfaceView mSurfaceView) {
        this.mCamera = mCamera;
        this.mConfig = mConfig;
        this.mSurefaceView = mSurfaceView;
        mRecorder = new MediaRecorder();
    }

    @Override
    public void prepare() {
        mRecorder.setCamera(mCamera);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
//        profile.videoFrameWidth = 640;
//        profile.videoFrameHeight = 480;
//        mRecorder.setProfile(profile);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mRecorder.setVideoFrameRate(mConfig.getVideoFrameRate());
        mRecorder.setOutputFile(FileUtil.getOutputMediaFile(Constants.MEDIA_TYPE_VIDEO));
        try {
            this.piple = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            e.printStackTrace();
            release();
        }
//        mRecorder.setOutputFile(this.piple[1].getFileDescriptor());
        try {
            mRecorder.setOnInfoListener(this);
            mRecorder.setOnErrorListener(this);
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            release();
        }
    }

    @Override
    public void start() {
        if (!mRunning) {
            mRunning = true;
            prepare();
//            this.thread = new Thread(this);
//            this.thread.start();
        }
    }

    @Override
    public void stop() {
        if (mRunning == true) {
            release();
            mRunning = false;
        }
    }

    @Override
    public void release() {
        releaseRecoder();
        releaseCamera();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void releaseRecoder() {
        if (mRecorder != null) {
            Log.d(Constants.LOG_TAG, "mRecorder reset begin");
            mRecorder.reset();
            Log.d(Constants.LOG_TAG, "mRecorder reset over");
            mRecorder.release();
            Log.d(Constants.LOG_TAG, "mRecorder release");
            mRecorder = null;
            Log.d(Constants.LOG_TAG, "mRecorder complete");
            mCamera.lock();
        }
    }

    @Override
    public void run() {
//        is = new FileInputStream(this.piple[0].getFileDescriptor());
//        while (mRunning) {
//            byte[] buffer = new byte[10 * 1024 * 1024];
//            for (int i = 0; i < 20 * 1024; i++) {
//
//                try {
//                    byte data = (byte) is.read();
//                    buffer[i] = data;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            Log.d(Constants.LOG_TAG, "result = ");
//            char[] result = getChars(buffer);
//            String result_str = String.valueOf(result);
//            StringBuffer stringBuffer = new StringBuffer();
//            for (int i = 0; i < buffer.length; i++) {
//                stringBuffer.append(buffer[i]).append(",");
//            }
//            String result_str = stringBuffer.toString();
//            path = FileUtil.getOutputMediaFile(Constants.MEDIA_TYPE_TXT);
//            createFile(path, buffer);
//            Log.d(Constants.LOG_TAG, "write data over");
//            break;
//        }
    }

    public void createFile(String path, byte[] content) {
        try {
            FileOutputStream outputStream = new FileOutputStream(path);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            bufferedOutputStream.write(content);
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        Log.d(Constants.LOG_TAG, "onInfo Message what = " + what + ",extra =" + extra);
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.d(Constants.LOG_TAG, "onError Message what = " + what + ",extra =" + extra);
    }
}
