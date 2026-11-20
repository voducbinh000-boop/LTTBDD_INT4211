package com.example.quanlychitieu.ui.fragment;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.db.DBHelper;
import com.example.quanlychitieu.data.model.Category;
import com.example.quanlychitieu.data.model.Transaction;
import com.example.quanlychitieu.ui.activity.AddTransactionActivity;
import com.example.quanlychitieu.ui.adapter.TransactionAdapter;
import com.example.quanlychitieu.util.Session;
import com.example.quanlychitieu.util.TransactionActions;
import com.example.quanlychitieu.util.Utils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HistoryFragment extends Fragment {
    private EditText edtSearch;
    private ChipGroup chipFilter;
    private Chip chipCategory, chipClearCategory;
    private Chip chipDateRange, chipClearDate;
    private TextView tvEmpty;
    private TransactionAdapter adapter;

    private String filterFromDate = null;
    private String filterToDate = null;
    private Long filterCategoryId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);
        edtSearch = v.findViewById(R.id.edtSearch);
        chipFilter = v.findViewById(R.id.chipFilter);
        chipCategory = v.findViewById(R.id.chipCategory);
        chipClearCategory = v.findViewById(R.id.chipClearCategory);
        chipDateRange = v.findViewById(R.id.chipDateRange);
        chipClearDate = v.findViewById(R.id.chipClearDate);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        RecyclerView rv = v.findViewById(R.id.rvHistory);

        adapter = new TransactionAdapter(true,
                t -> TransactionActions.show(requireActivity(), t, this::loadData));
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void afterTextChanged(Editable s) { loadData(); }
        });

        chipFilter.setOnCheckedStateChangeListener((group, ids) -> {
            resetCategoryFilterIfNotMatching();
            loadData();
        });

        // Xử lý bộ lọc Danh mục
        chipCategory.setOnClickListener(x -> showCategoryFilterDialog());
        chipClearCategory.setOnClickListener(x -> {
            filterCategoryId = null;
            chipCategory.setText("Lọc theo danh mục");
            chipClearCategory.setVisibility(View.GONE);
            loadData();
        });

        // Xử lý bộ lọc Ngày
        chipDateRange.setOnClickListener(x -> pickDateRange());
        chipClearDate.setOnClickListener(x -> {
            filterFromDate = null;
            filterToDate = null;
            chipDateRange.setText("Lọc theo ngày");
            chipClearDate.setVisibility(View.GONE);
            loadData();
        });

        v.findViewById(R.id.fabAdd).setOnClickListener(x ->
                startActivity(new Intent(getActivity(), AddTransactionActivity.class)));

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private String getCurrentType() {
        int checked = chipFilter.getCheckedChipId();
        if (checked == R.id.chipIncome) return "income";
        if (checked == R.id.chipExpense) return "expense";
        return null;
    }

    private void resetCategoryFilterIfNotMatching() {
        if (filterCategoryId == null || !isAdded()) return;
        long uid = Session.userId(requireContext());
        String currentType = getCurrentType();
        if (currentType != null) {
            List<Category> matching = DBHelper.get(requireContext()).getCategories(uid, currentType);
            boolean found = false;
            for (Category c : matching) {
                if (c.id == filterCategoryId) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                filterCategoryId = null;
                chipCategory.setText("Lọc theo danh mục");
                chipClearCategory.setVisibility(View.GONE);
            }
        }
    }

    private void showCategoryFilterDialog() {
        long uid = Session.userId(requireContext());
        DBHelper db = DBHelper.get(requireContext());
        String type = getCurrentType();

        List<Category> cats;
        if ("income".equals(type)) {
            cats = db.getCategories(uid, "income");
        } else if ("expense".equals(type)) {
            cats = db.getCategories(uid, "expense");
        } else {
            cats = new ArrayList<>();
            cats.addAll(db.getCategories(uid, "expense"));
            cats.addAll(db.getCategories(uid, "income"));
        }

        if (cats.isEmpty()) {
            Utils.toast(requireContext(), "Chưa có danh mục nào để lọc");
            return;
        }

        String[] names = new String[cats.size() + 1];
        names[0] = "Tất cả danh mục";
        int checkedItem = 0;
        for (int i = 0; i < cats.size(); i++) {
            names[i + 1] = cats.get(i).name + (type == null ? (" (" + (cats.get(i).type.equals("income") ? "Thu" : "Chi") + ")") : "");
            if (filterCategoryId != null && filterCategoryId == cats.get(i).id) {
                checkedItem = i + 1;
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chọn danh mục lọc")
                .setSingleChoiceItems(names, checkedItem, (dialog, which) -> {
                    if (which == 0) {
                        filterCategoryId = null;
                        chipCategory.setText("Lọc theo danh mục");
                        chipClearCategory.setVisibility(View.GONE);
                    } else {
                        Category selected = cats.get(which - 1);
                        filterCategoryId = selected.id;
                        chipCategory.setText(selected.name);
                        chipClearCategory.setVisibility(View.VISIBLE);
                    }
                    dialog.dismiss();
                    loadData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void pickDateRange() {
        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH), d = c.get(Calendar.DAY_OF_MONTH);

        // Chọn ngày bắt đầu
        DatePickerDialog dlgFrom = new DatePickerDialog(requireContext(), (v1, y1, m1, d1) -> {
            String from = Utils.toIso(y1, m1 + 1, d1);
            // Tiếp tục chọn ngày kết thúc
            DatePickerDialog dlgTo = new DatePickerDialog(requireContext(), (v2, y2, m2, d2) -> {
                String to = Utils.toIso(y2, m2 + 1, d2);
                if (from.compareTo(to) > 0) {
                    filterFromDate = to;
                    filterToDate = from;
                } else {
                    filterFromDate = from;
                    filterToDate = to;
                }
                chipDateRange.setText(Utils.display(filterFromDate) + " - " + Utils.display(filterToDate));
                chipClearDate.setVisibility(View.VISIBLE);
                loadData();
            }, y1, m1, d1);
            dlgTo.setTitle("Chọn ngày kết thúc");
            dlgTo.show();
        }, y, m, d);
        dlgFrom.setTitle("Chọn ngày bắt đầu");
        dlgFrom.show();
    }

    private void loadData() {
        if (!isAdded()) return;
        String type = getCurrentType();
        long uid = Session.userId(requireContext());
        String kw = edtSearch.getText() != null ? edtSearch.getText().toString().trim() : null;

        List<Transaction> list = DBHelper.get(requireContext())
                .getTransactionsFiltered(uid, type, filterFromDate, filterToDate, filterCategoryId, kw);

        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
