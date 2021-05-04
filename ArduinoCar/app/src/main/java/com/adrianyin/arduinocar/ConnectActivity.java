package com.adrianyin.arduinocar;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ConnectActivity extends AppCompatActivity {

    Button connectButton;
    TextView connectText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        init();

        connectText.setText("请先点击下方按钮连接小车蓝牙");

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectText.setText("连接中，请稍后...");
                MyBluetooth.get().connect();
                if (MyBluetooth.get().getConnected()) {
                    connectText.setText("连接成功！");
                    Toast.makeText(
                            getApplicationContext(), "连接成功", Toast.LENGTH_LONG).show();
                    // 跳转至remote页
                    Intent intent = new Intent(
                            ConnectActivity.this, RemoteActivity.class);
                    startActivity(intent);
                } else {
                    connectText.setText("连接失败，请检查是否已开启蓝牙及小车电源");
                }
            }
        });
    }

    private void init() {
        connectButton = findViewById(R.id.connectButton);
        connectText = findViewById(R.id.connectText);
    }
}