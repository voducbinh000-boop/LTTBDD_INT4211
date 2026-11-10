package com.example.quanlychitieu;

import android.content.Context;
import android.widget.Toast;

import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

public class Utils {

    /** 5250000 -> "5.250.000đ" */
    public static String money(long v) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.US);
        s.setGroupingSeparator('.');
        return new DecimalFormat("#,###", s).format(v) + "đ";
    }

    public static String signedMoney(Transaction t) {
        return (t.isIncome() ? "+" : "-") + money(t.amount);
    }

    public static String toIso(int y, int m, int d) {
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d);
    }

    public static String toIso(Calendar c) {
        return toIso(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static String today() { return toIso(Calendar.getInstance()); }

    /** 2026-11-01 -> 01/11/2026 */
    public static String display(String iso) {
        String[] p = iso.split("-");
        return p[2] + "/" + p[1] + "/" + p[0];
    }

    /** Hiển thị "Hôm nay", "Hôm qua" hoặc ngày đầy đủ. */
    public static String relative(String iso) {
        if (iso.equals(today())) return "Hôm nay";
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -1);
        if (iso.equals(toIso(c))) return "Hôm qua";
        return display(iso);
    }

    public static int iconRes(String key) {
        if (key == null) return R.drawable.ic_category;
        switch (key) {
            case "food": return R.drawable.ic_food;
            case "shopping": return R.drawable.ic_shopping;
            case "transport": return R.drawable.ic_transport;
            case "salary":
            case "wallet": return R.drawable.ic_wallet;
            default: return R.drawable.ic_category;
        }
    }

    public static long parseAmount(String s) {
        String digits = s == null ? "" : s.replaceAll("[^0-9]", "");
        if (digits.isEmpty() || digits.length() > 12) return -1;
        return Long.parseLong(digits);
    }

    public static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return s;
        }
    }

    public static void toast(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
    }
}
