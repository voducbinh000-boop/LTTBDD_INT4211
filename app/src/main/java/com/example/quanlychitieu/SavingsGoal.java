package com.example.quanlychitieu;

public class SavingsGoal {
    public long id;
    public long userId;
    public String name;
    public long targetAmount;
    public long currentAmount;
    public String startDate;
    public String endDate;
    public String status; // "active" hoặc "completed"

    public SavingsGoal() {
        this.status = "active";
    }

    public SavingsGoal(long id, long userId, String name, long targetAmount, long currentAmount,
                       String startDate, String endDate, String status) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status != null ? status : "active";
    }

    public int getPercent() {
        if (targetAmount <= 0) return 0;
        return (int) Math.min(100, (currentAmount * 100) / targetAmount);
    }

    public boolean isCompleted() {
        return currentAmount >= targetAmount || "completed".equalsIgnoreCase(status);
    }

    public long getRemaining() {
        return Math.max(0, targetAmount - currentAmount);
    }
}
