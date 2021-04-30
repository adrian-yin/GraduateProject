package com.adrianyin.car;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketClient {

    private static final String TAG = "SocketClient";
    // socket客户端实例
    private static SocketClient instance;

    private Socket socket;
    private Callback callback;

    private final String roomName = "car";

    private final TrustManager[] trustAll = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    Log.i(TAG, "checkClientTrusted");
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    Log.i(TAG, "checkServerTrusted");
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
    };

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
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, null);
            IO.setDefaultHostnameVerifier(((hostname, session) -> true));
            IO.setDefaultSSLContext(sslContext);

            // 建立到信令服务器的连接
            String serverURI = "https://yinxu.monster:9123?userType=car";
            socket = IO.socket(serverURI);
            socket.connect();
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
                Object arg = args[0];
                if (arg instanceof JSONObject) {
                    JSONObject data = (JSONObject) arg;
                    String type = data.optString("type");
                    switch (type) {
                        case "offer":
                            callback.onOfferReceived(data);
                            break;
                        case "answer":
                            callback.onAnswerReceived(data);
                            break;
                        case "candidate":
                            callback.onIceCandidateReceived(data);
                            break;
                        default:
                            break;
                    }
                }
            });

        } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // 发送候选人信息
    public void sendIceCandidate(IceCandidate iceCandidate) {
        JSONObject message = new JSONObject();
        try {
            message.put("type", "candidate");
            message.put("label", iceCandidate.sdpMLineIndex);
            message.put("id", iceCandidate.sdpMid);
            message.put("candidate", iceCandidate.sdp);

            socket.emit("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 发送Sdp信息
    public void sendSessionDescription(SessionDescription sdp) {
        JSONObject message = new JSONObject();
        try {
            message.put("type", sdp.type.canonicalForm());
            message.put("sdp", sdp.description);

            socket.emit("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 事件回调函数接口
    public interface Callback {

        void onOfferReceived(JSONObject data);
        void onAnswerReceived(JSONObject data);
        void onIceCandidateReceived(JSONObject data);

        void onCreateRoom();
        void onSelfJoined();
        void onPeerJoined();
        void onPeerLeave(String message);
    }
}
