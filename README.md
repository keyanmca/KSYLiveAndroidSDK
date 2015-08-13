#KSLiveSDK for Android使用指南
---
##SDK说明
KSLiveSDK for Android(以下简称SDK)是基于RTMP的推流器

###开发环境
本SDK使用了Android Studio + Gradle的方式进行构建,暂不支持eclipse的方式,目前处于测试阶段,只提供工程项目,日后会发布AAR形式的SKD,并上传Gradle仓库。
目前使用gradle1.2.3构建通过

###运行环境
目前SDK使用MediaRecorder的方式利用Android硬件资源进行编码,主要是支持H264和AAC方式的编码,主要支持API14(android4.0)以上设备。

##集成使用引导

###SDK结构
- recordlib 推流器的核心组件,需要以library形式引入项目
- app SDK的demo工程,包含简单的使用Sample

###声明权限
- Android权限申明

```
	<!-- 使用权限 -->
    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.RECORD_AUDIO"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
	<!-- 硬件特性 -->
    <uses-feature android:name="android.hardware.camera" />
    <uses-feature android:name="android.hardware.camera.autofocus" />
```

###使用

- 可以直接clone工程之后 使用Android Stuido的Import project(Eclipse ADT,Gradle,etc.)导入工程,app即为示例Demo,直接运行即可.

###集成
####初始化

- 创建KsyRecordClient实例 KsyRecordCLient是SDK的核心控制器,一切交互通过这个类进行交互.KsyRecordClient为单例模式,全局只能创建一个.创建后必须先设置Config和预览的SurfaceView或TextureView

```
	client = KsyRecordClient.getInstance(getApplicationContext());
	client.setConfig(config);
	client.setDisplayPreview(mSurfaceView);

```
- KsyRecordClientConfig 为Client的配置,可以使用自带的Builder简单构建,config主要配置推流器音视频大小编码类型采样率等。必须要设置推流的server地址，简单配置可以直接使用VideoProfile进行初始化.

```
	KsyRecordClientConfig.Builder builder = new KsyRecordClientConfig.Builder();
	builder.setVideoProfile(CamcorderProfile.QUALITY_480P).setUrl(Constants.URL_DEFAULT);
	config = builder.build();
```
####开始停止推留

- 开始推流
```
 client.startRecord();
```

- 停止推流
```
 client.stopRecord();
```

##已知问题
- 音视频同步问题,目前使用MediaRecorder的3GPP封装没有timestemp,需要手工编码TS
,导致音视频播放时候可能出现帧间隔过大的情况,在某些手机上(Mi2A, Meizu MX4Pro)可能会导致音视频帧间隔改变导致的画面轻微跳动卡顿等,这个问题会在日后通过优化同步时钟或使用其他封装格式实现
