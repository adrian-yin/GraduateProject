package com.adrianyin.arduinocar;

import android.content.Context;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationListener;

public class MyMap {

    private final Context applicationContext;

    private AMapLocationClient aMapLocationClient;

    public MyMap(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    public AMapLocationClient getaMapLocationClient() {
        return aMapLocationClient;
    }

    public void startLocate() {
        aMapLocationClient = new AMapLocationClient(applicationContext);
        aMapLocationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation != null) {
                    if (aMapLocation.getErrorCode() == 0) {
                        // 解析定位结果

                    }
                }
            }
        });
        aMapLocationClient.startLocation();
    }
}
