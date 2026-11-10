package com.example.quanlychitieu;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private TextView tvGreeting, tvMonthLabel, tvBalance, tvIncome, tvExpense, tvBudgetText, tvEmpty;
    private ProgressBar progress;
    private RecyclerView rv;
    private TransactionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        tvGreeting = v.findViewById(R.id.tvGreeting);
        tvMonthLabel = v.findViewById(R.id.tvMonthLabel);
        tvBalance = v.findViewById(R.id.tvBalance);
        tvIncome = v.findViewById(R.id.tvIncome);
        tvExpense = v.findViewById(R.id.tvExpense);
        tvBudgetText = v.findViewById(R.id.tvBudgetText);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        progress = v.findViewById(R.id.progressBudget);
        rv = v.findViewById(R.id.rvRecent);

        adapter = new TransactionAdapter(false,
                t -> TransactionActions.show(requireActivity(), t, this::loadData));
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        v.findViewById(R.id.btnAddIncome).setOnClickListener(x -> openAdd("income"));
        v.findViewById(R.id.btnAddExpense).setOnClickListener(x -> openAdd("expense"));
        v.findViewById(R.id.btnStats).setOnClickListener(x -> ((MainActivity) requireActivity()).selectTab(R.id.nav_stats));
        v.findViewById(R.id.btnSettings).setOnClickListener(x -> ((MainActivity) requireActivity()).selectTab(R.id.nav_profile));
        v.findViewById(R.id.tvSeeAll).setOnClickListener(x -> ((MainActivity) requireActivity()).selectTab(R.id.nav_history));

        // Bấm vào hộp ngân sách chuyển thẳng sang tab Ngân sách
        v.findViewById(R.id.budgetBox).setOnClickListener(x -> ((MainActivity) requireActivity()).selectTab(R.id.nav_budget));
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void openAdd(String type) {
        Intent i = new Intent(getActivity(), AddTransactionActivity.class);
        i.putExtra("type", type);
        startActivity(i);
    }

    private void loadData() {
        if (!isAdded()) return;
        long uid = Session.userId(requireContext());
        DBHelper db = DBHelper.get(requireContext());
        User u = db.getUser(uid);
        tvGreeting.setText("Xin chào,\n" + (u != null ? u.name : "") + " 👋");

        // Quét giao dịch định kỳ đến hạn
        db.processDueRecurringTransactions(uid);

        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH) + 1;
        String monthStr = String.format(Locale.US, "%04d-%02d", y, m);
        String from = Utils.toIso(y, m, 1), to = Utils.toIso(y, m, 31);
        long income = db.sum(uid, "income", from, to);
        long expense = db.sum(uid, "expense", from, to);

        tvMonthLabel.setText("Tổng quan tháng " + m + "/" + y);
        tvBalance.setText(Utils.money(income - expense));
        tvIncome.setText(Utils.money(income));
        tvExpense.setText(Utils.money(expense));

        long budget = db.getTotalMonthlyBudget(uid, monthStr);
        if (budget <= 0) budget = Session.budget(requireContext());

        int percent = budget > 0 ? (int) Math.min(999, (expense * 100) / budget) : 0;
        progress.setProgress(Math.min(100, percent));

        if (expense > budget) {
            tvBudgetText.setText("🚨 Ngân sách: đã chi " + Utils.money(expense) + " / " + Utils.money(budget)
                    + " (" + percent + "% - VƯỢT MỨC!)");
            tvBudgetText.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense));
        } else if (percent >= 80) {
            tvBudgetText.setText("⚠️ Ngân sách: đã chi " + Utils.money(expense) + " / " + Utils.money(budget)
                    + " (" + percent + "% - SẮP ĐẠT GIỚI HẠN)");
            tvBudgetText.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));
        } else {
            tvBudgetText.setText("Ngân sách: đã chi " + Utils.money(expense) + " / " + Utils.money(budget)
                    + " (" + percent + "%)");
            tvBudgetText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dark));
        }

        NotificationHelper.checkAndNotifyBudget(requireContext(), uid);

        List<Transaction> recent = db.getTransactions(uid, null, null, 5);
        adapter.setItems(recent);
        tvEmpty.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
