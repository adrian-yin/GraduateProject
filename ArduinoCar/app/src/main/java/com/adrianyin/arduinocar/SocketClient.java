package com.adrianyin.arduinocar;

import android.location.Location;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Arrays;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketClient {

    private static final String TAG = "SocketClient";
    // socket客户端实例
    private static SocketClient instance;

    private Socket socket;
    private Callback callback;

    private final String roomName = "car";

    // 构造时初始化
    private SocketClient() {
        init();
    }

    // 获取Socket客户端实例
    public static SocketClient get() {
        if (instance == null) {
            synchronized (SocketClient.class) {
                if (instance == null) {
                    instance = new SocketClient();
                }
            }
        }
        return instance;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private void init() {
        try {
            IO.Options options = new IO.Options();
            SocketSSL.set(options);
            // 建立到信令服务器的连接
            String serverURI = "https://yinxu.monster:9123";
            socket = IO.socket(serverURI, options);
            socket = socket.connect();
            // 发送创建或加入指定房间的要求
            socket.emit("create or join", roomName);

            socket.on("created", args -> {
                Log.i(TAG, "成功创建房间" + roomName);
                callback.onCreateRoom();
            });
            socket.on("full", args ->
                    Log.e(TAG, "房间已满，加入失败"));
            socket.on("join", args -> {
                Log.i(TAG, "有节点加入房间");
                callback.onPeerJoined();
            });
            socket.on("joined", args -> {
                Log.i(TAG, "成功加入房间" + roomName);
                callback.onSelfJoined();
            });
            socket.on("log", args ->
                    Log.i(TAG, "服务器日志信息：" + Arrays.toString(args)));
            socket.on("bye", args -> {
                Log.i(TAG, "节点" + args[0] + "退出房间");
                callback.onPeerLeave((String) args[0]);
            });
            socket.on("message", args -> {
                Log.i(TAG, "收到消息：" + Arrays.toString(args));
                try {
                    JSONObject data = new JSONObject(args[0].toString());
                    String type = data.optString("type");
                    switch (type) {
                        case "command":
                            callback.onCommandReceived(data);
                            break;
                        default:
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void sendLocation(Location location) {
        JSONObject data = new JSONObject();
        try {
            data.put("type", "location");
            data.put("longitude", location.getLongitude());
            data.put("latitude", location.getLatitude());

            socket.emit("message", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 事件回调函数接口
    public interface Callback {
        void onCommandReceived(JSONObject data);

        void onCreateRoom();
        void onSelfJoined();
        void onPeerJoined();
        void onPeerLeave(String message);
    }
}
