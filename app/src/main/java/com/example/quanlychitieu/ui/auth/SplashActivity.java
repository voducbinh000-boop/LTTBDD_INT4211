package com.example.quanlychitieu.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.db.DBHelper;
import com.example.quanlychitieu.ui.activity.MainActivity;
import com.example.quanlychitieu.util.Session;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Áp dụng cấu hình chế độ giao diện đã lưu
        AppCompatDelegate.setDefaultNightMode(Session.getThemeMode(this));
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
