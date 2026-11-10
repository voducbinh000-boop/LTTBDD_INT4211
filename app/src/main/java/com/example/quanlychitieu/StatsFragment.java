package com.example.quanlychitieu;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatsFragment extends Fragment {
    private static final int DAY = 0, MONTH = 1, YEAR = 2;

    private int mode = MONTH;
    private final Calendar cal = Calendar.getInstance();

    private TextView tvPeriod, tvTotalIncome, tvTotalExpense, tvNetBalance;
    private View boxComparison, boxTopExpense;
    private TextView tvComparison, tvTopExpense, tvBarTitle;
    private PieChartView pie;
    private BarChartView bar;
    private LinearLayout listCategories;

    private long curIncome = 0;
    private long curExpense = 0;
    private String currentPeriodLabel = "";
    private String currentFrom = "";
    private String currentTo = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        View v = inflater.inflate(R.layout.fragment_stats, container, false);
        tvPeriod = v.findViewById(R.id.tvPeriod);
        tvTotalIncome = v.findViewById(R.id.tvTotalIncome);
        tvTotalExpense = v.findViewById(R.id.tvTotalExpense);
        tvNetBalance = v.findViewById(R.id.tvNetBalance);
        boxComparison = v.findViewById(R.id.boxComparison);
        tvComparison = v.findViewById(R.id.tvComparison);
        boxTopExpense = v.findViewById(R.id.boxTopExpense);
        tvTopExpense = v.findViewById(R.id.tvTopExpense);
        tvBarTitle = v.findViewById(R.id.tvBarTitle);
        pie = v.findViewById(R.id.pie);
        bar = v.findViewById(R.id.bar);
        listCategories = v.findViewById(R.id.listCategories);

        MaterialButtonToggleGroup toggle = v.findViewById(R.id.toggleMode);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnDay) mode = DAY;
            else if (checkedId == R.id.btnMonth) mode = MONTH;
            else mode = YEAR;
            loadData();
        });
        v.findViewById(R.id.btnPrev).setOnClickListener(x -> shift(-1));
        v.findViewById(R.id.btnNext).setOnClickListener(x -> shift(1));

        v.findViewById(R.id.btnExportPdf).setOnClickListener(x -> exportPdf());
        v.findViewById(R.id.btnExportExcel).setOnClickListener(x -> exportExcel());

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void shift(int step) {
        if (mode == DAY) cal.add(Calendar.DAY_OF_MONTH, step);
        else if (mode == MONTH) cal.add(Calendar.MONTH, step);
        else cal.add(Calendar.YEAR, step);
        loadData();
    }

    private void loadData() {
        if (!isAdded()) return;
        int y = cal.get(Calendar.YEAR), m = cal.get(Calendar.MONTH) + 1, d = cal.get(Calendar.DAY_OF_MONTH);
        if (mode == DAY) {
            currentFrom = currentTo = Utils.toIso(y, m, d);
            currentPeriodLabel = Utils.display(currentFrom);
        } else if (mode == MONTH) {
            currentFrom = Utils.toIso(y, m, 1);
            currentTo = Utils.toIso(y, m, 31);
            currentPeriodLabel = String.format(Locale.US, "Tháng %02d/%d", m, y);
        } else {
            currentFrom = Utils.toIso(y, 1, 1);
            currentTo = Utils.toIso(y, 12, 31);
            currentPeriodLabel = "Năm " + y;
        }
        tvPeriod.setText(currentPeriodLabel);

        long uid = Session.userId(requireContext());
        DBHelper db = DBHelper.get(requireContext());
        curIncome = db.sum(uid, "income", currentFrom, currentTo);
        curExpense = db.sum(uid, "expense", currentFrom, currentTo);
        long net = curIncome - curExpense;

        tvTotalIncome.setText(Utils.money(curIncome));
        tvTotalExpense.setText(Utils.money(curExpense));
        tvNetBalance.setText((net >= 0 ? "+" : "") + Utils.money(net));
        tvNetBalance.setTextColor(ContextCompat.getColor(requireContext(),
                net >= 0 ? R.color.income : R.color.expense));

        // 1. So sánh với tháng trước (nếu đang ở chế độ xem Tháng)
        if (mode == MONTH) {
            boxComparison.setVisibility(View.VISIBLE);
            long[] comp = db.getMonthlyComparison(uid, y, m);
            long curExp = comp[0], prevExp = comp[1];
            long diff = curExp - prevExp;

            if (prevExp > 0) {
                int percentChange = (int) Math.abs((diff * 100) / prevExp);
                if (diff > 0) {
                    tvComparison.setText("⚠️ So với tháng trước: Chi tiêu tăng " + percentChange + "% (+" + Utils.money(diff) + ")");
                    tvComparison.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense));
                } else if (diff < 0) {
                    tvComparison.setText("👏 So với tháng trước: Tiết kiệm hơn " + percentChange + "% (-" + Utils.money(Math.abs(diff)) + ")");
                    tvComparison.setTextColor(ContextCompat.getColor(requireContext(), R.color.income));
                } else {
                    tvComparison.setText("Chi tiêu ngang bằng tháng trước (" + Utils.money(curExp) + ")");
                    tvComparison.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dark));
                }
            } else {
                tvComparison.setText("Chi tiêu tháng này: " + Utils.money(curExp) + " (Chưa có dữ liệu tháng trước)");
                tvComparison.setTextColor(ContextCompat.getColor(requireContext(), R.color.wine));
            }
        } else {
            boxComparison.setVisibility(View.GONE);
        }

        // 2. Biểu đồ tròn + danh sách danh mục
        List<CategorySum> sums = db.expenseByCategory(uid, currentFrom, currentTo);
        long[] values = new long[sums.size()];
        for (int i = 0; i < sums.size(); i++) values[i] = sums.get(i).total;
        pie.setData(values, Utils.money(curExpense), "Tổng chi");

        // 3. Danh mục chi tiêu nhiều nhất
        if (!sums.isEmpty() && curExpense > 0) {
            boxTopExpense.setVisibility(View.VISIBLE);
            CategorySum top = sums.get(0);
            int topPct = (int) ((top.total * 100) / curExpense);
            tvTopExpense.setText("Chi nhiều nhất: " + top.name + " (" + Utils.money(top.total) + " - chiếm " + topPct + "% tổng chi)");
        } else {
            boxTopExpense.setVisibility(View.GONE);
        }

        // Danh sách danh mục
        listCategories.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(getContext());
        for (int i = 0; i < sums.size(); i++) {
            CategorySum s = sums.get(i);
            View row = inf.inflate(R.layout.item_category_stat, listCategories, false);

            View dot = row.findViewById(R.id.dot);
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(PieChartView.COLORS[i % PieChartView.COLORS.length]);
            dot.setBackground(gd);

            ((TextView) row.findViewById(R.id.tvName)).setText(s.name);
            ((TextView) row.findViewById(R.id.tvAmount)).setText(Utils.money(s.total));

            int pct = curExpense > 0 ? (int) (s.total * 100 / curExpense) : 0;
            ((TextView) row.findViewById(R.id.tvPercent)).setText(pct + "%");

            listCategories.addView(row);
        }

        // 4. Biểu đồ cột 12 tháng
        tvBarTitle.setText("Xu hướng thu chi năm " + y);
        long[][] monthly = db.monthlyTotals(uid, y);
        bar.setData(monthly[0], monthly[1]);
    }

    private void exportPdf() {
        try {
            long uid = Session.userId(requireContext());
            DBHelper db = DBHelper.get(requireContext());
            User user = db.getUser(uid);
            List<Transaction> txList = db.getTransactionsFiltered(uid, null, currentFrom, currentTo, null, null);

            File file = PdfExporter.export(requireContext(), user, currentPeriodLabel, curIncome, curExpense, txList);
            shareExportedFile(file, "application/pdf", "Chia sẻ Báo cáo PDF");
        } catch (Exception e) {
            Utils.toast(requireContext(), "Lỗi xuất PDF: " + e.getMessage());
        }
    }

    private void exportExcel() {
        try {
            long uid = Session.userId(requireContext());
            DBHelper db = DBHelper.get(requireContext());
            User user = db.getUser(uid);
            List<Transaction> txList = db.getTransactionsFiltered(uid, null, currentFrom, currentTo, null, null);

            File file = ExcelExporter.export(requireContext(), user, currentPeriodLabel, curIncome, curExpense, txList);
            shareExportedFile(file, "text/csv", "Chia sẻ Báo cáo Excel (CSV)");
        } catch (Exception e) {
            Utils.toast(requireContext(), "Lỗi xuất Excel: " + e.getMessage());
        }
    }

    private void shareExportedFile(File file, String mimeType, String chooserTitle) {
        try {
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Báo cáo thu chi - " + currentPeriodLabel);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, chooserTitle));
        } catch (Exception e) {
            Utils.toast(requireContext(), "Lỗi chia sẻ: " + e.getMessage());
        }
    }
}
