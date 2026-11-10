package com.example.quanlychitieu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {
    public interface OnItemClick { void onClick(Transaction t); }

    private List<Transaction> items = new ArrayList<>();
    private final OnItemClick listener;
    private final boolean showDate; // true: hiện ngày dd/MM/yyyy, false: hiện "Hôm nay"...

    public TransactionAdapter(boolean showDate, OnItemClick listener) {
        this.showDate = showDate;
        this.listener = listener;
    }

    public void setItems(List<Transaction> list) {
        items = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Transaction t = items.get(position);
        int color = ContextCompat.getColor(h.itemView.getContext(), t.isIncome() ? R.color.income : R.color.expense);
        int bg = ContextCompat.getColor(h.itemView.getContext(), t.isIncome() ? R.color.income_bg : R.color.expense_bg);

        h.imgIcon.setImageResource(Utils.iconRes(t.categoryIcon));
        h.imgIcon.setColorFilter(color);
        h.iconBg.getBackground().mutate().setTint(bg);
        h.tvTitle.setText(t.getTitle());
        String date = showDate ? Utils.display(t.date) : Utils.relative(t.date);
        h.tvSub.setText(t.categoryName + " • " + date);
        h.tvAmount.setText(Utils.signedMoney(t));
        h.tvAmount.setTextColor(color);
        h.itemView.setOnClickListener(v -> listener.onClick(t));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        View iconBg;
        ImageView imgIcon;
        TextView tvTitle, tvSub, tvAmount;
        VH(View v) {
            super(v);
            iconBg = v.findViewById(R.id.iconBg);
            imgIcon = v.findViewById(R.id.imgIcon);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvSub = v.findViewById(R.id.tvSub);
            tvAmount = v.findViewById(R.id.tvAmount);
        }
    }
}
