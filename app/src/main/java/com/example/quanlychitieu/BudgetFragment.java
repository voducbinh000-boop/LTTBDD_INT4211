package com.example.quanlychitieu;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BudgetFragment extends Fragment {
    private final Calendar cal = Calendar.getInstance();

    private TextView tvCurrentMonth, tvTotalBudgetAmount, tvTotalSpent, tvTotalRemaining;
    private ProgressBar progressTotalBudget;
    private LinearLayout boxAlert;
    private ImageView imgAlertIcon;
    private TextView tvAlertMessage;
    private TextView tvEmptyCategoryBudgets;
    private RecyclerView rvCategoryBudgets;

    private DBHelper db;
    private long userId;
    private List<Budget> categoryBudgets = new ArrayList<>();
    private CategoryBudgetAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        View v = inflater.inflate(R.layout.fragment_budget, container, false);
        db = DBHelper.get(requireContext());
        userId = Session.userId(requireContext());

        tvCurrentMonth = v.findViewById(R.id.tvCurrentMonth);
        tvTotalBudgetAmount = v.findViewById(R.id.tvTotalBudgetAmount);
        tvTotalSpent = v.findViewById(R.id.tvTotalSpent);
        tvTotalRemaining = v.findViewById(R.id.tvTotalRemaining);
        progressTotalBudget = v.findViewById(R.id.progressTotalBudget);
        boxAlert = v.findViewById(R.id.boxAlert);
        imgAlertIcon = v.findViewById(R.id.imgAlertIcon);
        tvAlertMessage = v.findViewById(R.id.tvAlertMessage);
        tvEmptyCategoryBudgets = v.findViewById(R.id.tvEmptyCategoryBudgets);
        rvCategoryBudgets = v.findViewById(R.id.rvCategoryBudgets);

        rvCategoryBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CategoryBudgetAdapter();
        rvCategoryBudgets.setAdapter(adapter);

        v.findViewById(R.id.btnPrevMonth).setOnClickListener(x -> {
            cal.add(Calendar.MONTH, -1);
            loadData();
        });
        v.findViewById(R.id.btnNextMonth).setOnClickListener(x -> {
            cal.add(Calendar.MONTH, 1);
            loadData();
        });
        v.findViewById(R.id.btnEditTotalBudget).setOnClickListener(x -> showEditTotalBudgetDialog());
        v.findViewById(R.id.btnAddCategoryBudget).setOnClickListener(x -> showAddCategoryBudgetDialog(null));

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private String getMonthString() {
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH) + 1;
        return String.format(Locale.US, "%04d-%02d", y, m);
    }

    private void loadData() {
        if (!isAdded()) return;
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH) + 1;
        tvCurrentMonth.setText(String.format(Locale.US, "Tháng %02d/%d", m, y));

        String monthStr = getMonthString();
        String from = Utils.toIso(y, m, 1);
        String to = Utils.toIso(y, m, 31);

        long totalBudget = db.getTotalMonthlyBudget(userId, monthStr);
        if (totalBudget <= 0) {
            // Mặc định lấy theo Session nếu chưa set riêng cho tháng này
            totalBudget = Session.budget(requireContext());
        }
        long spent = db.sum(userId, "expense", from, to);
        long remaining = totalBudget - spent;
        int percent = totalBudget > 0 ? (int) Math.min(999, (spent * 100) / totalBudget) : 0;

        tvTotalBudgetAmount.setText(Utils.money(totalBudget));
        tvTotalSpent.setText("Đã chi: " + Utils.money(spent));
        progressTotalBudget.setProgress(Math.min(100, percent));

        // Cập nhật cảnh báo & màu sắc
        if (spent > totalBudget) {
            long exceeded = spent - totalBudget;
            tvTotalRemaining.setText("Vượt mức: -" + Utils.money(exceeded));
            tvTotalRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense));

            boxAlert.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.expense_bg));
            imgAlertIcon.setImageResource(R.drawable.ic_warning);
            imgAlertIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.expense));
            tvAlertMessage.setText("🚨 Cảnh báo: Bạn đã chi vượt ngân sách " + Utils.money(exceeded) + " (" + percent + "%)!");
            tvAlertMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense));
        } else if (spent == totalBudget && totalBudget > 0) {
            tvTotalRemaining.setText("Còn lại: 0đ");
            tvTotalRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));

            boxAlert.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.warning_bg));
            imgAlertIcon.setImageResource(R.drawable.ic_warning);
            imgAlertIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.warning));
            tvAlertMessage.setText("⚠️ Ngân sách tháng này đã hết (100%)! Hãy hạn chế phát sinh thêm chi tiêu.");
            tvAlertMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));
        } else if (percent >= 80) {
            tvTotalRemaining.setText("Còn lại: " + Utils.money(remaining));
            tvTotalRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));

            boxAlert.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.warning_bg));
            imgAlertIcon.setImageResource(R.drawable.ic_warning);
            imgAlertIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.warning));
            tvAlertMessage.setText("⚠️ Cảnh báo: Bạn đã sử dụng " + percent + "% ngân sách! Sắp chạm giới hạn tháng.");
            tvAlertMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));
        } else {
            tvTotalRemaining.setText("Còn lại: " + Utils.money(remaining));
            tvTotalRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.income));

            boxAlert.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.income_bg));
            imgAlertIcon.setImageResource(R.drawable.ic_check_circle);
            imgAlertIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.income));
            tvAlertMessage.setText("Chi tiêu đang trong tầm kiểm soát an toàn (" + percent + "%).");
            tvAlertMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.income));
        }

        // Tải danh sách ngân sách theo danh mục
        List<Budget> allBudgets = db.getBudgetsForMonth(userId, monthStr);
        categoryBudgets.clear();
        for (Budget b : allBudgets) {
            if (b.categoryId > 0) categoryBudgets.add(b);
        }

        adapter.notifyDataSetChanged();
        tvEmptyCategoryBudgets.setVisibility(categoryBudgets.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showEditTotalBudgetDialog() {
        String monthStr = getMonthString();
        long current = db.getTotalMonthlyBudget(userId, monthStr);
        if (current <= 0) current = Session.budget(requireContext());

        EditText edt = new EditText(requireContext());
        edt.setInputType(InputType.TYPE_CLASS_NUMBER);
        edt.setHint("Nhập số tiền ngân sách tháng");
        edt.setText(current > 0 ? String.valueOf(current) : "");

        FrameLayout box = new FrameLayout(requireContext());
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(edt);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Thiết lập ngân sách tháng")
                .setMessage("Nhập số tiền ngân sách tổng cho " + tvCurrentMonth.getText().toString() + ":")
                .setView(box)
                .setPositiveButton("Lưu", (d, w) -> {
                    long amount = Utils.parseAmount(edt.getText().toString());
                    if (amount <= 0) {
                        Utils.toast(requireContext(), "Số tiền không hợp lệ");
                        return;
                    }
                    db.setBudget(userId, monthStr, 0, amount);
                    Session.setBudget(requireContext(), amount);
                    Utils.toast(requireContext(), "Đã cập nhật ngân sách tháng");
                    loadData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showAddCategoryBudgetDialog(@Nullable Budget editing) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.activity_add_transaction, null);
        // Tái sử dụng form hoặc tạo custom view dialog
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad / 2, pad, 0);

        TextView lblCat = new TextView(requireContext());
        lblCat.setText("Chọn danh mục chi tiêu:");
        lblCat.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dark));
        lblCat.setTextSize(14);
        root.addView(lblCat);

        Spinner spCat = new Spinner(requireContext());
        List<Category> expenseCategories = db.getCategories(userId, "expense");
        ArrayAdapter<Category> spAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, expenseCategories);
        spCat.setAdapter(spAdapter);
        root.addView(spCat);

        if (editing != null) {
            for (int i = 0; i < expenseCategories.size(); i++) {
                if (expenseCategories.get(i).id == editing.categoryId) {
                    spCat.setSelection(i);
                    break;
                }
            }
            spCat.setEnabled(false);
        }

        TextView lblAmount = new TextView(requireContext());
        lblAmount.setText("Giới hạn chi tiêu tối đa (VNĐ):");
        lblAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dark));
        lblAmount.setTextSize(14);
        lblAmount.setPadding(0, pad / 2, 0, 0);
        root.addView(lblAmount);

        EditText edt = new EditText(requireContext());
        edt.setInputType(InputType.TYPE_CLASS_NUMBER);
        edt.setHint("Ví dụ: 3000000");
        if (editing != null) edt.setText(String.valueOf(editing.amount));
        root.addView(edt);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(editing != null ? "Sửa hạn mức danh mục" : "Thêm hạn mức danh mục")
                .setView(root)
                .setPositiveButton("Lưu", (d, w) -> {
                    long amount = Utils.parseAmount(edt.getText().toString());
                    if (amount <= 0) {
                        Utils.toast(requireContext(), "Vui lòng nhập số tiền hợp lệ (> 0)");
                        return;
                    }
                    Category c = (Category) spCat.getSelectedItem();
                    if (c == null) {
                        Utils.toast(requireContext(), "Chưa chọn danh mục");
                        return;
                    }
                    db.setBudget(userId, getMonthString(), c.id, amount);
                    Utils.toast(requireContext(), "Đã lưu hạn mức cho " + c.name);
                    loadData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDeleteCategoryBudget(Budget b) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa hạn mức")
                .setMessage("Bạn có chắc muốn xóa hạn mức cho danh mục \"" + b.categoryName + "\" trong tháng này?")
                .setPositiveButton("Xóa", (d, w) -> {
                    db.deleteBudget(b.id, userId);
                    Utils.toast(requireContext(), "Đã xóa hạn mức");
                    loadData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Adapter danh sách ngân sách theo danh mục
    private class CategoryBudgetAdapter extends RecyclerView.Adapter<CategoryBudgetAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_budget_category, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Budget b = categoryBudgets.get(pos);
            h.tvCategoryName.setText(b.categoryName);
            h.imgCategoryIcon.setImageResource(Utils.iconRes(b.categoryIcon));

            h.tvBudgetDetail.setText("Đã chi: " + Utils.money(b.spentAmount) + " / " + Utils.money(b.amount));
            int p = b.getPercent();
            h.tvPercent.setText(p + "%");
            h.progressCategory.setProgress(Math.min(100, p));

            if (b.isExceeded()) {
                long over = b.spentAmount - b.amount;
                h.tvStatus.setText("🚨 Vượt mức: " + Utils.money(over));
                h.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense));
                h.tvPercent.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense));
            } else if (b.isWarning()) {
                h.tvStatus.setText("⚠️ Đã chi " + p + "% (Sắp chạm hạn mức!)");
                h.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));
                h.tvPercent.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));
            } else {
                h.tvStatus.setText("Còn lại: " + Utils.money(b.getRemaining()));
                h.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.income));
                h.tvPercent.setTextColor(ContextCompat.getColor(requireContext(), R.color.wine));
            }

            h.btnEdit.setOnClickListener(v -> showAddCategoryBudgetDialog(b));
            h.btnDelete.setOnClickListener(v -> confirmDeleteCategoryBudget(b));
        }

        @Override
        public int getItemCount() {
            return categoryBudgets.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView imgCategoryIcon;
            TextView tvCategoryName, tvBudgetDetail, tvPercent, tvStatus;
            ProgressBar progressCategory;
            ImageButton btnEdit, btnDelete;

            VH(View v) {
                super(v);
                imgCategoryIcon = v.findViewById(R.id.imgCategoryIcon);
                tvCategoryName = v.findViewById(R.id.tvCategoryName);
                tvBudgetDetail = v.findViewById(R.id.tvBudgetDetail);
                tvPercent = v.findViewById(R.id.tvPercent);
                tvStatus = v.findViewById(R.id.tvStatus);
                progressCategory = v.findViewById(R.id.progressCategory);
                btnEdit = v.findViewById(R.id.btnEdit);
                btnDelete = v.findViewById(R.id.btnDelete);
            }
        }
    }
}
