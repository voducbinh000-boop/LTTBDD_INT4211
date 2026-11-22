package com.example.quanlychitieu.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.model.Transaction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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

    /** Lưu ảnh được chọn từ thư viện vào bộ nhớ riêng của ứng dụng để hiển thị lâu dài */
    public static String saveAvatarFromUri(Context context, Uri uri, long userId) {
        try {
            File dir = new File(context.getFilesDir(), "avatars");
            if (!dir.exists()) dir.mkdirs();
            File destFile = new File(dir, "avatar_" + userId + ".jpg");

            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;

            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            if (bitmap == null) return null;

            // Giới hạn độ phân giải tối đa 512x512 để tiết kiệm bộ nhớ và tải mượt mà
            int maxDim = 512;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxDim || height > maxDim) {
                float ratio = (float) width / (float) height;
                if (ratio > 1) {
                    width = maxDim;
                    height = (int) (maxDim / ratio);
                } else {
                    height = maxDim;
                    width = (int) (maxDim * ratio);
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            FileOutputStream fos = new FileOutputStream(destFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();

            return destFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Xóa ảnh đại diện cục bộ */
    public static void deleteAvatar(Context context, long userId) {
        try {
            File dir = new File(context.getFilesDir(), "avatars");
            File destFile = new File(dir, "avatar_" + userId + ".jpg");
            if (destFile.exists()) {
                destFile.delete();
            }
        } catch (Exception ignored) {}
    }

    /** Tải và hiển thị ảnh đại diện lên ImageView, nếu không có ảnh thì hiển thị chữ cái đầu tiên */
    public static void loadAvatar(Context context, String avatarPath, ImageView iv, TextView tvFallback, String name) {
        if (avatarPath != null && !avatarPath.isEmpty()) {
            File f = new File(avatarPath);
            if (f.exists() && f.isFile()) {
                try {
                    Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath());
                    if (bm != null) {
                        iv.setImageBitmap(bm);
                        iv.setVisibility(View.VISIBLE);
                        if (tvFallback != null) tvFallback.setVisibility(View.GONE);
                        return;
                    }
                } catch (Exception ignored) {}
            }
        }
        iv.setImageDrawable(null);
        iv.setVisibility(View.GONE);
        if (tvFallback != null) {
            tvFallback.setVisibility(View.VISIBLE);
            tvFallback.setText(name == null || name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        }
    }
}
