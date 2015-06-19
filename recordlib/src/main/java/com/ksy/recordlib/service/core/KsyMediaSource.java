package com.ksy.recordlib.service.core;

/**
 * Created by eflakemac on 15/6/19.
 */
public abstract class KsyMediaSource implements Runnable {
    public Thread thread;

    public abstract void prepare();

    public abstract void start();

    public abstract void stop();

    public abstract void release();

}
