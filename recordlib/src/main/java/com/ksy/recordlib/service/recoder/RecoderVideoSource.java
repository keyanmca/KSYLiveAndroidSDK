package com.ksy.recordlib.service.recoder;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.SurfaceView;

import com.ksy.recordlib.service.core.KsyMediaSource;
import com.ksy.recordlib.service.core.KsyRecordClient;
import com.ksy.recordlib.service.core.KsyRecordClientConfig;
import com.ksy.recordlib.service.util.Constants;
import com.ksy.recordlib.service.util.FileUtil;
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
import java.util.ArrayList;

/**
 * Created by eflakemac on 15/6/19.
 */
public class RecoderVideoSource extends KsyMediaSource implements MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener {
    private static final int FRAME_TYPE_PPS = 0;
    private static final int FRAME_TYPE_SPS = 1;
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
    private long delay = 0;
    private long oldTime = 0;
    private long newTime = 0;
    private long duration = 0;
    private Statistics stats = new Statistics();
    private int length;
    private int type;
    private String pps;
    private String sps;
    private String pl;
    private int sum = 0;
    private boolean isPpsFrameSended = false;
    private boolean isSpsFrameSended = false;
    private byte[] content;
    private byte[] flvFrameByteArray;
    private byte[] dataLengthArray;
    private byte[] timestampArray;
    private byte[] allFrameLengthArray;
    private byte[] buffer = new byte[1000 * 1000];
    private boolean isWriteFlvInSdcard = false;
    private int recordsum = 0;


    public RecoderVideoSource(Camera mCamera, KsyRecordClientConfig mConfig, SurfaceView mSurfaceView, KsyRecordClient.RecordHandler mRecordHandler, Context mContext) {
        super(mConfig.getUrl());
        this.mCamera = mCamera;
        this.mConfig = mConfig;
        this.mSurefaceView = mSurfaceView;
        mRecorder = new MediaRecorder();
        mHandler = mRecordHandler;
        this.mContext = mContext;
    }

    @Override
    public void prepare() {
        mRecorder.setCamera(mCamera);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
//        profile.videoFrameWidth = 1;
//        profile.videoFrameHeight = 1;
//        mRecorder.setProfile(profile);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mRecorder.setVideoFrameRate(mConfig.getVideoFrameRate());
//        mRecorder.setOutputFile(FileUtil.getOutputMediaFile(Constants.MEDIA_TYPE_VIDEO));
        try {
            this.piple = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            e.printStackTrace();
            release();
        }
        mRecorder.setOutputFile(this.piple[1].getFileDescriptor());
        try {
            mRecorder.setOnInfoListener(this);
            mRecorder.setOnErrorListener(this);
            mRecorder.prepare();
            mRecorder.start();
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
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
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
            mCamera.lock();
        }
    }

    @Override
    public void run() {
        prepare();
        is = new FileInputStream(this.piple[0].getFileDescriptor());
        while (mRunning) {
            Log.d(Constants.LOG_TAG, "entering video loop");

            // This will skip the MPEG4 header if this step fails we can't stream anything :(
            try {
                byte buffer[] = new byte[4];
                // Skip all atoms preceding mdat atom
                while (true) {
                    while (is.read() != 'm') ;
                    is.read(buffer, 0, 3);
                    if (buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') break;
                }
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, "Couldn't skip mp4 header :/");
                return;
            }
            pl = PrefUtil.getMp4ConfigProfileLevel(mContext);
            pps = PrefUtil.getMp4ConfigPps(mContext);
            sps = PrefUtil.getMp4ConfigSps(mContext);
            Log.d(Constants.LOG_TAG, "pl = " + pl + ",pps =" + pps + ",sps = " + sps);
            while (!Thread.interrupted()) {
                // Begin parse video data
                oldTime = System.nanoTime();
                parseAndSend();
                duration = System.nanoTime() - oldTime;
                stats.push(duration);
                delay = stats.average();
            }
        }
        Log.d(Constants.LOG_TAG, "exiting video loop");
    }

    private void parseAndSend() {
        parseVideo();
    }

    private void parseVideo() {
        try {
            // 0-3 length,4 type
//            int headerResult = is.read(header, 0, 4);
            int headerResult = fill(header, 0, 4);

            Log.d(Constants.LOG_TAG, "header size = " + 4 + "header read result = " + headerResult);
            if (headerResult != 4) {
            }
            ts += delay;
            length = (header[0] & 0xFF) << 24 | (header[1] & 0xFF) << 16 | (header[2] & 0xFF) << 8 | (header[3] & 0xFF);
            if (length > 100000 || length < 0) {
//                resync();
            }
//            type = header[4] & 0x1F;
            if (content != null) {
                content = null;
            }
            content = new byte[length];
//          int contentResult = is.read(content, 0, length);
            int contentResult = fill(content, 0, length);
            Log.d(Constants.LOG_TAG, "content length = " + length + ",content read result = " + contentResult);
//            content = null;
            sum += length;
            // Three types of flv video frame
            if (!isSpsFrameSended) {
                content = null;
                content = sps.getBytes();
                length = content.length;
                makeFlvFrame();
                isSpsFrameSended = true;
                return;
            }
            if (!isPpsFrameSended) {
                content = null;
                content = pps.getBytes();
                length = content.length;
                makeFlvFrame();
                isPpsFrameSended = true;
                return;
            }
            makeFlvFrame();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void makeFlvFrame() {
        if (flvFrameByteArray != null) {
            flvFrameByteArray = null;
        }
        flvFrameByteArray = new byte[FRAME_DEFINE_HEAD_LENGTH + length + FRAME_DEFINE_FOOTER_LENGTH];
        flvFrameByteArray[0] = (byte) FRAME_DEFINE_TYPE_VIDEO;
        dataLengthArray = intToByteArray(length);
        flvFrameByteArray[1] = dataLengthArray[0];
        flvFrameByteArray[2] = dataLengthArray[1];
        flvFrameByteArray[3] = dataLengthArray[2];
        timestampArray = longToByteArray(ts);
        flvFrameByteArray[4] = timestampArray[0];
        flvFrameByteArray[5] = timestampArray[1];
        flvFrameByteArray[6] = timestampArray[2];
        flvFrameByteArray[7] = timestampArray[3];
        flvFrameByteArray[8] = (byte) 0;
        flvFrameByteArray[9] = (byte) 0;
        flvFrameByteArray[10] = (byte) 0;
        for (int i = 0; i < length; i++) {
            flvFrameByteArray[FRAME_DEFINE_HEAD_LENGTH + i] = content[i];
        }
        allFrameLengthArray = intToByteArrayFull(FRAME_DEFINE_HEAD_LENGTH + length + FRAME_DEFINE_FOOTER_LENGTH);
        flvFrameByteArray[FRAME_DEFINE_HEAD_LENGTH + length] = allFrameLengthArray[0];
        flvFrameByteArray[FRAME_DEFINE_HEAD_LENGTH + length + 1] = allFrameLengthArray[0];
        flvFrameByteArray[FRAME_DEFINE_HEAD_LENGTH + length + 2] = allFrameLengthArray[1];
        flvFrameByteArray[FRAME_DEFINE_HEAD_LENGTH + length + 3] = allFrameLengthArray[2];
//        sender.send(flvFrameByteArray, FRAME_DEFINE_HEAD_LENGTH + length + FRAME_DEFINE_FOOTER_LENGTH);
        for (int i = 0; i < flvFrameByteArray.length; i++) {
            if (recordsum + i < buffer.length) {
                if (recordsum == 0) {
                    byte[] flv = hexStringToBytes("464C56010100000009");
                    Log.d(Constants.LOG_TAG, "flv length = " + flv.length);
                    for (int j = 0; j < 9; j++) {
                        buffer[recordsum + j] = flv[j];
                    }
                    buffer[recordsum + 9 + i] = flvFrameByteArray[i];
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
                    createFile(dir + File.separator + "frame", buffer);
                    Log.d(Constants.LOG_TAG, "write flv into sdcard complete");
                    isWriteFlvInSdcard = true;
                } else {
                    Log.d(Constants.LOG_TAG, "already write flv into sdcard complete");
                }
                break;

            }
        }
        if (recordsum == 0) {
            recordsum += flvFrameByteArray.length + 9;
        } else {
            recordsum += flvFrameByteArray.length;
        }
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

    private byte[] intToByteArrayFull(int length) {
        byte[] result = new byte[3];
        result[0] = (byte) ((length >> 24) & 0xFF);
        result[0] = (byte) ((length >> 16) & 0xFF);
        result[1] = (byte) ((length >> 8) & 0xFF);
        result[2] = (byte) ((length >> 0) & 0xFF);
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
                .equals(android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
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


