package com.ksy.ksyrecordsdk;

import android.content.Context;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;

import com.ksy.recordlib.service.core.KsyRecordClient;
import com.ksy.recordlib.service.exception.KsyRecordException;
import com.ksy.recordlib.service.util.Constants;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private CameraSurfaceView mSurfaceView;
    private FloatingActionButton mFab;
    private boolean mRecording = false;
    private SurfaceHolder mSurfaceHolder;
    private KsyRecordClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.LOG_TAG, "onCreate");
        setContentView(R.layout.activity_main);
        initWidget();
        setUpRecord();
    }

    private void initWidget() {
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRecord();
            }
        });
        setUpSurfaceView();
        setUpEnvironment();
    }

    private void setUpEnvironment() {
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void setUpSurfaceView() {
        mSurfaceView = (CameraSurfaceView) findViewById(R.id.surfaceView);
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this); // holder callback
        // setType must be set in old version, otherwise may cause crash
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }


    private void setUpRecord() {
        client = KsyRecordClient.getInstance(getApplicationContext());
        client.setCameraType(Constants.CAMERA_TYPE_BACK);
        client.setVoiceType(Constants.VOICE_TYPE_MIC);
        client.setAudioEncodeConfig(Constants.AUDIO_SAMPLE_RATE_DEFAULT, Constants.AUDIO_BIT_RATE_DEFAULT);
        client.setVideoEncodeConfig(Constants.VIDEO_FRAME_RATE_DEFAULT, Constants.VIDEO_BIT_RATE_DEFAULT);
        client.setVideoResolution(Constants.QUALITY_480P);
        client.setUrl(Constants.URL_DEFAULT);
        client.setDisplayPreview(mSurfaceView);
        // To do
//        client.setDropFrameFrequency();
//        client.getNewtWorkStatusType();
    }


    private void toggleRecord() {
        if (!mRecording) {
            try {
                client.startRecord();
                mRecording = true;
            } catch (KsyRecordException e) {
                e.printStackTrace();
                Log.d(Constants.LOG_TAG, "Client Error, reason = " + e.getMessage());
            }
        } else {
            client.stopRecord();
            mRecording = false;
        }
    }

    /*
    *
    * Activity Life Circle
    * */

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(Constants.LOG_TAG, "onStart");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(Constants.LOG_TAG, "onPause");
        client.release();
        mRecording = false;
        Log.d(Constants.LOG_TAG, "release");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Constants.LOG_TAG, "onResume");
//        toggleRecord();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(Constants.LOG_TAG, "onDestroy");
        client.release();
    }

    /*
    *
    * Surface Holder Callback
    * */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(Constants.LOG_TAG, "surfaceCreated");
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(Constants.LOG_TAG, "surfaceChanged");
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(Constants.LOG_TAG, "surfaceDestroyed");
//        mSurfaceHolder = null;
    }
}
