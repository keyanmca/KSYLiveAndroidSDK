package com.ksy.ksyrecordsdk;

import android.content.Intent;
import android.content.res.Configuration;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.heinrichreimersoftware.materialdrawer.DrawerView;
import com.heinrichreimersoftware.materialdrawer.structure.DrawerItem;
import com.ksy.ksyrecordsdk.com.ksy.ksyrecordsdk.config.DrawerItemConfigAdapter;
import com.ksy.recordlib.service.core.KsyRecordClient;
import com.ksy.recordlib.service.core.KsyRecordClientConfig;
import com.ksy.recordlib.service.core.KsyRecordSender;
import com.ksy.recordlib.service.exception.KsyRecordException;
import com.ksy.recordlib.service.util.Constants;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private CameraSurfaceView mSurfaceView;
    private FloatingActionButton mFab;
    private boolean mRecording = false;
    private KsyRecordClient client;
    private KsyRecordClientConfig config;
    private RelativeLayout mContainer;
    private ImageView mImageView;
    private TextView bitrate;
    private DrawerItemConfigAdapter adapter;
    private DrawerView drawer;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.LOG_TAG, "onCreate");
        setContentView(R.layout.activity_main);
        initWidget();
        setupRecord();
    }

    private void initWidget() {
        mContainer = (RelativeLayout) findViewById(R.id.container);
        bitrate = (TextView) findViewById(R.id.bitrate);
        mImageView = new ImageView(MainActivity.this);
        mImageView.setBackgroundColor(0xff000000);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRecord();
            }
        });
        setupSurfaceView();
        setUpEnvironment();
        initDrawer();
        startBitrateTimer();
    }

    private void startBitrateTimer() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                bitrate.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bitrate.setText(KsyRecordSender.getRecordInstance().getAVBitrate());
                    }
                }, 1000);
            }
        }, 1000, 1000);
    }

    private void initDrawer() {
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        drawer = (DrawerView) findViewById(R.id.drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.open,
                R.string.close
        ) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }
        };
        adapter = new DrawerItemConfigAdapter();
        adapter.setConfig(config);
        adapter.setContext(this);
        drawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.color_primary_dark));
        drawerLayout.setDrawerListener(drawerToggle);
        drawerLayout.closeDrawer(drawer);
        drawer.addItems(adapter.getItemViews());
        drawer.setOnItemClickListener(new DrawerItem.OnItemClickListener() {
            @Override
            public void onClick(final DrawerItem drawerItem, long l, final int position) {
                if (position == 6) {
                    new MaterialDialog.Builder(MainActivity.this)
                            .title(R.string.Url)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input("input rtmp server url", config.getUrl(), new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    config.setmUrl(input.toString());
                                    drawerItem.setTextSecondary(input.toString());
                                }
                            }).show();
                } else {
                    MaterialDialog.Builder builder = new MaterialDialog.Builder(MainActivity.this);
                    builder.itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                            adapter.onItemSelected(drawerItem, position, which, null);
                            return false;
                        }
                    });
                    builder.positiveText(R.string.choose);
                    adapter.setDialogItems(builder, position);
                    builder.show().setSelectedIndex(adapter.setDefaultSelected(position));
                }
            }
        });
    }

    private void setUpEnvironment() {
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        KsyRecordClientConfig.Builder builder = new KsyRecordClientConfig.Builder();
        builder.setVideoProfile(CamcorderProfile.QUALITY_720P).setUrl(Constants.URL_DEFAULT);
        config = builder.build();
    }

    private void setupSurfaceView() {
        mSurfaceView = (CameraSurfaceView) findViewById(R.id.surfaceView);
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this); // holder callback
        // setType must be set in old version, otherwise may cause crash
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    private void setupRecord() {
        client = KsyRecordClient.getInstance(getApplicationContext());
        client.setConfig(config);
        client.setDisplayPreview(mSurfaceView);
        // To do
    }


    private void toggleRecord() {
        if (!mRecording) {
            try {
                client.startRecord();
                mRecording = true;
                mFab.setImageDrawable(getResources().getDrawable(R.mipmap.btn_stop));
            } catch (KsyRecordException e) {
                e.printStackTrace();
                Log.d(Constants.LOG_TAG, "Client Error, reason = " + e.getMessage());
            }
        } else {
            mFab.setImageDrawable(getResources().getDrawable(R.mipmap.btn_record));
            client.stopRecord();
            mRecording = false;
            Log.d(Constants.LOG_TAG, "stop and release");
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
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(Constants.LOG_TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(Constants.LOG_TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(Constants.LOG_TAG, "surfaceDestroyed");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_github:
                String url = "https://github.com/HeinrichReimer/material-drawer";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
