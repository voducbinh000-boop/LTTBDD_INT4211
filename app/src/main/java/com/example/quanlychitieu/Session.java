package com.example.quanlychitieu;

import android.content.Context;
import android.content.SharedPreferences;

/** Lưu trạng thái đăng nhập và ngân sách tháng bằng SharedPreferences. */
public class Session {
    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("session", Context.MODE_PRIVATE);
    }
    public static void login(Context c, long userId) { sp(c).edit().putLong("uid", userId).apply(); }
    public static long userId(Context c) { return sp(c).getLong("uid", -1); }
    public static boolean isLoggedIn(Context c) { return userId(c) != -1; }
    public static void logout(Context c) { sp(c).edit().remove("uid").apply(); }

    public static long budget(Context c) { return sp(c).getLong("budget", 5000000L); }
    public static void setBudget(Context c, long v) { sp(c).edit().putLong("budget", v).apply(); }
}
