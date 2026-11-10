package com.example.quanlychitieu;

public class Budget {
    public long id;
    public long userId;
    public String month; // Định dạng "yyyy-MM"
    public long amount;  // Số tiền ngân sách đặt ra
    public long categoryId; // 0: Ngân sách tổng, >0: Ngân sách theo danh mục
    public String categoryName;
    public String categoryIcon;
    public long spentAmount; // Số tiền thực tế đã chi

    public Budget() {}

    public Budget(long id, long userId, String month, long amount, long categoryId) {
        this.id = id;
        this.userId = userId;
        this.month = month;
        this.amount = amount;
        this.categoryId = categoryId;
    }

    public long getRemaining() {
        return amount - spentAmount;
    }

    public int getPercent() {
        if (amount <= 0) return 0;
        return (int) Math.min(999, (spentAmount * 100) / amount);
    }

    public boolean isWarning() {
        int p = getPercent();
        return p >= 80 && p < 100;
    }

    public boolean isExceeded() {
        return spentAmount > amount;
    }

    public boolean isFull() {
        return spentAmount == amount;
    }
}
