package com.adrianyin.car;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class SdpAdapter implements SdpObserver {

    private final String TAG;

    public SdpAdapter(String tag) {
        TAG = "SdpAdapter" + tag;
    }

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.i(TAG, "onCreateSuccess: " + sessionDescription);
    }

    @Override
    public void onSetSuccess() {
        Log.i(TAG, "onSetSuccess");
    }

    @Override
    public void onCreateFailure(String s) {
        Log.e(TAG, "onCreateFailure: " + s);
    }

    @Override
    public void onSetFailure(String s) {
        Log.e(TAG, "onSetFailure: " + s);
    }
}
