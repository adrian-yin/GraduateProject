package com.adrianyin.car;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;

import org.webrtc.Camera1Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    SurfaceViewRenderer localView;

    PeerConnectionFactory peerConnectionFactory;
    EglBase.Context eglBaseContext;
    VideoTrack videoTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 申请权限
        String[] permissions = {
                Manifest.permission.CAMERA
        };
        if (!EasyPermissions.hasPermissions(this, permissions)) {
            EasyPermissions.requestPermissions(
                    this, "需要获取相机权限", 0, permissions);
        }

        initComponents();

        // WebRTC传输视频
        createPeerConnectionFactory();
        getLocalVideo();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    // 组件初始化及配置
    private void initComponents() {

        eglBaseContext = EglBase.create().getEglBaseContext();

        localView = findViewById(R.id.localView);
        localView.setMirror(true);
        localView.setKeepScreenOn(true);
        localView.setEnableHardwareScaler(false);
        localView.init(eglBaseContext, null);
    }

    // 创建peerConnectionFactory
    private void createPeerConnectionFactory() {

        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions
                        .builder(this)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory =
                new DefaultVideoEncoderFactory(eglBaseContext,
                        true,
                        true
                );
        DefaultVideoDecoderFactory defaultVideoDecoderFactory =
                new DefaultVideoDecoderFactory(eglBaseContext);

        peerConnectionFactory =
                PeerConnectionFactory.builder()
                        .setOptions(options)
                        .setVideoEncoderFactory(defaultVideoEncoderFactory)
                        .setVideoDecoderFactory(defaultVideoDecoderFactory)
                        .createPeerConnectionFactory();
    }

    // 创建视频捕获器
    private VideoCapturer createCameraCapturer() {
        Camera1Enumerator enumerator = new Camera1Enumerator(false);
        final String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        return null;
    }

    // 获取本地视频并渲染
    private void getLocalVideo() {

        SurfaceTextureHelper surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", eglBaseContext);

        VideoCapturer videoCapturer = createCameraCapturer();
        VideoSource videoSource =
                peerConnectionFactory.createVideoSource(false);
        assert videoCapturer != null;
        videoCapturer.initialize(
                surfaceTextureHelper,getApplicationContext(),videoSource.getCapturerObserver());
        videoCapturer.startCapture(480, 640, 30);

        videoTrack = peerConnectionFactory.createVideoTrack("carCamera", videoSource);
        videoTrack.addSink(localView);
    }


}