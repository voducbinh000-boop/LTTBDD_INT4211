package com.example.quanlychitieu;

import android.app.Activity;
import android.content.Intent;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Xem chi tiết / sửa / xóa một giao dịch (dùng chung cho Trang chủ và Lịch sử). */
public class TransactionActions {

    public static void show(Activity a, Transaction t, Runnable onChanged) {
        String msg = "Loại: " + (t.isIncome() ? "Thu nhập" : "Chi tiêu")
                + "\nDanh mục: " + t.categoryName
                + "\nSố tiền: " + Utils.money(t.amount)
                + "\nNgày: " + Utils.display(t.date)
                + "\nGhi chú: " + (t.note == null || t.note.isEmpty() ? "(không có)" : t.note);

        new MaterialAlertDialogBuilder(a)
                .setTitle("Chi tiết giao dịch")
                .setMessage(msg)
                .setPositiveButton("Chỉnh sửa", (d, w) -> {
                    Intent i = new Intent(a, AddTransactionActivity.class);
                    i.putExtra("id", t.id);
                    a.startActivity(i);
                })
                .setNegativeButton("Xóa", (d, w) -> confirmDelete(a, t, onChanged))
                .setNeutralButton("Đóng", null)
                .show();
    }

    private static void confirmDelete(Activity a, Transaction t, Runnable onChanged) {
        new MaterialAlertDialogBuilder(a)
                .setTitle("Xóa giao dịch")
                .setMessage("Bạn có chắc muốn xóa giao dịch này không?")
                .setPositiveButton("Xóa", (d, w) -> {
                    DBHelper.get(a).deleteTransaction(t.id);
                    Utils.toast(a, "Đã xóa giao dịch");
                    if (onChanged != null) onChanged.run();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
