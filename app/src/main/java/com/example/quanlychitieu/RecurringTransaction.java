package com.example.quanlychitieu;

import java.util.Calendar;

public class RecurringTransaction {
    public long id;
    public long userId;
    public String type; // "expense" hoặc "income"
    public long categoryId;
    public long amount;
    public String note;
    public String startDate;
    public String frequency; // "daily", "weekly", "monthly", "yearly"
    public String nextDate;  // Ngày đến kỳ tiếp theo "yyyy-MM-dd"
    public boolean isActive = true;

    public String categoryName;
    public String categoryIcon;

    public RecurringTransaction() {}

    public boolean isIncome() {
        return "income".equalsIgnoreCase(type);
    }

    public String getFrequencyLabel() {
        if ("daily".equalsIgnoreCase(frequency)) return "Hằng ngày";
        if ("weekly".equalsIgnoreCase(frequency)) return "Hằng tuần";
        if ("yearly".equalsIgnoreCase(frequency)) return "Hằng năm";
        return "Hằng tháng";
    }

    public static String computeNextDate(String dateStr, String freq) {
        try {
            String[] parts = dateStr.split("-");
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]) - 1;
            int d = Integer.parseInt(parts[2]);

            Calendar cal = Calendar.getInstance();
            cal.set(y, m, d);

            if ("daily".equalsIgnoreCase(freq)) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
            } else if ("weekly".equalsIgnoreCase(freq)) {
                cal.add(Calendar.WEEK_OF_YEAR, 1);
            } else if ("yearly".equalsIgnoreCase(freq)) {
                cal.add(Calendar.YEAR, 1);
            } else { // default: monthly
                cal.add(Calendar.MONTH, 1);
            }
            return Utils.toIso(cal);
        } catch (Exception e) {
            return dateStr;
        }
    }
}
