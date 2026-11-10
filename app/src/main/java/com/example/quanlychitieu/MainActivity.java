package com.example.quanlychitieu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this) || DBHelper.get(this).getUser(Session.userId(this)) == null) {
            Session.logout(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        // Khởi tạo các kênh thông báo
        NotificationHelper.initChannels(this);

        // Đăng ký lịch nhắc nhở hàng ngày mặc định (nếu đã bật)
        SharedPreferences sp = NotificationHelper.getPrefs(this);
        if (sp.getBoolean(NotificationHelper.KEY_DAILY_ENABLED, true)) {
            int h = sp.getInt(NotificationHelper.KEY_DAILY_HOUR, 20);
            int m = sp.getInt(NotificationHelper.KEY_DAILY_MINUTE, 0);
            NotificationHelper.scheduleDailyReminder(this, h, m);
        }

        // Tự động kiểm tra và tạo các giao dịch định kỳ đến hạn
        long uid = Session.userId(this);
        DBHelper.get(this).processDueRecurringTransactions(uid);
        NotificationHelper.checkAndNotifyBudget(this, uid);

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment f;
            if (id == R.id.nav_home) {
                f = new HomeFragment();
            } else if (id == R.id.nav_history) {
                f = new HistoryFragment();
            } else if (id == R.id.nav_budget) {
                f = new BudgetFragment();
            } else if (id == R.id.nav_stats) {
                f = new StatsFragment();
            } else {
                f = new ProfileFragment();
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.container, f).commit();
            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    /** Cho phép Fragment chuyển tab */
    public void selectTab(int menuId) {
        bottomNav.setSelectedItemId(menuId);
    }
}
