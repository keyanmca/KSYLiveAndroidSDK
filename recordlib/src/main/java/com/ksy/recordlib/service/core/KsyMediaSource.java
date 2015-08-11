package com.ksy.recordlib.service.core;

import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by eflakemac on 15/6/19.
 */
public abstract class KsyMediaSource implements Runnable {
    protected Thread thread;
    protected FileInputStream is;
    protected FileChannel inputChannel;
    protected byte[] header = new byte[4];
    protected long ts = 0;
    protected static ClockSync sync = new ClockSync();

    public abstract void prepare();

    public abstract void start();

    public abstract void stop();

    public abstract void release();

//    protected KsyRecordSender addToQueue;

    /*protected KsyMediaSource(String url, int i) {
        Log.e("lixp", "KsyMediaSource  27  ....");

//        KsyRecordSender.getInstance().setUrl(url, i);

        addToQueue = new KsyRecordSender(url, i);

      *//*  try {
            addToQueue.start();

        } catch (Exception e) {
            Log.e("lixp","KsyMediaSource   e = " + e);
        }*//*

    }
*/

    protected int fill(byte[] buffer, int offset, int length) throws IOException {
        int sum = 0, len;
        while (sum < length) {
            len = is.read(buffer, offset + sum, length - sum);
            if (len < 0) {
                throw new IOException("End of stream");
            } else sum += len;
        }
        return sum;
    }

    protected int readIntoBuffer(ByteBuffer byteBuffer, int length) throws IOException {
        int sum = 0, len;
        if (byteBuffer.position() + length > byteBuffer.capacity()) {
            byteBuffer.limit(byteBuffer.capacity());
        } else {
            byteBuffer.limit(byteBuffer.position() + length);
        }
        while (sum < length) {
            len = inputChannel.read(byteBuffer);
            if (len < 0) {
                throw new IOException("End of stream");
            } else sum += len;
        }
        return sum;
    }

    protected static class ClockSync {


        private long frameSumDuration = 0;
        private long frameSumCount = 10000;
        private boolean inited = false;
        private double lastTS = 0;
        private long lastSysTime = 0;
        private long syncTime = 0;

        public void reset(long pAudioTime) {
            syncTime = pAudioTime;
            Log.e("ClockSync", "pAudioTime==== " + pAudioTime);
        }

        public long getTime() {
            long d = 0;
            long delta = 0;
            if (!inited) {
                frameSumCount = 10000;
                frameSumDuration = frameSumCount * 33;
                lastSysTime = System.currentTimeMillis();
                lastTS = 0;
                syncTime = 0;
                inited = true;
            } else {
                long currentTime = System.currentTimeMillis();
                d = currentTime - lastSysTime;
                lastSysTime = currentTime;
                frameSumDuration += d;
                frameSumCount++;
                long diff = (long) (syncTime - lastTS);
                delta = 0;
                if ((diff) > 500) {
                    delta = (long) (1f / 3 * frameSumDuration / frameSumCount);
                } else {
                    delta = (long) (-1f / 3 * frameSumDuration / frameSumCount);
                }
                lastTS += 1.0f * frameSumDuration / frameSumCount + delta;

            }
            Log.e("ClockSync", "pVideoTime**** " + lastTS + " d=" + d + " delta=" + delta);
            if (delta > 0) {
                Log.e("ClockSync", "delat>0!!!");
            }
            return (long) lastTS;

        }

        public void clear() {
            inited = false;
        }
    }


}
