package com.ksy.recordlib.service.recoder;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceView;

import com.ksy.recordlib.service.core.KSYFlvData;
import com.ksy.recordlib.service.core.KsyMediaSource;
import com.ksy.recordlib.service.core.KsyRecordClient;
import com.ksy.recordlib.service.core.KsyRecordClientConfig;
import com.ksy.recordlib.service.core.KsyRecordSender;
import com.ksy.recordlib.service.util.Constants;
import com.ksy.recordlib.service.util.PrefUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Created by eflakemac on 15/6/19.
 */
public class RecoderVideoSource extends KsyMediaSource implements MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener {
    private static final int FRAME_TYPE_PPS = 1;
    private static final int FRAME_TYPE_SPS = 0;
    private static final int FRAME_TYPE_DATA = 2;
    private static final int FRAME_DEFINE_TYPE_VIDEO = 9;
    private static final int FRAME_DEFINE_HEAD_LENGTH = 11;
    private static final int FRAME_DEFINE_FOOTER_LENGTH = 4;
    private final KsyRecordClient.RecordHandler mHandler;
    private final Context mContext;
    private Camera mCamera;
    private SurfaceView mSurefaceView;
    private MediaRecorder mRecorder;
    private KsyRecordClientConfig mConfig;
    private ParcelFileDescriptor[] piple;
    private boolean mRunning = false;
    private String path;
    //    private long delay = 0;
    private long oldTime = 0;
    private long newTime = 0;
    private long duration = 0;
    private int length;
    private int type;
    private String pps;
    private String sps;
    private String pl;
    //    private int sum = 0;
    private boolean isPpsFrameSended = false;
    private boolean isSpsFrameSended = false;
    private ByteBuffer content;
    private byte[] flvFrameByteArray;
    private byte[] dataLengthArray;
    private byte[] timestampArray;
    private byte[] allFrameLengthArray;
    private byte[] buffer = new byte[1 * 1000 * 1000];
    private boolean isWriteFlvInSdcard = false;
    private int recordsum = 0;
    private int videoExtraSize = 9;
    private int last_sum = 0;

    private static final int VIDEO_TAG = 3;
    private static final int FROM_VIDEO_DATA = 6;
    private KsyRecordSender ksyVideoSender;

    private Byte kFlag;

    public static long startVideoTime;

    public RecoderVideoSource(Camera mCamera, KsyRecordClientConfig mConfig, SurfaceView mSurfaceView, KsyRecordClient.RecordHandler mRecordHandler, Context mContext) {
        this.mCamera = mCamera;
        this.mConfig = mConfig;
        this.mSurefaceView = mSurfaceView;
        mRecorder = new MediaRecorder();
        mHandler = mRecordHandler;
        this.mContext = mContext;

        ksyVideoSender = KsyRecordSender.getRecordInstance();
        ksyVideoSender.setRecorderData(mConfig.getUrl(), VIDEO_TAG);
        Log.d(Constants.LOG_TAG, "test");
    }

    @Override
    public void prepare() {
        mRecorder.setCamera(mCamera);
        mConfig.configMediaRecorder(mRecorder, KsyRecordClientConfig.MEDIA_SETP);
        try {
            this.piple = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            e.printStackTrace();
            release();
        }
//        delay = 1000 / 20;
        mRecorder.setOutputFile(this.piple[1].getFileDescriptor());
        try {
            mRecorder.setOnInfoListener(this);
            mRecorder.setOnErrorListener(this);
            mRecorder.prepare();
            mRecorder.start();
            startVideoTime = System.currentTimeMillis();
        } catch (IOException e) {
            e.printStackTrace();
            release();
        }
    }

    @Override
    public void start() {
        if (!mRunning) {
            mRunning = true;
            this.thread = new Thread(this);
            this.thread.start();
        }
    }

    @Override
    public void stop() {
        if (mRunning == true) {
            release();
        }
    }

    @Override
    public void release() {
        mRunning = false;
        releaseRecorder();
        releaseCamera();
        sync.clear();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
    }

    private void releaseRecorder() {
        if (mRecorder != null) {
            mRecorder.setOnErrorListener(null);
            mRecorder.setOnInfoListener(null);
            mRecorder.reset();
            Log.d(Constants.LOG_TAG, "mRecorder reset");
            mRecorder.release();
            Log.d(Constants.LOG_TAG, "mRecorder release");
            mRecorder = null;
            Log.d(Constants.LOG_TAG, "mRecorder complete");
        }
    }

    @Override
    public void run() {
        prepare();
        is = new FileInputStream(this.piple[0].getFileDescriptor());
        inputChannel = is.getChannel();
        while (mRunning) {
            Log.d(Constants.LOG_TAG, "entering video loop");
            // This will skip the MPEG4 header if this step fails we can't stream anything :(
            try {
                byte buffer[] = new byte[4];
                // Skip all atoms preceding mdat atom
                while (true) {  // box
                    while (is.read() != 'm') ;
                    is.read(buffer, 0, 3);
                    if (buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') break;
                    //mdat
                }
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, "Couldn't skip mp4 header :/");
                return;
            }
            pl = PrefUtil.getMp4ConfigProfileLevel(mContext);
            pps = PrefUtil.getMp4ConfigPps(mContext);
            sps = PrefUtil.getMp4ConfigSps(mContext);

            while (!Thread.interrupted()) {
                // Begin parse video data
                oldTime = System.currentTimeMillis();
                parseAndSend();
               /* duration = System.currentTimeMillis() - oldTime;
                stats.push(duration);
                delay = stats.average();*/
//                delay = 33;
            }
        }
        Log.d(Constants.LOG_TAG, "exiting video loop");
    }


    private void parseAndSend() {
        if (content == null) {
            content = ByteBuffer.allocate(mConfig.getVideoBitRate() * 2);
        }
        if (isSpsFrameSended) {
            parseVideo();
        } else {
//            delay = startVideoTime - RecoderAudioSource.startAudioTime;
//            if (Math.abs(delay) > 2000) {
//                delay = 0;
//            }
            content.clear();
            // Step One ,insert in header,sps & pps prefix & data
            byte[] sps_prefix = hexStringToBytes("0142C028FFE1");
            byte[] sps_only = Base64.decode(sps.getBytes(), Base64.DEFAULT);
            byte[] sps_length = intToByteArrayTwoByte(sps_only.length);
            byte[] pps_prefix = hexStringToBytes("01");
            byte[] pps_only = Base64.decode(pps.getBytes(), Base64.DEFAULT);
            byte[] pps_length = intToByteArrayTwoByte(pps_only.length);
            byte[] sps_pps = new byte[sps_prefix.length + sps_length.length + sps_only.length + pps_prefix.length
                    + pps_only.length + pps_length.length];
            fillArray(sps_pps, sps_prefix);
            fillArray(sps_pps, sps_length);
            fillArray(sps_pps, sps_only);
            fillArray(sps_pps, pps_prefix);
            fillArray(sps_pps, pps_length);
            fillArray(sps_pps, pps_only);
            // build sps_pps end
            content.put(sps_pps);
            length = content.position();
            makeFlvFrame(FRAME_TYPE_SPS);
            isSpsFrameSended = true;
        }
    }

    private void parseVideo() {
        try {
            // 0-3 length,4 type
            int headerResult = fill(header, 0, 4);
            Log.d(Constants.LOG_TAG, "header size = " + 4 + "header read result = " + headerResult);
            ts = sync.getTime();
            Log.d(Constants.LOG_TAG, "timestamp = " + ts);
            length = (header[0] & 0xFF) << 24 | (header[1] & 0xFF) << 16 | (header[2] & 0xFF) << 8 | (header[3] & 0xFF);
            if (length > mConfig.getVideoBitRate() * 5 || length < 0) {
                return;
            }
            Log.d(Constants.LOG_TAG, "header length = " + length + "content length");
            content.clear();
            int contentLength = readIntoBuffer(content, length);
            Log.d(Constants.LOG_TAG, "header length = " + length + "content length" + contentLength);
            if (content.limit() > 0) {
                kFlag = content.get(0);
                type = kFlag & 0x1F;
            }
            // Three types of flv video frame
            makeFlvFrame(FRAME_TYPE_DATA);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void fillArray(byte[] sps_pps, byte[] target) {
        for (int i = 0; i < target.length; i++) {
            sps_pps[last_sum + i] = target[i];
        }
        last_sum += target.length;
    }

    private void makeFlvFrame(int type) {
        if (type == FRAME_TYPE_SPS) {
            videoExtraSize = 5;
        } else {
            videoExtraSize = 9;
        }
        flvFrameByteArray = new byte[FRAME_DEFINE_HEAD_LENGTH + length + videoExtraSize + FRAME_DEFINE_FOOTER_LENGTH];
        flvFrameByteArray[0] = (byte) FRAME_DEFINE_TYPE_VIDEO;
        dataLengthArray = intToByteArray(length + videoExtraSize);
        flvFrameByteArray[1] = dataLengthArray[0];
        flvFrameByteArray[2] = dataLengthArray[1];
        flvFrameByteArray[3] = dataLengthArray[2];
        timestampArray = longToByteArray(ts);
        flvFrameByteArray[4] = timestampArray[1];
        flvFrameByteArray[5] = timestampArray[2];
        flvFrameByteArray[6] = timestampArray[3];
        flvFrameByteArray[7] = timestampArray[0];
        flvFrameByteArray[8] = (byte) 0;
        flvFrameByteArray[9] = (byte) 0;
        flvFrameByteArray[10] = (byte) 0;
        // added 5 extra bytes
        for (int i = 0; i < videoExtraSize; i++) {
            if (i == 0) {
                //1 byte flag
                flvFrameByteArray[11 + i] = (byte) 23;
            } else if (i == 1) {
                if (type == FRAME_TYPE_SPS || type == FRAME_TYPE_PPS) {
                    flvFrameByteArray[11 + i] = (byte) 0;
                } else {
                    flvFrameByteArray[11 + i] = (byte) 1;
                }
            } else if (i < 5) {
                flvFrameByteArray[11 + i] = (byte) 0;
            } else {
                if (type != FRAME_TYPE_SPS) {
                    byte[] real_length = intToByteArrayFull(length);
                    flvFrameByteArray[11 + i] = real_length[i - 5];
                }
            }
        }
        System.arraycopy(content.array(), 0, flvFrameByteArray, FRAME_DEFINE_HEAD_LENGTH + videoExtraSize, length);
        allFrameLengthArray = intToByteArrayFull(FRAME_DEFINE_HEAD_LENGTH + length + videoExtraSize + FRAME_DEFINE_FOOTER_LENGTH);
        flvFrameByteArray[FRAME_DEFINE_HEAD_LENGTH + length + videoExtraSize] = allFrameLengthArray[0];
        flvFrameByteArray[FRAME_DEFINE_HEAD_LENGTH + length + videoExtraSize + 1] = allFrameLengthArray[1];
        flvFrameByteArray[FRAME_DEFINE_HEAD_LENGTH + length + videoExtraSize + 2] = allFrameLengthArray[2];
        flvFrameByteArray[FRAME_DEFINE_HEAD_LENGTH + length + videoExtraSize + 3] = allFrameLengthArray[3];

        //添加视频数据到队列
        KSYFlvData ksyVideo = new KSYFlvData();
        ksyVideo.byteBuffer = flvFrameByteArray;
        ksyVideo.size = flvFrameByteArray.length;
        ksyVideo.dts = (int) ts;
        ksyVideo.type = 11;
        ksyVideo.frameType = type;

        ksyVideoSender.addToQueue(ksyVideo, FROM_VIDEO_DATA);

        /*for (int i = 0; i < flvFrameByteArray.length; i++) {
            if (recordsum + i < buffer.length) {
                if (recordsum == 0) {
                    byte[] flv = hexStringToBytes("464C56010100000009");
                    Log.d(Constants.LOG_TAG, "flv length = " + flv.length);
                    for (int j = 0; j < 9; j++) {
                        buffer[recordsum + j] = flv[j];
                    }
                    buffer[recordsum + 9 + 0] = 0;
                    buffer[recordsum + 9 + 1] = 0;
                    buffer[recordsum + 9 + 2] = 0;
                    buffer[recordsum + 9 + 3] = 0;
                    buffer[recordsum + 9 + 4 + i] = flvFrameByteArray[i];
                } else {
                    buffer[recordsum + i] = flvFrameByteArray[i];
                }
            } else {
                Log.d(Constants.LOG_TAG, "buffer write complete");

                if (!isWriteFlvInSdcard) {
                    String path = getSDPath();
                    File dir = new File(path + "/flvrecordtest");
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
                    createFile(dir + File.separator + "frame.flv", buffer);
                    Log.d(Constants.LOG_TAG, "write flv into sdcard complete");
                    isWriteFlvInSdcard = true;
                } else {
                    Log.d(Constants.LOG_TAG, "already write flv into sdcard complete");
                }
                break;

            }
        }
        if (recordsum == 0) {
            recordsum += flvFrameByteArray.length + 9 + 4;
        } else {
            recordsum += flvFrameByteArray.length;
        }*/
    }

    private byte[] longToByteArray(long ts) {
        byte[] result = new byte[4];
//        result[0] = new Long(ts >> 56 & 0xff).byteValue();
//        result[1] = new Long(ts >> 48 & 0xff).byteValue();
//        result[2] = new Long(ts >> 40 & 0xff).byteValue();
//        result[3] = new Long(ts >> 32 & 0xff).byteValue();
        result[0] = new Long(ts >> 24 & 0xff).byteValue();
        result[1] = new Long(ts >> 16 & 0xff).byteValue();
        result[2] = new Long(ts >> 8 & 0xff).byteValue();
        result[3] = new Long(ts >> 0 & 0xff).byteValue();
        return result;
    }

    private byte[] intToByteArray(int length) {
        byte[] result = new byte[3];
//        result[0] = (byte) ((length >> 24) & 0xFF);
        result[0] = (byte) ((length >> 16) & 0xFF);
        result[1] = (byte) ((length >> 8) & 0xFF);
        result[2] = (byte) ((length >> 0) & 0xFF);
        return result;
    }

    private byte[] intToByteArrayTwoByte(int length) {
        byte[] result = new byte[2];
//        result[0] = (byte) ((length >> 24) & 0xFF);
//        result[0] = (byte) ((length >> 16) & 0xFF);
        result[0] = (byte) ((length >> 8) & 0xFF);
        result[1] = (byte) ((length >> 0) & 0xFF);
        return result;
    }

    private byte[] intToByteArrayFull(int length) {
        byte[] result = new byte[4];
        result[0] = (byte) ((length >> 24) & 0xFF);
        result[1] = (byte) ((length >> 16) & 0xFF);
        result[2] = (byte) ((length >> 8) & 0xFF);
        result[3] = (byte) ((length >> 0) & 0xFF);
        return result;
    }

    private void sendFlv() {

    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        Log.d(Constants.LOG_TAG, "onInfo Message what = " + what + ",extra =" + extra);
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.d(Constants.LOG_TAG, "onError Message what = " + what + ",extra =" + extra);
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

    public void createFile(String path, byte[] content) {
        try {
            FileOutputStream outputStream = new FileOutputStream(path);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            bufferedOutputStream.write(content);
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private byte[] getBytes(char[] chars) {
        Charset cs = Charset.forName("UTF-8");
        CharBuffer cb = CharBuffer.allocate(chars.length);
        cb.put(chars);
        cb.flip();
        ByteBuffer bb = cs.encode(cb);
        return bb.array();
    }

    private char[] getChars(byte[] bytes) {
        Charset cs = Charset.forName("UTF-8");
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes);
        bb.flip();
        CharBuffer cb = cs.decode(bb);
        return cb.array();
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
}


