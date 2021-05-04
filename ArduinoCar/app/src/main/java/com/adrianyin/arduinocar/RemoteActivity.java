package com.adrianyin.arduinocar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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

public class RemoteActivity extends AppCompatActivity implements SocketClient.Callback {

    private static final String TAG = "RemoteActivity";

    private Button toLocalButton;
    private MapView mapView;

    private AMap aMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
        init();

        // 连接socket服务器并绑定事件
        SocketClient.get().setCallback(this);

        // 跳转至本地遥控页面按钮事件
        toLocalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        RemoteActivity.this, LocalActivity.class);
                startActivity(intent);
            }
        });

        // 创建地图
        mapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mapView.getMap();
        }

        // 配置地图显示内容
        UiSettings uiSettings = aMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(false);  // 缩放按钮
        uiSettings.setAllGesturesEnabled(false); // 禁用所有手势

        // 配置地图缩放级别
        float zoomLevel = 18.5f;  // 缩放级别3-19，越大越精细
        aMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel));

        // 获取自身定位点
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
    }

    private void init() {
        toLocalButton = findViewById(R.id.toLocalButton);
        mapView = findViewById(R.id.map);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
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
        // 发送蓝牙命令
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
}