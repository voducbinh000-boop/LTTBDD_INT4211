package com.example.quanlychitieu;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExcelExporter {

    public static File export(Context context, User user, String periodLabel,
                              long totalIncome, long totalExpense, List<Transaction> transactions) throws Exception {
        File dir = new File(context.getExternalFilesDir(null), "reports");
        if (!dir.exists()) dir.mkdirs();

        String fileName = "BaoCao_ChiTieu_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".csv";
        File file = new File(dir, fileName);

        FileOutputStream fos = new FileOutputStream(file);
        // Ghi mã UTF-8 BOM (\uFEFF) để Excel hiển thị đúng dấu tiếng Việt
        fos.write(0xEF);
        fos.write(0xBB);
        fos.write(0xBF);

        OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);

        writer.write("BÁO CÁO THU CHI CÁ NHÂN\n");
        writer.write("Người lập:," + escape(user != null ? user.name : "N/A") + "\n");
        writer.write("Email:," + escape(user != null ? user.email : "N/A") + "\n");
        writer.write("Thời gian xuất:," + new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.US).format(new Date()) + "\n");
        writer.write("Kỳ thống kê:," + escape(periodLabel) + "\n\n");

        writer.write("TỔNG QUAN TÀI CHÍNH\n");
        writer.write("Tổng thu nhập (VNĐ):," + totalIncome + "\n");
        writer.write("Tổng chi tiêu (VNĐ):," + totalExpense + "\n");
        writer.write("Số dư còn lại (VNĐ):," + (totalIncome - totalExpense) + "\n\n");

        writer.write("DANH SÁCH CHI TIẾT GIAO DỊCH\n");
        writer.write("STT,Ngày,Loại giao dịch,Danh mục,Số tiền (VNĐ),Ghi chú\n");

        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            String typeStr = t.isIncome() ? "Thu nhập" : "Chi tiêu";
            String catStr = t.categoryName != null ? t.categoryName : "Khác";
            String noteStr = t.note != null ? t.note : "";

            writer.write(String.format(Locale.US, "%d,%s,%s,%s,%d,%s\n",
                    (i + 1),
                    escape(Utils.display(t.date)),
                    escape(typeStr),
                    escape(catStr),
                    t.amount,
                    escape(noteStr)
            ));
        }

        writer.flush();
        writer.close();
        fos.close();

        return file;
    }

    private static String escape(String data) {
        if (data == null) return "";
        if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
            return "\"" + data.replace("\"", "\"\"") + "\"";
        }
        return data;
    }
}
