package com.example.quanlychitieu;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

public class NotificationHelper {
    public static final String CHANNEL_DAILY = "channel_daily_reminder";
    public static final String CHANNEL_BUDGET = "channel_budget_alert";
    public static final String CHANNEL_RECURRING = "channel_recurring_bill";

    private static final String PREF_NAME = "notification_settings";
    public static final String KEY_DAILY_ENABLED = "daily_enabled";
    public static final String KEY_DAILY_HOUR = "daily_hour";
    public static final String KEY_DAILY_MINUTE = "daily_minute";
    public static final String KEY_BUDGET_ENABLED = "budget_enabled";
    public static final String KEY_RECURRING_ENABLED = "recurring_enabled";

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void initChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm == null) return;

            // Kênh 1: Nhắc nhở ghi chép hàng ngày
            NotificationChannel c1 = new NotificationChannel(
                    CHANNEL_DAILY,
                    "Nhắc nhở ghi chép chi tiêu",
                    NotificationManager.IMPORTANCE_DEFAULT);
            c1.setDescription("Thông báo nhắc nhở cập nhật chi tiêu vào mỗi buổi tối");

            // Kênh 2: Cảnh báo ngân sách
            NotificationChannel c2 = new NotificationChannel(
                    CHANNEL_BUDGET,
                    "Cảnh báo hạn mức ngân sách",
                    NotificationManager.IMPORTANCE_HIGH);
            c2.setDescription("Cảnh báo khi chi tiêu đạt hoặc vượt 80%, 100% ngân sách");

            // Kênh 3: Hóa đơn & Giao dịch định kỳ
            NotificationChannel c3 = new NotificationChannel(
                    CHANNEL_RECURRING,
                    "Nhắc hóa đơn định kỳ",
                    NotificationManager.IMPORTANCE_HIGH);
            c3.setDescription("Nhắc các khoản chi tiêu / hóa đơn sắp đến hạn thanh toán");

            nm.createNotificationChannel(c1);
            nm.createNotificationChannel(c2);
            nm.createNotificationChannel(c3);
        }
    }

    /** Lên lịch nhắc nhở hàng ngày bằng AlarmManager */
    public static void scheduleDailyReminder(Context context, int hour, int minute) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, DailyReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context, 1001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        // Nếu giờ đặt đã qua trong ngày hôm nay thì hẹn cho ngày mai
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        try {
            am.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    cal.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pi);
        } catch (Exception e) {
            try {
                am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } catch (Exception ignored) {}
        }
    }

    public static void cancelDailyReminder(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, DailyReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context, 1001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        am.cancel(pi);
    }

    /** Gửi thông báo nhắc nhở ghi chép hàng ngày */
    public static void sendDailyReminderNotification(Context context) {
        initChannels(context);
        Intent openApp = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_DAILY)
                .setSmallIcon(R.drawable.ic_wallet)
                .setContentTitle("Đã đến giờ ghi chép chi tiêu! 📝")
                .setContentText("Hãy dành 1 phút cập nhật các khoản chi tiêu hôm nay để kiểm soát tài chính tốt hơn nhé!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Hãy dành 1 phút cập nhật các khoản chi tiêu hôm nay để kiểm soát tài chính và ngân sách cá nhân của bạn tốt hơn nhé!"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi);

        try {
            NotificationManagerCompat.from(context).notify(2001, b.build());
        } catch (SecurityException ignored) {}
    }

    /** Kiểm tra và tự động cảnh báo ngân sách khi phát sinh chi tiêu mới */
    public static void checkAndNotifyBudget(Context context, long userId) {
        SharedPreferences sp = getPrefs(context);
        if (!sp.getBoolean(KEY_BUDGET_ENABLED, true)) return;

        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH) + 1;
        String monthStr = String.format("%04d-%02d", y, m);
        String from = Utils.toIso(y, m, 1), to = Utils.toIso(y, m, 31);

        DBHelper db = DBHelper.get(context);
        long totalBudget = db.getTotalMonthlyBudget(userId, monthStr);
        if (totalBudget <= 0) {
            // Thử lấy ngân sách từ Session nếu có
            totalBudget = Session.budget(context);
        }
        if (totalBudget <= 0) return;

        long expense = db.sum(userId, "expense", from, to);
        int percent = (int) ((expense * 100) / totalBudget);

        String key80 = "notified_80_" + monthStr + "_" + userId;
        String key100 = "notified_100_" + monthStr + "_" + userId;

        if (percent >= 100) {
            if (!sp.getBoolean(key100, false)) {
                sendBudgetAlertNotification(context,
                        "🚨 Ngân sách tháng này đã hết!",
                        "Bạn đã chi tiêu " + Utils.money(expense) + " / " + Utils.money(totalBudget) +
                                " (" + percent + "%). Bạn đã vượt quá ngân sách đặt ra!",
                        3001);
                sp.edit().putBoolean(key100, true).putBoolean(key80, true).apply();
            }
        } else if (percent >= 80) {
            if (!sp.getBoolean(key80, false)) {
                sendBudgetAlertNotification(context,
                        "⚠️ Cảnh báo: Sắp đạt giới hạn ngân sách!",
                        "Bạn đã sử dụng " + percent + "% ngân sách tháng này (" +
                                Utils.money(expense) + " / " + Utils.money(totalBudget) + "). Hãy chú ý chi tiêu!",
                        3002);
                sp.edit().putBoolean(key80, true).apply();
            }
        }
    }

    public static void sendBudgetAlertNotification(Context context, String title, String message, int id) {
        initChannels(context);
        Intent openApp = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                context, id, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_BUDGET)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        try {
            NotificationManagerCompat.from(context).notify(id, b.build());
        } catch (SecurityException ignored) {}
    }

    public static void sendRecurringBillNotification(Context context, String title, String message, int id) {
        initChannels(context);
        Intent openApp = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                context, id, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_RECURRING)
                .setSmallIcon(R.drawable.ic_recurring)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        try {
            NotificationManagerCompat.from(context).notify(id, b.build());
        } catch (SecurityException ignored) {}
    }
}
