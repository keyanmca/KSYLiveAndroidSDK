package com.ksy.recordlib.service.core;

import android.util.Log;

import com.ksy.recordlib.service.util.Constants;

import java.io.IOException;
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

    private static final int LEVEL1_QUEUE_SZIE = 10;
    private static final int LEVEL2_QUEUE_SZIE = 50;
    private static final int MAX_QUEUE_SIZE = 500;


    private static KsyRecordSender ksyRecordSenderInstance = new KsyRecordSender();

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
    private long lastSendVideoDts;
    private long lastSendAudioDts;

    private KsyRecordSender() {
        recordQueue = new LinkedList<KSYFlvData>();
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
        mUrl = url;
        int i = _set_output_url(url);
        //3视频  0音频
        if (j == FIRST_OPEN) {
            int c = _open();
            Log.d(TAG, "c ==" + c + ">>i=" + i);
        }
    }

    public String getAVBitrate() {
        return "currentTransferVideoBr=" + currentVideoBitrate +
                ", currentTransferAudiobr:" + currentAudioBitrate +
                ", encodeVideobr:" + encodeVideoBitrate +
                ", encodeAudiobr:" + encodeAudioBitrate +
                ", avgInstantaneousVideobr:" + avgInstantaneousVideoBitrate +
                ", avgInstantaneousAudiobr:" + avgInstantaneousAudioBitrate + ",size=" + recordQueue.size();
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
        while (!Thread.interrupted()) {
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
                if (ksyFlv.type == KSYFlvData.FLV_TYPE_VIDEO) {
                    frame_video--;
                } else if (ksyFlv.type == KSYFlvData.FLV_TYTPE_AUDIO) {
                    frame_audio--;
                }
                if (needDropFrame(ksyFlv)) {
                    statDropFrame(ksyFlv);
                } else {
                    lastRefreshTime = System.currentTimeMillis();
                    int w = _write(ksyFlv.byteBuffer, ksyFlv.byteBuffer.length);
                    statBitrate(w, ksyFlv.type);
                }
            } else {
//                Log.d(TAG, "::cycle() frame_video ||  frame_audio  <1 -------");
            }
        }
    }

    private boolean needDropFrame(KSYFlvData ksyFlv) {
        boolean dropFrame;
        int queueSize = recordQueue.size();
        int dts = ksyFlv.dts;
        if (queueSize < LEVEL1_QUEUE_SZIE) {
            dropFrame = false;
        } else if (queueSize < LEVEL2_QUEUE_SZIE) {
            if (ksyFlv.type == KSYFlvData.FLV_TYTPE_AUDIO || ksyFlv.isKeyframe()) {
                dropFrame = false;
            } else {
                int needKps = (int) (ksyFlv.size / 1024 * (1000) / (dts - lastSendVideoDts));
                dropFrame = (needKps > (avgInstantaneousAudioBitrate + avgInstantaneousVideoBitrate) / 2);
            }
        } else if (queueSize < MAX_QUEUE_SIZE) {
            if (ksyFlv.isKeyframe()) {
                dropFrame = false;
            } else {
                int needKps;
                if (ksyFlv.type == KSYFlvData.FLV_TYPE_VIDEO) {
                    needKps = (int) (ksyFlv.size / 1024 * (1000) / (dts - lastSendVideoDts));
                } else {
                    needKps = (int) (ksyFlv.size / 1024 * (1000) / (dts - lastSendAudioDts));
                }
                dropFrame = (needKps > (avgInstantaneousAudioBitrate + avgInstantaneousVideoBitrate) / 2);
            }
        } else {
            dropFrame = true;
        }
        if (ksyFlv.type == KSYFlvData.FLV_TYPE_VIDEO) {
            lastSendVideoDts = ksyFlv.dts;
        } else {
            lastSendAudioDts = ksyFlv.dts;
        }
        return dropFrame;
    }

    private void statDropFrame(KSYFlvData dropped) {
        Log.d(TAG, "drop frame !!" + dropped.isKeyframe());
    }

    private void statBitrate(int sent, int type) {
        long time = System.currentTimeMillis() - lastRefreshTime;
        time = time == 0 ? 1 : time;
        Log.e(TAG, "type=" + type + "time = " + time);
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
        worker.interrupt();
        recordQueue.clear();
        frame_video = 0;
        frame_audio = 0;
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
        Log.e(TAG, "::sender() k==" + k + ">>>time=" + time + "<<<frame_video==" + frame_video + ">>>frame_audio=" + frame_audio);
        // add video data
        synchronized (mutex) {
            if (recordQueue.size() > MAX_QUEUE_SIZE) {
                recordQueue.remove();
            }
            recordQueue.add(ksyFlvData);
        }
    }

    private native int _set_output_url(String url);

    private native int _open();

    private native int _close();

    private native int _write(byte[] buffer, int size);

}

