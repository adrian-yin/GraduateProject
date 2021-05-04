package com.adrianyin.arduinocar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MyBluetooth {

    private static MyBluetooth instance;

    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Boolean isConnected;
    BluetoothReadThread bluetoothReadThread;

    public MyBluetooth() {
        MyBluetooth.instance = this;
        // 开启蓝牙
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        isConnected = false;
    }

    public static MyBluetooth get() {
        if (MyBluetooth.instance == null) {
            new MyBluetooth();
        }
        return MyBluetooth.instance;
    }

    public Boolean getConnected() {
        return isConnected;
    }

    public void connect() {
        // 获取远端设备
        String TARGET_MAC = "98:D3:11:FC:78:40";
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(TARGET_MAC);
        // 创建Socket并连接
        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            bluetoothSocket.connect();
            isConnected = true;
        } catch (IOException e) {
            e.printStackTrace();
            try {
                bluetoothSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        // 初始化输入输出流
        if (isConnected) {
            try {
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 开启接收数据线程
            bluetoothReadThread = new BluetoothReadThread();
            bluetoothReadThread.start();
        }
    }

    public void close() {
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        bluetoothReadThread.interrupt();
    }

    public void writeSerial(String s) {
        try {
            outputStream.write(s.getBytes());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String readSerial() throws UnsupportedEncodingException {
        byte[] buffer = new byte[1024];
        int count = 0;
        try {
            count = inputStream.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(buffer, 0, count, StandardCharsets.UTF_8);
    }

    // 在另一个线程中接收数据
    public class BluetoothReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String s = readSerial();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
