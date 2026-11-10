package com.example.quanlychitieu;

/** Một giao dịch thu nhập hoặc chi tiêu. */
public class Transaction {
    public long id;
    public String type;        // "income" hoặc "expense"
    public long categoryId;
    public String categoryName;
    public String categoryIcon;
    public long amount;
    public String note;
    public String date;        // định dạng yyyy-MM-dd

    public boolean isIncome() { return "income".equals(type); }

    public String getTitle() {
        if (note != null && !note.trim().isEmpty()) return note;
        return categoryName != null ? categoryName : "Khác";
    }
}
