package com.example.quanlychitieu;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        findViewById(R.id.btnStart).setOnClickListener(v -> {
            // Đã đăng nhập thì vào thẳng Trang chủ, chưa thì sang Đăng nhập
            Class<?> target = Session.isLoggedIn(this) && DBHelper.get(this).getUser(Session.userId(this)) != null
                    ? MainActivity.class : LoginActivity.class;
            startActivity(new Intent(this, target));
            finish();
        });
    }
}
