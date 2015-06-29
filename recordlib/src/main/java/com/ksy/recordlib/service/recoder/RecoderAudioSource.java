package com.ksy.recordlib.service.recoder;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.ksy.recordlib.service.core.KsyMediaSource;
import com.ksy.recordlib.service.core.KsyRecordClient;
import com.ksy.recordlib.service.core.KsyRecordClientConfig;
import com.ksy.recordlib.service.util.Constants;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by eflakemac on 15/6/19.
 */
public class RecoderAudioSource extends KsyMediaSource implements MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {
    private KsyRecordClient.RecordHandler mRecordHandler;
    private Context mContext;
    private KsyRecordClientConfig mConfig;
    private MediaRecorder mRecorder;
    private ParcelFileDescriptor[] piple;
    private boolean mRunning;

    public RecoderAudioSource(KsyRecordClientConfig mConfig, KsyRecordClient.RecordHandler mRecordHandler, Context mContext) {
        super(mConfig.getUrl());
        this.mConfig = mConfig;
        this.mRecordHandler = mRecordHandler;
        this.mContext = mContext;
        mRecorder = new MediaRecorder();
    }

    @Override
    public void prepare() {
        mRecorder.setOnErrorListener(this);
        mRecorder.setOnInfoListener(this);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
//        mRecorder.setAudioChannels();
        mRecorder.setAudioSamplingRate(mConfig.getAudioSampleRate());
        mRecorder.setAudioEncodingBitRate(mConfig.getAudioBitRate());
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        try {
            this.piple = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            e.printStackTrace();
            release();
        }
        mRecorder.setOutputFile(this.piple[1].getFileDescriptor());
        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
        }
    }

    @Override
    public void start() {
        if (!mRunning) {
            mRunning = true;
            this.thread = new Thread(this);
            this.thread.start();
        }
    }

    @Override
    public void stop() {
        if (mRunning == true) {
            release();
        }
    }

    @Override
    public void release() {
        mRunning = false;
        releaseRecorder();
    }

    private void releaseRecorder() {
        if (mRecorder != null) {
            mRecorder.setOnErrorListener(null);
            mRecorder.setOnInfoListener(null);
            mRecorder.reset();
            Log.d(Constants.LOG_TAG, "mRecorder reset");
            mRecorder.release();
            Log.d(Constants.LOG_TAG, "mRecorder release");
            mRecorder = null;
            Log.d(Constants.LOG_TAG, "mRecorder complete");
        }
    }

    @Override
    public void run() {
        prepare();
        is = new FileInputStream(this.piple[0].getFileDescriptor());
        while (mRunning) {
//            Log.d(Constants.LOG_TAG, "entering audio loop");
           /* try {
                while (true) {
                    Log.d(Constants.LOG_TAG, "audio read =" + is.read());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.d(Constants.LOG_TAG, "onError Message what = " + what + ",extra =" + extra);

    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        Log.d(Constants.LOG_TAG, "onInfo Message what = " + what + ",extra =" + extra);

    }
}
