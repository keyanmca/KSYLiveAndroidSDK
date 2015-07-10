package com.ksy.recordlib.service.core;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by eflakemac on 15/6/19.
 */
public abstract class KsyMediaSource implements Runnable {
    protected Thread thread;
    protected FileInputStream is;
    protected byte[] header = new byte[4];
    protected long ts = 0;

    public abstract void prepare();

    public abstract void start();

    public abstract void stop();

    public abstract void release();

//    protected KsyRecordSender sender;

    /*protected KsyMediaSource(String url, int i) {
        Log.e("lixp", "KsyMediaSource  27  ....");

//        KsyRecordSender.getInstance().setUrl(url, i);

        sender = new KsyRecordSender(url, i);

      *//*  try {
            sender.start();

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


    /**
     * Used in packetizers to estimate timestamps in RTP packets.
     */
    protected static class Statistics {

        public final static String TAG = "Statistics";

        private int count = 700, c = 0;
        private float m = 0, q = 0;
        private long elapsed = 0;
        private long start = 0;
        private long duration = 0;
        private long period = 10000000000L;
        private boolean initoffset = false;

        public Statistics() {
        }

        public Statistics(int count, int period) {
            this.count = count;
            this.period = period;
        }

        public void reset() {
            initoffset = false;
            q = 0;
            m = 0;
            c = 0;
            elapsed = 0;
            start = 0;
            duration = 0;
        }

        public void push(long value) {
            elapsed += value;
            if (elapsed > period) {
                elapsed = 0;
                long now = System.nanoTime();
                if (!initoffset || (now - start < 0)) {
                    start = now;
                    duration = 0;
                    initoffset = true;
                }
                // Prevents drifting issues by comparing the real duration of the
                // stream with the sum of all temporal lengths of RTP packets.
                value += (now - start) - duration;
                //Log.d(TAG, "sum1: "+duration/1000000+" sum2: "+(now-start)/1000000+" drift: "+((now-start)-duration)/1000000+" v: "+value/1000000);
            }
            if (c < 5) {
                // We ignore the first 20 measured values because they may not be accurate
                c++;
                m = value;
            } else {
                m = (m * q + value) / (q + 1);
                if (q < count) q++;
            }
        }

        public long average() {
            long l = (long) m;
            duration += l;
            return l;
        }

    }

}
