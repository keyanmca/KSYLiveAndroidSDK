package com.ksy.recordlib.service.core;

import android.util.Log;

import com.ksy.recordlib.service.util.Constants;

import java.io.IOException;

/**
 * Created by eflakemac on 15/6/26.
 */
public class KsyRecordSender {
    //	@AccessedByNative
    public long mNativeRTMP;

    static {
        System.loadLibrary("rtmp");
        Log.i(Constants.LOG_TAG, "rtmp.so loaded");
        System.loadLibrary("ksyrtmp");
        Log.i(Constants.LOG_TAG, "ksyrtmp.so loaded");
    }

    private Thread worker;

    public KsyRecordSender(String url) {
        _set_output_url(url);
        _open();
    }



    public void start() throws IOException {

        worker = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    cycle();
                } catch (Exception e) {
                    Log.i(Constants.LOG_TAG, "worker: thread exception.");
                    e.printStackTrace();
                }
            }
        });
        worker.start();
    }

    private void cycle() {

    }

    public void send(byte[] buffer, int size) {
        _write(buffer, size);
    }

    private native int _set_output_url(String url);

    private native int _open();

    private native int _close();

    private native int _write(byte[] buffer, int size);
}
