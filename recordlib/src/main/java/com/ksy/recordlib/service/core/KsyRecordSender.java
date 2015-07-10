package com.ksy.recordlib.service.core;

import android.util.Log;

import com.ksy.recordlib.service.util.Constants;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by eflakemac on 15/6/26.
 */
public class KsyRecordSender {
    //	@AccessedByNative
    public long mNativeRTMP;

    private Thread worker;
    private String mUrl;
    private boolean connected = false;

    private ArrayList<KSYFlvData> recordQueue ;

    private static final int FIRST_OPEN = 3;
    private static final int FROM_AUDIO = 8;
    private static final int FROM_VIDEO = 6;

    private static int frame_video;
    private static int frame_audio;

    private static KsyRecordSender ksyRecordSenderInstance;
    private KsyRecordSender() {}

    static {
        System.loadLibrary("rtmp");
        Log.i(Constants.LOG_TAG, "rtmp.so loaded");
        System.loadLibrary("ksyrtmp");
        Log.i(Constants.LOG_TAG, "ksyrtmp.so loaded");
    }


    public static KsyRecordSender getRecordInstance() {

        if (ksyRecordSenderInstance == null) {

            synchronized(KsyRecordSender.class) {
                ksyRecordSenderInstance = new KsyRecordSender();
            }

        }

        return ksyRecordSenderInstance;
    }


    public void setRecorderData(String url, int j) {

        recordQueue = new ArrayList<KSYFlvData>();

        mUrl = url;

        int i = _set_output_url(url);

        //3视频  0音频
        if (j == FIRST_OPEN) {
            int c = _open();
            Log.e("lixp", "c ==" + c);
        }

    }


    public void start() throws IOException {

        worker = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
//                    cycle();

                } catch (Exception e) {
                    Log.e(Constants.LOG_TAG, "worker: thread exception.");
                    e.printStackTrace();
                }
            }
        });
        worker.start();
    }


    //重新连接
    private void reconnect() throws Exception {

        if (connected) {
            return;
        }

        _close();

        _set_output_url(mUrl);

        int conncode = _open();

        connected = conncode == 0 ? true : false;

    }

    //断开连接
    private void disconnect() {

        _close();
    }


    //send data to server
    public synchronized void sender(KSYFlvData ksyFlvData, int k) {

        if (ksyFlvData == null) {
            return;
        }

        if (ksyFlvData.size <= 0) {
            return;
        }


        if (k == FROM_VIDEO) { //视频数据
            frame_video++;

        } else if (k == FROM_AUDIO) {//音频数据
            frame_audio++;

        }

        int time = ksyFlvData.dts;

        Log.e("lixp", "k==" + k + ">>>time=" + time + "<<<>>>frame_video==" + frame_video + ">>>frame_audio=" + frame_audio);

        // add video data
        recordQueue.add(ksyFlvData);

        //send
        if (frame_video > 1 && frame_audio > 1) {
            //数据排序
            /*Collections.sort(recordQueue, new Comparator<KSYFlvData>() {

                @Override
                public int compare(KSYFlvData lhs, KSYFlvData rhs) {

                    //Log.e("lixp", "lhs.dts=" + lhs.dts + ">>rhs.dts=" + rhs.dts + ">>>lhs.type==" + lhs.type + "<<>>rhs.type=" + rhs.type);

                    return lhs.dts - rhs.dts;
                }
            });*/


            while (frame_video > 1 && frame_audio > 1) {

                KSYFlvData ksyFlv = recordQueue.remove(0);

                if (ksyFlv.type == 11) {
                    frame_video--;

                } else if (ksyFlv.type == 12) {
                    frame_audio--;
                }

                ksyFlv.byteBuffer.rewind(); //使缓冲区为重新读取已包含的数据做好准备，它使限制保持不变，将位置设置为0
                byte[] data = ksyFlv.byteBuffer.array();

                int w = _write(data, data.length);

                Log.e("lixp", " w=" + w + ">>data=" + data + "<<<>>>>" + data.length);

            }
        }
    }


    private native int _set_output_url(String url);

    private native int _open();

    private native int _close();

    private native int _write(byte[] buffer, int size);

}

