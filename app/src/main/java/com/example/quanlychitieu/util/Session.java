package com.example.quanlychitieu.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Lưu trạng thái đăng nhập, ngân sách tháng và cấu hình chế độ giao diện (Dark Mode). */
public class Session {
    public static final String KEY_THEME = "app_theme_mode";
    public static final int THEME_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; // -1
    public static final int THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;             // 1
    public static final int THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES;            // 2

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("session", Context.MODE_PRIVATE);
    }

    public static void login(Context c, long userId) { sp(c).edit().putLong("uid", userId).apply(); }
    public static long userId(Context c) { return sp(c).getLong("uid", -1); }
    public static boolean isLoggedIn(Context c) { return userId(c) != -1; }
    public static void logout(Context c) { sp(c).edit().remove("uid").apply(); }

    public static long budget(Context c) { return sp(c).getLong("budget", 5000000L); }
    public static void setBudget(Context c, long v) { sp(c).edit().putLong("budget", v).apply(); }

    public static int getThemeMode(Context c) {
        return sp(c).getInt(KEY_THEME, THEME_SYSTEM);
    }

    public static void setThemeMode(Context c, int mode) {
        sp(c).edit().putInt(KEY_THEME, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
