package com.ksy.recordlib.service.core;

import android.os.Environment;
import android.util.Log;

import com.ksy.recordlib.service.util.Constants;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * Created by eflakemac on 15/6/26.
 */
public class KsyRecordSender {
    //	@AccessedByNative
    public long mNativeRTMP;

    private String TAG = "KsyRecordSender";

    private Thread worker;
    private String mUrl;
    private boolean connected = false;

    private LinkedList<KSYFlvData> recordQueue;

    private Object mutex = new Object();

    private static final int FIRST_OPEN = 3;
    private static final int FROM_AUDIO = 8;
    private static final int FROM_VIDEO = 6;

    private static volatile int frame_video;
    private static volatile int frame_audio;

    private static KsyRecordSender ksyRecordSenderInstance = new KsyRecordSender();

    private byte[] buffer = new byte[10 * 1000 * 1000];

    private boolean isWrite = false;
    private int recordsum = 0;
    String filePath;

    /**
     * this is instantaneous value of video/audio bitrate
     */
    private float currentVideoBitrate, currentAudioBitrate = 0;
    /**
     * producer bitrate
     */
    private float encodeVideoBitrate, encodeAudioBitrate;
    /**
     * this is average instantaneous value of video/audio bitrate during last second
     */
    private float avgInstantaneousVideoBitrate, avgInstantaneousAudioBitrate;
    private int videoByteSum, audioByteSum;
    private long videoTime, audioTime;
    private long last_stat_time;
    private long lastRefreshTime;

    private KsyRecordSender() {
    }

    public float getCurrentVideoBitrate() {
        return currentVideoBitrate;
    }

    public float getCurrentAudioBitrate() {
        return currentAudioBitrate;
    }

    public float getEncodeVideoBitrate() {
        return encodeVideoBitrate;
    }

    public float getEncodeAudioBitrate() {
        return encodeAudioBitrate;
    }

    public float getAvgInstantaneousVideoBitrate() {
        return avgInstantaneousVideoBitrate;
    }

    public float getAvgInstantaneousAudioBitrate() {
        return avgInstantaneousAudioBitrate;
    }

    static {
        System.loadLibrary("rtmp");
        Log.i(Constants.LOG_TAG, "rtmp.so loaded");
        System.loadLibrary("ksyrtmp");
        Log.i(Constants.LOG_TAG, "ksyrtmp.so loaded");
    }


    public static KsyRecordSender getRecordInstance() {
        return ksyRecordSenderInstance;
    }


    public void setRecorderData(String url, int j) {

        recordQueue = new LinkedList<KSYFlvData>();

        mUrl = url;

        int i = _set_output_url(url);

        //3视频  0音频
        if (j == FIRST_OPEN) {
            int c = _open();

            Log.d(TAG, "c ==" + c + ">>i=" + i);
        }

    }

    public String getAVBitrate() {
        return
                "currentTransferVideoBr=" + currentVideoBitrate +
                        ", currentTransferAudiobr:" + currentAudioBitrate +
                        ", encodeVideobr:" + encodeVideoBitrate +
                        ", encodeAudiobr:" + encodeAudioBitrate +
                        ", avgInstantaneousVideobr:" + avgInstantaneousVideoBitrate +
                        ", avgInstantaneousAudiobr:" + avgInstantaneousAudioBitrate
                ;
    }

    public void start() throws IOException {

        worker = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    cycle();

                } catch (Exception e) {
                    Log.e(Constants.LOG_TAG, "worker: thread exception. e＝" + e);
                    e.printStackTrace();
                }
            }
        });
        worker.start();
    }


    private void cycle() {

        while (true) {
            //send
            if (frame_video > 1 || frame_audio > 1) {

                KSYFlvData ksyFlv;

                synchronized (mutex) {
                    Collections.sort(recordQueue, new Comparator<KSYFlvData>() {
                        @Override
                        public int compare(KSYFlvData lhs, KSYFlvData rhs) {

                            return lhs.dts - rhs.dts;
                        }
                    });

                    ksyFlv = recordQueue.remove(0);
                }

                Log.e(TAG, "---======---- ksyFlv.type=" + ksyFlv.type + ">>frame_video=" + frame_video + "<<>>frame_audio=" + frame_audio);

                if (ksyFlv.type == 11) {
                    frame_video--;

                } else if (ksyFlv.type == 12) {
                    frame_audio--;
                }

                //写文件
                createFile(filePath, ksyFlv.byteBuffer, ksyFlv.byteBuffer.length);


                lastRefreshTime = System.currentTimeMillis();
                int w = _write(ksyFlv.byteBuffer, ksyFlv.byteBuffer.length);
                statBitrate(w, ksyFlv.type);

                Log.e(TAG, " w=" + w + ">>data=" + "<<<>>>>" + ksyFlv.byteBuffer.length);

            } else {

                Log.d(TAG, "frame_video ||  frame_audio  <1 -------");
            }
        }
    }

    private void statBitrate(int sent, int type) {
        long time = System.currentTimeMillis() - lastRefreshTime;
        time = time == 0 ? 1 : time;
        Log.d(TAG, "type=" + type + "time = " + time);
        if (type == 11) {
            currentVideoBitrate = sent / (time);
            videoByteSum += sent;
            videoTime += time;
        } else if (type == 12) {
            currentAudioBitrate = sent / (time);
            audioByteSum += sent;
            audioTime += time;
        }
        long escape = System.currentTimeMillis() - last_stat_time;
        if (escape > 1000) {
            encodeVideoBitrate = (float) videoByteSum / escape;
            encodeAudioBitrate = (float) audioByteSum / escape;
            avgInstantaneousVideoBitrate = (float) videoByteSum / videoTime;
            avgInstantaneousAudioBitrate = (float) audioByteSum / audioTime;
            videoByteSum = 0;
            videoTime = 0;
            audioByteSum = 0;
            audioTime = 0;
            last_stat_time = System.currentTimeMillis();
        }
    }


    public byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }


    private String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
            return sdDir.toString();
        }

        return null;
    }

    public void createFile(String path, byte[] content, int length) {
        try {
            FileOutputStream outputStream = new FileOutputStream(path, true);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            bufferedOutputStream.write(content, 0, length);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void reconnect() throws Exception {

        if (connected) {
            return;
        }

        _close();

        _set_output_url(mUrl);

        int conncode = _open();

        connected = conncode == 0 ? true : false;
    }


    public void disconnect() {

        _close();

        recordQueue.clear();
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

        Log.e(TAG, "k==" + k + ">>>time=" + time + "<<<frame_video==" + frame_video + ">>>frame_audio=" + frame_audio);

        // add video data
        synchronized (mutex) {
            recordQueue.add(ksyFlvData);
        }

    }

    private native int _set_output_url(String url);

    private native int _open();

    private native int _close();

    private native int _write(byte[] buffer, int size);

}

