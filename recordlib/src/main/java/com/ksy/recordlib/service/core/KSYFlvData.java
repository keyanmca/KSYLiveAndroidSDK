package com.ksy.recordlib.service.core;

import java.nio.ByteBuffer;

/**
 * Created by lixiaopeng on 15/7/6.
 */
public class KSYFlvData {

    public int dts;//解码时间戳

    public ByteBuffer byteBuffer; //数据

    public int size; //字节长度

    public int type; //视频和音频的分类

}
