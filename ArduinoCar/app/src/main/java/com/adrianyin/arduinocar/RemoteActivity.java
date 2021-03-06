package com.adrianyin.arduinocar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;

import org.json.JSONObject;

import io.agora.rtc.IRtcChannelEventHandler;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import pub.devrel.easypermissions.EasyPermissions;

public class RemoteActivity extends AppCompatActivity implements SocketClient.Callback {

    private static final String TAG = "RemoteActivity";

    private Button toLocalButton;
    private MapView mapView;

    private AMap aMap;
    private RtcEngine rtcEngine;
    private final IRtcEngineEventHandler iRtcEngineEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onJoinChannelSuccess(String channel, final int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("agora", "Join channel success, uid: " + (uid & 0xFFFFFFFFL));
                }
            });
        }

        @Override
        public void onUserJoined(final int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("agora", "Remote user joined, uid: " + (uid & 0xFFFFFFFFL));
                }
            });
        }

        @Override
        public void onUserOffline(final int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("agora","User offline, uid: " + (uid & 0xFFFFFFFFL));
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
        init();

        // ????????????
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS
        };
        if (!EasyPermissions.hasPermissions(this, permissions)) {
            EasyPermissions.requestPermissions(
                    this, "????????????????????????", 0, permissions);
        }

        // ??????socket????????????????????????
        SocketClient.get().setCallback(this);

        // ???????????????????????????????????????
        toLocalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        RemoteActivity.this, LocalActivity.class);
                startActivity(intent);
            }
        });

        // ????????????
        mapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mapView.getMap();
        }

        // ????????????????????????
        UiSettings uiSettings = aMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(false);  // ????????????
        uiSettings.setAllGesturesEnabled(false); // ??????????????????

        // ????????????????????????
        float zoomLevel = 18.5f;  // ????????????3-19??????????????????
        aMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel));

        // ?????????????????????
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
        myLocationStyle.interval(2000);
        myLocationStyle.showMyLocation(true);
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true);
        aMap.setOnMyLocationChangeListener(new AMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                SocketClient.get().sendLocation(location);
            }
        });

        // ?????????RtcEngine???????????????
        initEngineAndJoinChannel();

        // ??????????????????
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void init() {
        toLocalButton = findViewById(R.id.toLocalButton);
        mapView = findViewById(R.id.map);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        rtcEngine.leaveChannel();
        RtcEngine.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onCommandReceived(JSONObject data) {
        // ??????????????????
        Log.d(TAG, "onCommandReceived");
        String command = data.optString("command");
        MyBluetooth.get().writeSerial(command);
    }

    @Override
    public void onCreateRoom() {
        Log.d(TAG, "onCreateRoom");
    }

    @Override
    public void onSelfJoined() {
        Log.d(TAG, "onSelfJoined");
    }

    @Override
    public void onPeerJoined() {
        Log.d(TAG, "onPeerJoined");
    }

    @Override
    public void onPeerLeave(String message) {
        Log.d(TAG, "onPeerLeave");
    }

    // ?????????RtcEngine???????????????
    private void initEngineAndJoinChannel() {
        initializeEngine();
        setupLocalVideo();
        joinChannel();
        rtcEngine.switchCamera();
    }

    private void initializeEngine() {
        try {
            rtcEngine = RtcEngine.create(
                    getBaseContext(),
                    getString(R.string.agora_app_id),
                    iRtcEngineEventHandler);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void setupLocalVideo() {
        rtcEngine.enableVideo();
        SurfaceView surfaceView;
        surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        VideoCanvas videoCanvas = new VideoCanvas(
                surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0);
        rtcEngine.setupLocalVideo(videoCanvas);
    }

    private void joinChannel() {
        rtcEngine.joinChannel(
                getString(R.string.agora_token),
                "test",
                "Extra Optional Data",
                0
        );
    }
}