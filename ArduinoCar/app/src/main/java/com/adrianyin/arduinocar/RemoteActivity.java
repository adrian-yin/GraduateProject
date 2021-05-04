package com.adrianyin.arduinocar;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class RemoteActivity extends AppCompatActivity {

    Button toLocalButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
        init();

        // 跳转至本地遥控页面按钮事件
        toLocalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        RemoteActivity.this, LocalActivity.class);
                startActivity(intent);
            }
        });
    }

    private void init() {
        toLocalButton = findViewById(R.id.toLocalButton);
    }
}