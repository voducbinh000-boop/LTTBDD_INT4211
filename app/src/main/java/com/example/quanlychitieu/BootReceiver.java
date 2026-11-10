package com.example.quanlychitieu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences sp = NotificationHelper.getPrefs(context);
            boolean enabled = sp.getBoolean(NotificationHelper.KEY_DAILY_ENABLED, true);
            if (enabled) {
                int hour = sp.getInt(NotificationHelper.KEY_DAILY_HOUR, 20);
                int minute = sp.getInt(NotificationHelper.KEY_DAILY_MINUTE, 0);
                NotificationHelper.scheduleDailyReminder(context, hour, minute);
            }
        }
    }
}
