package com.example.quanlychitieu;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

public class NotificationSettingsActivity extends AppCompatActivity {
    private SwitchMaterial switchDaily, switchBudget, switchRecurring;
    private View rowTime;
    private TextView tvTime;
    private SharedPreferences sp;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Utils.toast(this, "Đã cấp quyền thông báo thành công");
                } else {
                    Utils.toast(this, "Bạn cần cấp quyền để ứng dụng gửi được thông báo");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);
        NotificationHelper.initChannels(this);
        checkNotificationPermission();

        sp = NotificationHelper.getPrefs(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        switchDaily = findViewById(R.id.switchDaily);
        switchBudget = findViewById(R.id.switchBudget);
        switchRecurring = findViewById(R.id.switchRecurring);
        rowTime = findViewById(R.id.rowTime);
        tvTime = findViewById(R.id.tvTime);

        boolean dailyOn = sp.getBoolean(NotificationHelper.KEY_DAILY_ENABLED, true);
        boolean budgetOn = sp.getBoolean(NotificationHelper.KEY_BUDGET_ENABLED, true);
        boolean recurringOn = sp.getBoolean(NotificationHelper.KEY_RECURRING_ENABLED, true);
        int hour = sp.getInt(NotificationHelper.KEY_DAILY_HOUR, 20);
        int min = sp.getInt(NotificationHelper.KEY_DAILY_MINUTE, 0);

        switchDaily.setChecked(dailyOn);
        switchBudget.setChecked(budgetOn);
        switchRecurring.setChecked(recurringOn);
        rowTime.setVisibility(dailyOn ? View.VISIBLE : View.GONE);
        tvTime.setText(String.format(Locale.US, "%02d:%02d", hour, min));

        switchDaily.setOnCheckedChangeListener((btn, isChecked) -> {
            sp.edit().putBoolean(NotificationHelper.KEY_DAILY_ENABLED, isChecked).apply();
            rowTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                int h = sp.getInt(NotificationHelper.KEY_DAILY_HOUR, 20);
                int m = sp.getInt(NotificationHelper.KEY_DAILY_MINUTE, 0);
                NotificationHelper.scheduleDailyReminder(this, h, m);
                Utils.toast(this, "Đã bật nhắc nhở lúc " + String.format(Locale.US, "%02d:%02d", h, m));
            } else {
                NotificationHelper.cancelDailyReminder(this);
                Utils.toast(this, "Đã tắt nhắc nhở hàng ngày");
            }
        });

        rowTime.setOnClickListener(v -> {
            int curH = sp.getInt(NotificationHelper.KEY_DAILY_HOUR, 20);
            int curM = sp.getInt(NotificationHelper.KEY_DAILY_MINUTE, 0);
            new TimePickerDialog(this, (view, h, m) -> {
                sp.edit()
                        .putInt(NotificationHelper.KEY_DAILY_HOUR, h)
                        .putInt(NotificationHelper.KEY_DAILY_MINUTE, m)
                        .apply();
                tvTime.setText(String.format(Locale.US, "%02d:%02d", h, m));
                NotificationHelper.scheduleDailyReminder(this, h, m);
                Utils.toast(this, "Đã cập nhật giờ nhắc: " + String.format(Locale.US, "%02d:%02d", h, m));
            }, curH, curM, true).show();
        });

        switchBudget.setOnCheckedChangeListener((btn, isChecked) -> {
            sp.edit().putBoolean(NotificationHelper.KEY_BUDGET_ENABLED, isChecked).apply();
            Utils.toast(this, isChecked ? "Đã bật cảnh báo ngân sách" : "Đã tắt cảnh báo ngân sách");
        });

        switchRecurring.setOnCheckedChangeListener((btn, isChecked) -> {
            sp.edit().putBoolean(NotificationHelper.KEY_RECURRING_ENABLED, isChecked).apply();
            Utils.toast(this, isChecked ? "Đã bật nhắc hóa đơn định kỳ" : "Đã tắt nhắc hóa đơn định kỳ");
        });

        findViewById(R.id.btnTestNotification).setOnClickListener(v -> {
            checkNotificationPermission();
            NotificationHelper.sendDailyReminderNotification(this);
            NotificationHelper.sendBudgetAlertNotification(this,
                    "⚠️ Thử nghiệm cảnh báo ngân sách",
                    "Bạn đã chi tiêu 85% ngân sách tháng này. Vui lòng kiểm soát chi tiêu hợp lý!",
                    3999);
            Utils.toast(this, "Đã gửi thông báo thử nghiệm lên thanh trạng thái!");
        });
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
