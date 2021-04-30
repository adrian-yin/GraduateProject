package com.adrianyin.car;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements SocketClient.Callback {

    private static final String TAG = "MainActivity";

    SurfaceViewRenderer localView;
    SurfaceViewRenderer remoteView;

    PeerConnectionFactory peerConnectionFactory;
    PeerConnection peerConnection;
    EglBase.Context eglBaseContext;
    VideoTrack videoTrack;
    MediaStream mediaStream;
    MediaConstraints mediaConstraints;

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

        // 初始化组件
        initComponents();

        // 创建peerConnectionFactory
        createPeerConnectionFactory();
        // 配置mediaConstraints
        setMediaConstraints();
        // 获取本地视频并创建媒体流
        getLocalVideo();
        // 连接信令服务器并绑定回调函数
        SocketClient.get().setCallback(this);
        // 创建peerConnection
        createPeerConnection();
        // 添加本地媒体流到节点
        peerConnection.addStream(mediaStream);
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

        remoteView = findViewById(R.id.remoteView);
        remoteView.setMirror(true);
        remoteView.setKeepScreenOn(true);
        remoteView.setEnableHardwareScaler(false);
        remoteView.init(eglBaseContext, null);
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

        mediaStream = peerConnectionFactory.createLocalMediaStream("mediaSteam");
        mediaStream.addTrack(videoTrack);
    }

    private void createPeerConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(
                PeerConnection.IceServer
                        .builder("stun:stun.l.google.com:19302")
                        .createIceServer()
        );
        peerConnection = peerConnectionFactory.createPeerConnection(
                iceServers,
                new PeerConnectionAdapter() {
                    @Override
                    public void onIceCandidate(IceCandidate iceCandidate) {
                        super.onIceCandidate(iceCandidate);
                        SocketClient.get().sendIceCandidate(iceCandidate);
                    }

                    @Override
                    public void onAddStream(MediaStream mediaStream) {
                        super.onAddStream(mediaStream);
                        VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                        runOnUiThread(() -> remoteVideoTrack.addSink(remoteView));
                    }
                }
        );
    }

    private void setMediaConstraints() {
        mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
    }


    // 实现SocketClient事件回调函数

    // 收到offer设置remote sdp；并创建answer同时设置local sdp
    @Override
    public void onOfferReceived(JSONObject data) {
        runOnUiThread(() -> {
            peerConnection.setRemoteDescription(new SdpAdapter("SetRemote"),
                    new SessionDescription(SessionDescription.Type.OFFER, data.optString("sdp")));
            peerConnection.createAnswer(new SdpAdapter("Answer") {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    super.onCreateSuccess(sessionDescription);
                    peerConnection.setLocalDescription(
                            new SdpAdapter("SetLocal"), sessionDescription);
                    SocketClient.get().sendSessionDescription(sessionDescription);
                }
            }, mediaConstraints);
        });
    }

    // 收到Answer设置remote sdp
    @Override
    public void onAnswerReceived(JSONObject data) {
        peerConnection.setRemoteDescription(new SdpAdapter("SetRemote"),
                new SessionDescription(SessionDescription.Type.ANSWER, data.optString("sdp")));
    }

    // 收到候选人信息加入候选人
    @Override
    public void onIceCandidateReceived(JSONObject data) {
        peerConnection.addIceCandidate(new IceCandidate(
                data.optString("id"),
                data.optInt("label"),
                data.optString("candidate")
        ));
    }

    @Override
    public void onCreateRoom() {

    }

    // 后加入的节点发送offer
    @Override
    public void onSelfJoined() {
        peerConnection.createOffer(new SdpAdapter("Offer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                peerConnection.setLocalDescription(
                        new SdpAdapter("SetLocal"), sessionDescription);
                SocketClient.get().sendSessionDescription(sessionDescription);
            }
        }, mediaConstraints);
    }

    @Override
    public void onPeerJoined() {

    }

    @Override
    public void onPeerLeave(String message) {

    }
}