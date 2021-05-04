package com.adrianyin.arduinocar;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

public class LocalActivity extends AppCompatActivity {

    Button toRemoteButton;
    ImageButton forwardButton;
    ImageButton backwardButton;
    ImageButton leftButton;
    ImageButton rightButton;

    private static final int FORWARD = 0;
    private static final int BACKWARD = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local);
        init();

        // 跳转至远程控制活动
        toRemoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        LocalActivity.this, RemoteActivity.class);
                startActivity(intent);
            }
        });

        // 发送控制小车移动胡蓝牙数据
        forwardButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                sendCarCommand(FORWARD, event);
                return false;
            }
        });
        backwardButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                sendCarCommand(BACKWARD, event);
                return false;
            }
        });
        leftButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                sendCarCommand(LEFT, event);
                return false;
            }
        });
        rightButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                sendCarCommand(RIGHT, event);
                return false;
            }
        });
    }

    private void init() {
        toRemoteButton = findViewById(R.id.toRemoteButton);

        forwardButton = findViewById(R.id.forwardButton);
        backwardButton = findViewById(R.id.backwardButton);
        leftButton = findViewById(R.id.leftButton);
        rightButton = findViewById(R.id.rightButton);
    }

    // 向小车发送遥控指令
    private void sendCarCommand(int direction, MotionEvent event) {
        // 按下移动，松开停止
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            String s = "";
            switch (direction) {
                case FORWARD:
                    s = "w";
                    break;
                case BACKWARD:
                    s = "x";
                    break;
                case LEFT:
                    s = "a";
                    break;
                case RIGHT:
                    s = "d";
                    break;
                default:
                    break;
            }
            MyBluetooth.get().writeSerial(s);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            MyBluetooth.get().writeSerial("s");
        }
    }
}