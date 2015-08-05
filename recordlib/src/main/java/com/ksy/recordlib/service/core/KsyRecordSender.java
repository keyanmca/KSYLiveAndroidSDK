package com.ksy.recordlib.service.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ksy.recordlib.service.util.Constants;
import com.ksy.recordlib.service.util.NetworkMonitor;

import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;

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

    //    private LinkedList<KSYFlvData> recordQueue;
    private PriorityQueue<KSYFlvData> recordPQueue;

    private Object mutex = new Object();
    private Context mContext;

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

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.NETWORK_STATE_CHANGED)) {
                onNetworkChanged();
            }
        }
    };

    static {
        System.loadLibrary("rtmp");
        Log.i(Constants.LOG_TAG, "rtmp.so loaded");
        System.loadLibrary("ksyrtmp");
        Log.i(Constants.LOG_TAG, "ksyrtmp.so loaded");
    }

    private KsyRecordSender() {
//        recordQueue = new LinkedList<>();
        recordPQueue = new PriorityQueue<>(10, new Comparator<KSYFlvData>() {
            @Override
            public int compare(KSYFlvData lhs, KSYFlvData rhs) {
                return lhs.dts - rhs.dts;
            }
        });
    }


    public static KsyRecordSender getRecordInstance() {
        return ksyRecordSenderInstance;
    }

    public String getAVBitrate() {
        return "currentTransferVideoBr=" + currentVideoBitrate +
                ", currentTransferAudiobr:" + currentAudioBitrate +
                ", encodeVideobr:" + encodeVideoBitrate +
                ", encodeAudiobr:" + encodeAudioBitrate +
                ", avgInstantaneousVideobr:" + avgInstantaneousVideoBitrate +
                ", avgInstantaneousAudiobr:" + avgInstantaneousAudioBitrate + ",size=" + recordPQueue.size();
    }


    public void start(Context pContext) throws IOException {
        IntentFilter filter = new IntentFilter(Constants.NETWORK_STATE_CHANGED);
        LocalBroadcastManager.getInstance(pContext).registerReceiver(receiver, filter);
        mContext = pContext;
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

    private void cycle() throws InterruptedException {
        while (!Thread.interrupted()) {
            while (!connected) {
                Thread.sleep(10);
            }
            if (frame_video > 1 || frame_audio > 1) {
                KSYFlvData ksyFlv;
                synchronized (mutex) {
//                    Collections.sort(recordQueue, new Comparator<KSYFlvData>() {
//                        @Override
//                        public int compare(KSYFlvData lhs, KSYFlvData rhs) {
//                            return lhs.dts - rhs.dts;
//                        }
//                    });
                    ksyFlv = recordPQueue.remove();
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
            }
        }

    }

    private boolean needDropFrame(KSYFlvData ksyFlv) {
        boolean dropFrame;
        int queueSize = recordPQueue.size();
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
        if (sent == -1) {
            Log.e(TAG, "statBitrate send frame failed!");
        } else {
            Log.d(TAG, "statBitrate send successful sent =" + sent + "type= " + type);
            long time = System.currentTimeMillis() - lastRefreshTime;
            long escape = System.currentTimeMillis() - last_stat_time;
            time = time == 0 ? 1 : time;
            if (type == 11) {
                currentVideoBitrate = sent / (time);
                videoByteSum += sent;
                videoTime += time;
            } else if (type == 12) {
                currentAudioBitrate = sent / (time);
                audioByteSum += sent;
                audioTime += time;
            }
            if (time > 500) {
                Log.e(TAG, "statBitrate time > 500" + time);
            }
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

    }

    //send data to server
    public synchronized void addToQueue(KSYFlvData ksyFlvData, int k) {
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
//        int time = ksyFlvData.dts;
//        Log.e(TAG, "::addToQueue() k==" + k + ">>>time=" + time + "<<<frame_video==" + frame_video + ">>>frame_audio=" + frame_audio);
        // add video data
        synchronized (mutex) {
            if (recordPQueue.size() > MAX_QUEUE_SIZE) {
                recordPQueue.remove();
            }
            recordPQueue.add(ksyFlvData);
        }
    }


    private synchronized void onNetworkChanged() {
        Log.e(TAG, "onNetworkChanged .." + NetworkMonitor.networkConnected());
        if (NetworkMonitor.networkConnected()) {
            reconnect();
        } else {
            pauseSend();
        }
    }

    private void reconnect() {
        Log.e(TAG, "reconnecting ..");
        Log.e(TAG, "close .." + _close());
        Log.e(TAG, "_set_output_url .." + _set_output_url(mUrl));
        connected = _open() == 0;
        Log.e(TAG, "opens result .." + connected);
    }

    private void pauseSend() {
        connected = false;
    }

    public void disconnect() {
        _close();
        worker.interrupt();
        recordPQueue.clear();
        frame_video = 0;
        frame_audio = 0;
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(receiver);
    }

    public void setRecorderData(String url, int j) {
        mUrl = url;
        int i = _set_output_url(url);
        //3视频  0音频
        if (j == FIRST_OPEN) {
            connected = _open() == 0;
        }
    }

    private native int _set_output_url(String url);

    private native int _open();

    private native int _close();

    private native int _write(byte[] buffer, int size);

}

