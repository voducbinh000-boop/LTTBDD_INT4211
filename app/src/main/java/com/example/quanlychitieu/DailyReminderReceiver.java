package com.example.quanlychitieu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.List;

public class DailyReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sp = NotificationHelper.getPrefs(context);
        boolean dailyEnabled = sp.getBoolean(NotificationHelper.KEY_DAILY_ENABLED, true);
        if (dailyEnabled) {
            NotificationHelper.sendDailyReminderNotification(context);
        }

        // Kiểm tra hóa đơn định kỳ sắp đến hạn
        boolean recurringEnabled = sp.getBoolean(NotificationHelper.KEY_RECURRING_ENABLED, true);
        if (recurringEnabled && Session.isLoggedIn(context)) {
            long uid = Session.userId(context);
            DBHelper db = DBHelper.get(context);
            List<RecurringTransaction> list = db.getRecurringTransactions(uid);
            String today = Utils.today();

            for (RecurringTransaction r : list) {
                if (r.isActive && r.nextDate != null && r.nextDate.equals(today)) {
                    NotificationHelper.sendRecurringBillNotification(
                            context,
                            "Hóa đơn đến hạn hôm nay: " + r.note,
                            "Khoản " + (r.isIncome() ? "thu " : "chi ") + r.note + " (" +
                                    Utils.money(r.amount) + ") đến hạn hôm nay.",
                            (int) (4000 + r.id)
                    );
                }
            }
        }
    }
}
