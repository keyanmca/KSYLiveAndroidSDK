package com.ksy.ksyrecordsdk.com.ksy.ksyrecordsdk.config;

import android.content.Context;
import android.hardware.Camera;

import com.afollestad.materialdialogs.MaterialDialog;
import com.heinrichreimersoftware.materialdrawer.structure.DrawerItem;
import com.ksy.ksyrecordsdk.R;
import com.ksy.recordlib.service.core.CameraHelper;
import com.ksy.recordlib.service.core.KsyRecordClientConfig;
import com.ksy.recordlib.service.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hansentian on 7/16/15.
 */
public class DrawerItemConfigAdapter {
    private KsyRecordClientConfig config;
    private ArrayList<ConfigItem> items;
    private ArrayList<DrawerItem> drawerItems;
    private Context context;


    public List<DrawerItem> getItemViews() {
        if (config == null || context == null) {
            throw (new IllegalArgumentException("config or context should be set first"));
        }
        makeConfigItems();
        drawerItems = new ArrayList<>();
        for (ConfigItem configItem : items) {
            DrawerItem drawerItem = new DrawerItem();
            drawerItem.setTextPrimary(configItem.configName);
            drawerItem.setTextSecondary(configItem.currentValueString(config));
            drawerItems.add(drawerItem);
        }
        return drawerItems;
    }

    public DrawerItemConfigAdapter setContext(Context context) {
        this.context = context;
        return this;
    }

    public DrawerItemConfigAdapter setConfig(KsyRecordClientConfig config) {
        this.config = config;
        return this;
    }

    /**
     * set drawer items
     */
    public MaterialDialog.Builder setDialogItems(MaterialDialog.Builder builder, int pos) {
        ConfigItem drawerItem = items.get(pos);
        if (pos == 5) makeVideoProfile(drawerItem);
        builder.title(drawerItem.configName)
                .items(drawerItem.configValueName);
        return builder;
    }

    public int setDefaultSelected(int pos) {
        ConfigItem drawerItem = items.get(pos);
        int defaults = 0;
        for (int value : drawerItem.configValue) {
            if (value == drawerItem.currentValue(config)) {
                return defaults;
            }
            defaults++;
        }
        return -1;
    }

    public void onItemSelected(DrawerItem drawerItem, int position, int selected, String value) {
        if (drawerItem != null) {
            drawerItem.setTextSecondary(items.get(position).configValueName[selected]);
            items.get(position).changeValue(config, selected, value);
        }
    }

    private void makeConfigItems() {
        items = new ArrayList<>();
        ConfigItem audioSampleRate = new ConfigItem();
        audioSampleRate.index = 0;
        audioSampleRate.configName = context.getString(R.string.audio_sample_rate);
        audioSampleRate.configValue = new int[]{Constants.CONFIG_AUDIO_SAMPLERATE_44100};
        audioSampleRate.configValueName = new String[]{"44100Hz"};
        items.add(audioSampleRate);

        ConfigItem audioBitrate = new ConfigItem();
        audioBitrate.index = 1;
        audioBitrate.configName = context.getString(R.string.audio_bitrate);
        audioBitrate.configValue = new int[]{Constants.CONFIG_AUDIO_BITRATE_32K, Constants.CONFIG_AUDIO_BITRATE_48K, Constants.CONFIG_AUDIO_BITRATE_64K};
        audioBitrate.configValueName = new String[]{"32K", "48K", "64K"};
        items.add(audioBitrate);

        ConfigItem videoFrameRate = new ConfigItem();
        videoFrameRate.index = 2;
        videoFrameRate.configName = context.getString(R.string.video_frame_rate);
        videoFrameRate.configValue = new int[]{Constants.CONFIG_VIDEO_FRAME_RATE_30};
        videoFrameRate.configValueName = new String[]{"30fps"};
        items.add(videoFrameRate);

        ConfigItem videoBitrate = new ConfigItem();
        videoBitrate.index = 3;
        videoBitrate.configName = context.getString(R.string.video_bitrate);
        videoBitrate.configValue = new int[]{Constants.CONFIG_VIDEO_BITRATE_250K, Constants.CONFIG_VIDEO_BITRATE_500K, Constants.CONFIG_VIDEO_BITRATE_750K, Constants.CONFIG_VIDEO_BITRATE_1000K};
        videoBitrate.configValueName = new String[]{"250K", "500K", "750K", "1000K"};
        items.add(videoBitrate);

        ConfigItem cameraType = new ConfigItem();
        cameraType.index = 4;
        cameraType.configName = context.getString(R.string.camera_type);
        cameraType.configValue = new int[]{Constants.CONFIG_CAMERA_TYPE_BACK, Constants.CONFIG_CAMERA_TYPE_FRONT};
        cameraType.configValueName = new String[]{"back", "front"};
        items.add(cameraType);

        ConfigItem videoSize = new ConfigItem();
        videoSize.index = 5;
        videoSize.configName = context.getString(R.string.video_size);
        makeVideoProfile(videoSize);
        items.add(videoSize);
    }

    private void makeVideoProfile(ConfigItem item) {
        android.hardware.Camera camera = CameraHelper.getDefaultCamera(config.getCameraType());
        if (camera != null) {
            List<Camera.Size> sizes = camera.getParameters().getSupportedVideoSizes();
            camera.release();
            if (sizes != null) {
                item.configValueName = new String[sizes.size()];
                item.configValue = new int[sizes.size()];
                int i = 0;
                for (Camera.Size size : sizes) {
                    item.configValue[i] = CameraHelper.cameraSizeToInt(size.width, size.height);
                    StringBuffer builder = new StringBuffer();
                    builder.append(size.width);
                    builder.append("x");
                    builder.append(size.height);
                    item.configValueName[i] = builder.toString();
                    i++;
                }
            }

        }
    }


}
