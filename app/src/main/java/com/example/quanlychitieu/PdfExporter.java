package com.example.quanlychitieu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfExporter {

    public static File export(Context context, User user, String periodLabel,
                              long totalIncome, long totalExpense, List<Transaction> transactions) throws Exception {
        PdfDocument document = new PdfDocument();
        // Kích thước chuẩn khổ A4: 595 x 842 pt
        int pageWidth = 595;
        int pageHeight = 842;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        int pageNumber = 1;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // 1. Tiêu đề chính
        paint.setColor(0xFF913D40); // Wine color
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(20);
        paint.setFakeBoldText(true);
        canvas.drawText("BÁO CÁO THU CHI CÁ NHÂN", 40, 50, paint);

        // 2. Thông tin xuất báo cáo
        paint.setColor(0xFF3B1F21);
        paint.setTextSize(10);
        paint.setFakeBoldText(false);
        String exportTime = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.US).format(new Date());
        canvas.drawText("Người lập: " + (user != null ? user.name : "N/A") + " (" + (user != null ? user.email : "") + ")", 40, 75, paint);
        canvas.drawText("Kỳ thống kê: " + periodLabel + "  |  Thời gian xuất: " + exportTime, 40, 92, paint);

        // Đường kẻ phân cách
        paint.setColor(0xFFEFE3E4);
        paint.setStrokeWidth(1.5f);
        canvas.drawLine(40, 105, pageWidth - 40, 105, paint);

        // 3. Khung tóm tắt tổng quan tài chính
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFCE8EB);
        canvas.drawRoundRect(40, 115, pageWidth - 40, 175, 8, 8, paint);

        paint.setTextSize(11);
        paint.setFakeBoldText(true);
        paint.setColor(0xFF159447); // Xanh thu nhập
        canvas.drawText("Tổng thu: +" + Utils.money(totalIncome), 60, 145, paint);

        paint.setColor(0xFFE53935); // Đỏ chi tiêu
        canvas.drawText("Tổng chi: -" + Utils.money(totalExpense), 230, 145, paint);

        paint.setColor(0xFF913D40); // Đỏ rượu số dư
        long balance = totalIncome - totalExpense;
        canvas.drawText("Số dư còn: " + (balance >= 0 ? "+" : "") + Utils.money(balance), 400, 145, paint);

        // 4. Bảng chi tiết danh sách giao dịch
        int y = 205;
        paint.setColor(0xFF913D40);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(12);
        paint.setFakeBoldText(true);
        canvas.drawText("DANH SÁCH CHI TIẾT GIAO DỊCH", 40, y, paint);

        y += 18;
        // Header bảng
        paint.setColor(0xFFF7D5D8);
        canvas.drawRect(40, y, pageWidth - 40, y + 24, paint);

        textPaint.setColor(0xFF3B1F21);
        textPaint.setTextSize(10);
        textPaint.setFakeBoldText(true);
        canvas.drawText("STT", 45, y + 16, textPaint);
        canvas.drawText("Ngày", 75, y + 16, textPaint);
        canvas.drawText("Loại", 150, y + 16, textPaint);
        canvas.drawText("Danh mục", 210, y + 16, textPaint);
        canvas.drawText("Số tiền", 330, y + 16, textPaint);
        canvas.drawText("Ghi chú", 430, y + 16, textPaint);

        y += 24;
        textPaint.setFakeBoldText(false);

        int maxItemsPerPage = 25;
        int count = 0;

        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            if (count >= maxItemsPerPage) {
                document.finishPage(page);
                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
                count = 0;
            }

            // Vẽ hàng
            if (i % 2 == 1) {
                paint.setColor(0xFFFAF2F3);
                canvas.drawRect(40, y, pageWidth - 40, y + 20, paint);
            }

            textPaint.setColor(0xFF3B1F21);
            canvas.drawText(String.valueOf(i + 1), 45, y + 14, textPaint);
            canvas.drawText(Utils.display(t.date), 75, y + 14, textPaint);

            if (t.isIncome()) {
                textPaint.setColor(0xFF159447);
                canvas.drawText("Thu", 150, y + 14, textPaint);
            } else {
                textPaint.setColor(0xFFE53935);
                canvas.drawText("Chi", 150, y + 14, textPaint);
            }

            textPaint.setColor(0xFF3B1F21);
            String catName = t.categoryName != null ? t.categoryName : "Khác";
            if (catName.length() > 16) catName = catName.substring(0, 14) + "...";
            canvas.drawText(catName, 210, y + 14, textPaint);

            String amountText = (t.isIncome() ? "+" : "-") + Utils.money(t.amount);
            canvas.drawText(amountText, 330, y + 14, textPaint);

            String note = t.note != null ? t.note : "";
            if (note.length() > 22) note = note.substring(0, 20) + "...";
            canvas.drawText(note, 430, y + 14, textPaint);

            y += 20;
            count++;
        }

        // Footer
        paint.setColor(0xFF8A7476);
        paint.setTextSize(9);
        canvas.drawText("Quản Lý Chi Tiêu Cá Nhân - Xuất tự động từ ứng dụng Android", 40, pageHeight - 30, paint);

        document.finishPage(page);

        File dir = new File(context.getExternalFilesDir(null), "reports");
        if (!dir.exists()) dir.mkdirs();

        String fileName = "BaoCao_ChiTieu_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".pdf";
        File file = new File(dir, fileName);
        FileOutputStream fos = new FileOutputStream(file);
        document.writeTo(fos);
        fos.close();
        document.close();

        return file;
    }
}
