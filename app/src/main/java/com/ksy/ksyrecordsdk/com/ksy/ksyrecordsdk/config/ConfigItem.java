package com.ksy.ksyrecordsdk.com.ksy.ksyrecordsdk.config;


import com.ksy.recordlib.service.core.CameraHelper;
import com.ksy.recordlib.service.core.KsyRecordClientConfig;
import com.ksy.recordlib.service.util.Constants;

/**
 * Created by hansentian on 7/17/15.
 */
public class ConfigItem {
    public int index;
    public String configName;
    public int[] configValue;
    public String[] configValueName;


    public int currentValue(KsyRecordClientConfig config) {
        int currentValueString = 0;
        switch (index) {
            case 0:
                currentValueString = config.getAudioSampleRate();
                break;
            case 1:
                currentValueString = config.getAudioBitRate();
                break;
            case 2:
                currentValueString = config.getVideoFrameRate();
                break;
            case 3:
                currentValueString = config.getVideoBitRate();
                break;
            case 4:
                currentValueString = config.getCameraType();
                break;
            case 5:
                currentValueString = CameraHelper.cameraSizeToInt(config.getVideoWidth(), config.getVideoHeigh());
                break;
        }
        return currentValueString;
    }

    public String currentValueString(KsyRecordClientConfig config) {
        if (config == null) {
            return null;
        }
        String currentValue = "not set";
        switch (index) {
            case 0:
                currentValue = config.getAudioSampleRate() + "Hz";
                break;
            case 1:
                currentValue = config.getAudioBitRate() / 1000 + "Kbps";
                break;
            case 2:
                currentValue = config.getVideoFrameRate() + "fps";
                break;
            case 3:
                currentValue = config.getVideoBitRate() / 1000 + "Kbps";
                break;
            case 4:
                currentValue = config.getCameraType() == Constants.CONFIG_CAMERA_TYPE_BACK ? "back" : "front";
                break;
            case 5:
                currentValue = config.getVideoWidth() + "x" + config.getVideoHeigh();
                break;
            case 6:
                currentValue = configValueName[0];
                break;
        }
        return currentValue;

    }

    public void changeValue(KsyRecordClientConfig config, int selected, String value) {
        switch (index) {
            case 0:
                config.setmAudioSampleRate(configValue[selected]);
                break;
            case 1:
                config.setmAudioBitRate(configValue[selected]);
                break;
            case 2:
                config.setmVideoFrameRate(configValue[selected]);
                break;
            case 3:
                config.setmVideoBitRate(configValue[selected]);
                break;
            case 4:
                config.setmCameraType(configValue[selected]);
                break;
            case 5:
                config.setmVideoWidth(CameraHelper.intToCameraWidth(configValue[selected]));
                config.setmVideoHeigh(CameraHelper.intToCameraHeight(configValue[selected]));
                break;
            case 6:
                config.setmUrl(value);
                break;
        }
    }

}
