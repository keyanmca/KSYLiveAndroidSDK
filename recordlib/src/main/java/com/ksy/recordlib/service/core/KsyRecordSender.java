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

    private static final int LEVEL1_QUEUE_SZIE = 250;
    private static final int LEVEL2_QUEUE_SZIE = 500;
    private static final int MAX_QUEUE_SIZE = 800;
    private static final int MIN_QUEUE_BUFFER = 1;


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
    private long lastSendVideoTs;
    private long lastSendAudioTs;
    private int dropAudioCount;
    private int dropVideoCount;

    private int lastAddAudioTs = 0;
    private int lastAddVideoTs = 0;

    private boolean inited = false;
    private long ideaStartTime;
    private long systemStartTime;

    private Speedometer vidoeFps = new Speedometer();
    private Speedometer audioFps = new Speedometer();

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
                "\n,vFps =" + vidoeFps.getSpeed() + " aFps=" + audioFps.getSpeed() + " dropA:" + dropAudioCount + " dropV" + dropVideoCount +
                "\n, lastSendAudioTs:" + lastSendAudioTs + "det=" + (lastSendAudioTs - lastSendVideoDts) + ",size=" + recordPQueue.size() + "\nf_v=" + frame_video + " f_a=" + frame_audio + "\n" + KsyMediaSource.sync.lastMessage;
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
            if (frame_video > MIN_QUEUE_BUFFER && frame_audio > MIN_QUEUE_BUFFER) {
                KSYFlvData ksyFlv = null;
                synchronized (mutex) {
                    if (recordPQueue.size() > 0) {
                        ksyFlv = recordPQueue.remove();
                    } else {
                        frame_audio = 0;
                        frame_video = 0;
                        continue;
                    }
                }
                if (ksyFlv.type == KSYFlvData.FLV_TYPE_VIDEO) {
                    frame_video--;
                    lastSendVideoTs = ksyFlv.dts;
                } else if (ksyFlv.type == KSYFlvData.FLV_TYTPE_AUDIO) {
                    frame_audio--;
                    lastSendAudioTs = ksyFlv.dts;
                }
                if (needDropFrame(ksyFlv)) {
                    statDropFrame(ksyFlv);
                } else {
                    lastRefreshTime = System.currentTimeMillis();
                    waiting(ksyFlv);
                    int w = _write(ksyFlv.byteBuffer, ksyFlv.byteBuffer.length);
                    statBitrate(w, ksyFlv.type);
                }
            }
        }
    }

    private boolean needDropFrame(KSYFlvData ksyFlv) {
        boolean dropFrame = false;
        int queueSize = recordPQueue.size();
        int dts = ksyFlv.dts;
        long duration = dts - lastSendVideoDts;
        if (duration == 0) {
            duration = 1;
        }
        if (queueSize < LEVEL1_QUEUE_SZIE) {
            dropFrame = false;
        } else if (queueSize < LEVEL2_QUEUE_SZIE) {
            if (ksyFlv.type == KSYFlvData.FLV_TYTPE_AUDIO || ksyFlv.isKeyframe()) {
                dropFrame = false;
            } else {
                int needKps = (int) (ksyFlv.size / 1024 * (1000) / duration);
                dropFrame = (needKps > (avgInstantaneousAudioBitrate + avgInstantaneousVideoBitrate) / 2);
            }
        } else if (queueSize < MAX_QUEUE_SIZE) {
            if (ksyFlv.isKeyframe()) {
                dropFrame = false;
            } else {
                int needKps;
                if (ksyFlv.type == KSYFlvData.FLV_TYPE_VIDEO) {
                    needKps = (int) (ksyFlv.size / 1024 * (1000) / duration);
                } else {
                    needKps = (int) (ksyFlv.size / 1024 * (1000) / duration);
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
        if (dropped.type == KSYFlvData.FLV_TYPE_VIDEO) {
            dropVideoCount++;
        } else if (dropped.type == KSYFlvData.FLV_TYTPE_AUDIO) {
            dropAudioCount++;
        }
        Log.d(TAG, "drop frame !!" + dropped.isKeyframe());
    }

    private void statBitrate(int sent, int type) {
        if (sent == -1) {
            connected = false;
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
                Log.e(TAG, "statBitrate time > 500ms network maybe poor! Time use:" + time);
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

        KsyMediaSource.sync.avDistance = (lastAddAudioTs - lastAddVideoTs);
        // add video data
        synchronized (mutex) {
            if (recordPQueue.size() > MAX_QUEUE_SIZE) {
                if (recordPQueue.remove().type == KSYFlvData.FLV_TYPE_VIDEO) {
                    frame_video--;
                } else {
                    frame_audio--;
                }
            }
            recordPQueue.add(ksyFlvData);
            if (k == FROM_VIDEO) { //视频数据
                vidoeFps.tickTock();
                frame_video++;
                lastAddVideoTs = ksyFlvData.dts;
            } else if (k == FROM_AUDIO) {//音频数据
                audioFps.tickTock();
                frame_audio++;
                lastAddAudioTs = ksyFlvData.dts;
            }
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
        if (!connected) {
            Log.e(TAG, "reconnecting ...");
            Log.e(TAG, "close .." + _close());
            Log.e(TAG, "_set_output_url .." + _set_output_url(mUrl));
            int result = _open();
            connected = result == 0;
            Log.e(TAG, "opens result ..>" + result);
        }
    }

    private void pauseSend() {
        connected = false;
    }

    public void disconnect() {
        _close();
        if (worker.isAlive()) {
            worker.interrupt();
        }
        recordPQueue.clear();
        frame_video = 0;
        frame_audio = 0;
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(receiver);
    }

    public void setRecorderData(String url, int j) {
        Log.e(TAG, "setRecorderData ..");
        mUrl = url;
        int i = _set_output_url(mUrl);
        Log.e(TAG, "_set_output_url .." + i);
        //3视频  0音频
        if (j == FIRST_OPEN) {
            int k = _open();
            connected = k == 0;
            Log.e(TAG, "connected .." + k);
        }
    }

    public void waiting(KSYFlvData ksyFlvData) throws InterruptedException {
        if (ksyFlvData.type != KSYFlvData.FLV_TYTPE_AUDIO) {
            return;
        }
        long ts = ksyFlvData.dts;
        if (!inited) {
            ideaStartTime = ts;
            systemStartTime = System.currentTimeMillis();
            inited = true;
            return;
        }
        long ideaTime = System.currentTimeMillis() - systemStartTime + ideaStartTime;
//            Log.e("SenderSync", "type=" + ksyFlvData.type + " ts=" + ts + " ideaTime=" + ideaTime + " d=" + (ts - ideaTime));
        if (Math.abs(ideaTime - ts) > 100) {
            inited = false;
            return;
        }
        while (ts > ideaTime) {
            Thread.sleep(1);
            ideaTime = System.currentTimeMillis() - systemStartTime + ideaStartTime;
        }
    }

    private native int _set_output_url(String url);

    private native int _open();

    private native int _close();

    private native int _write(byte[] buffer, int size);


    public static class Speedometer {
        private int time;
        private long startTime;
        private float currentFps = 0;

        public float getSpeedAndRestart() {
            float speed = getSpeed();
            time = 0;
            return speed;
        }

        public void tickTock() {
            if (time == 0) {
                startTime = System.currentTimeMillis();
            }
            time++;
            long current = System.currentTimeMillis();
            long lapse = current - startTime;
            if (lapse > 2000) {
                currentFps = ((float) time / lapse * 1000);
                startTime = current;
                time = 0;
            }
        }

        public void start() {
            time = 0;
            startTime = System.currentTimeMillis();
        }


        public float getSpeed() {
            return currentFps;
        }

    }

}

