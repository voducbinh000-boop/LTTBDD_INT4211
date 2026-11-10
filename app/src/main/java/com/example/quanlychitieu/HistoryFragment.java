package com.example.quanlychitieu;

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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Calendar;
import java.util.List;

public class HistoryFragment extends Fragment {
    private EditText edtSearch;
    private ChipGroup chipFilter;
    private Chip chipDateRange, chipClearDate;
    private TextView tvEmpty;
    private TransactionAdapter adapter;

    private String filterFromDate = null;
    private String filterToDate = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);
        edtSearch = v.findViewById(R.id.edtSearch);
        chipFilter = v.findViewById(R.id.chipFilter);
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

        chipFilter.setOnCheckedStateChangeListener((group, ids) -> loadData());

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
        String type = null;
        int checked = chipFilter.getCheckedChipId();
        if (checked == R.id.chipIncome) type = "income";
        else if (checked == R.id.chipExpense) type = "expense";

        long uid = Session.userId(requireContext());
        String kw = edtSearch.getText() != null ? edtSearch.getText().toString().trim() : null;

        List<Transaction> list = DBHelper.get(requireContext())
                .getTransactionsFiltered(uid, type, filterFromDate, filterToDate, null, kw);

        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
