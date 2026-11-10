package com.example.quanlychitieu;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class RecurringTransactionsActivity extends AppCompatActivity {
    private DBHelper db;
    private long userId;
    private final List<RecurringTransaction> items = new ArrayList<>();
    private RecurringAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring_transactions);
        db = DBHelper.get(this);
        userId = Session.userId(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView rv = findViewById(R.id.rvRecurring);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecurringAdapter();
        rv.setAdapter(adapter);

        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddOrEditDialog(null));

        // Tự động kiểm tra và tạo giao dịch đến kỳ
        int created = db.processDueRecurringTransactions(userId);
        if (created > 0) {
            Utils.toast(this, "Đã tự động tạo " + created + " giao dịch định kỳ đến hạn!");
        }

        loadData();
    }

    private void loadData() {
        items.clear();
        items.addAll(db.getRecurringTransactions(userId));
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddOrEditDialog(@Nullable RecurringTransaction editing) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);

        final String[] typeHolder = {editing != null ? editing.type : "expense"};

        // Toggle loại Thu/Chi
        TextView lblType = new TextView(this);
        lblType.setText("Loại giao dịch:");
        lblType.setTextSize(13);
        lblType.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        layout.addView(lblType);

        Spinner spType = new Spinner(this);
        String[] types = {"Chi tiêu", "Thu nhập"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spType.setAdapter(typeAdapter);
        if ("income".equals(typeHolder[0])) spType.setSelection(1);
        layout.addView(spType);

        EditText edtNote = new EditText(this);
        edtNote.setHint("Tên khoản chi (VD: Tiền nhà, Tiền Internet, Lương...)");
        if (editing != null) edtNote.setText(editing.note);
        layout.addView(edtNote);

        EditText edtAmount = new EditText(this);
        edtAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtAmount.setHint("Số tiền (VNĐ)");
        if (editing != null) edtAmount.setText(String.valueOf(editing.amount));
        layout.addView(edtAmount);

        // Danh mục
        TextView lblCat = new TextView(this);
        lblCat.setText("Danh mục:");
        lblCat.setTextSize(13);
        lblCat.setPadding(0, pad / 2, 0, 0);
        layout.addView(lblCat);

        Spinner spCat = new Spinner(this);
        List<Category> categories = db.getCategories(userId, "expense");
        ArrayAdapter<Category> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spCat.setAdapter(catAdapter);
        layout.addView(spCat);

        // Chu kỳ
        TextView lblFreq = new TextView(this);
        lblFreq.setText("Chu kỳ lặp lại:");
        lblFreq.setTextSize(13);
        lblFreq.setPadding(0, pad / 2, 0, 0);
        layout.addView(lblFreq);

        Spinner spFreq = new Spinner(this);
        String[] freqLabels = {"Hằng tháng", "Hằng tuần", "Hằng ngày", "Hằng năm"};
        String[] freqValues = {"monthly", "weekly", "daily", "yearly"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, freqLabels);
        spFreq.setAdapter(freqAdapter);
        if (editing != null) {
            for (int i = 0; i < freqValues.length; i++) {
                if (freqValues[i].equalsIgnoreCase(editing.frequency)) {
                    spFreq.setSelection(i);
                    break;
                }
            }
        }
        layout.addView(spFreq);

        // Ngày đến kỳ kế tiếp
        final String[] nextDateHolder = {editing != null ? editing.nextDate : Utils.today()};
        TextView tvNextDate = new TextView(this);
        tvNextDate.setPadding(0, pad / 2, 0, pad / 2);
        tvNextDate.setTextColor(ContextCompat.getColor(this, R.color.wine));
        tvNextDate.setText("Ngày đến hạn kế tiếp: " + Utils.display(nextDateHolder[0]));
        tvNextDate.setOnClickListener(v -> {
            String[] p = nextDateHolder[0].split("-");
            new DatePickerDialog(this, (view, y, m, d) -> {
                nextDateHolder[0] = Utils.toIso(y, m + 1, d);
                tvNextDate.setText("Ngày đến hạn kế tiếp: " + Utils.display(nextDateHolder[0]));
            }, Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2])).show();
        });
        layout.addView(tvNextDate);

        new MaterialAlertDialogBuilder(this)
                .setTitle(editing == null ? "Thêm giao dịch định kỳ" : "Chỉnh sửa giao dịch định kỳ")
                .setView(layout)
                .setPositiveButton("Lưu", (d, w) -> {
                    String note = edtNote.getText().toString().trim();
                    long amount = Utils.parseAmount(edtAmount.getText().toString());
                    String type = spType.getSelectedItemPosition() == 0 ? "expense" : "income";
                    String freq = freqValues[spFreq.getSelectedItemPosition()];
                    Category cat = (Category) spCat.getSelectedItem();
                    long catId = cat != null ? cat.id : 0;

                    if (note.isEmpty()) {
                        Utils.toast(this, "Vui lòng nhập tên khoản giao dịch");
                        return;
                    }
                    if (amount <= 0) {
                        Utils.toast(this, "Số tiền không hợp lệ");
                        return;
                    }

                    if (editing == null) {
                        RecurringTransaction r = new RecurringTransaction();
                        r.userId = userId;
                        r.type = type;
                        r.categoryId = catId;
                        r.amount = amount;
                        r.note = note;
                        r.startDate = Utils.today();
                        r.frequency = freq;
                        r.nextDate = nextDateHolder[0];
                        r.isActive = true;
                        db.insertRecurringTransaction(userId, r);
                        Utils.toast(this, "Đã thêm lịch giao dịch định kỳ");
                    } else {
                        editing.type = type;
                        editing.categoryId = catId;
                        editing.amount = amount;
                        editing.note = note;
                        editing.frequency = freq;
                        editing.nextDate = nextDateHolder[0];
                        db.updateRecurringTransaction(editing);
                        Utils.toast(this, "Đã cập nhật lịch định kỳ");
                    }
                    loadData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDelete(RecurringTransaction r) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa giao dịch định kỳ")
                .setMessage("Bạn có chắc muốn xóa lịch định kỳ \"" + r.note + "\"?")
                .setPositiveButton("Xóa", (d, w) -> {
                    db.deleteRecurringTransaction(r.id, userId);
                    Utils.toast(this, "Đã xóa");
                    loadData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private class RecurringAdapter extends RecyclerView.Adapter<RecurringAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recurring_transaction, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            RecurringTransaction r = items.get(pos);
            h.tvNote.setText(r.note);
            String cat = r.categoryName != null ? r.categoryName : "Chung";
            h.tvCategoryAndFreq.setText(cat + " • " + r.getFrequencyLabel());
            h.imgCategory.setImageResource(Utils.iconRes(r.categoryIcon));

            String moneyStr = (r.isIncome() ? "+" : "-") + Utils.money(r.amount);
            h.tvAmount.setText(moneyStr);
            h.tvAmount.setTextColor(ContextCompat.getColor(RecurringTransactionsActivity.this,
                    r.isIncome() ? R.color.income : R.color.expense));

            h.tvNextDate.setText("Kỳ kế tiếp: " + Utils.display(r.nextDate));

            String today = Utils.today();
            if (r.nextDate != null && r.nextDate.compareTo(today) <= 0) {
                h.tvDueStatus.setVisibility(View.VISIBLE);
                h.tvDueStatus.setText("Đến hạn hôm nay!");
                h.tvDueStatus.setTextColor(ContextCompat.getColor(RecurringTransactionsActivity.this, R.color.expense));
                h.tvDueStatus.setBackgroundColor(ContextCompat.getColor(RecurringTransactionsActivity.this, R.color.expense_bg));
            } else {
                h.tvDueStatus.setVisibility(View.VISIBLE);
                h.tvDueStatus.setText("Đang theo dõi");
                h.tvDueStatus.setTextColor(ContextCompat.getColor(RecurringTransactionsActivity.this, R.color.income));
                h.tvDueStatus.setBackgroundColor(ContextCompat.getColor(RecurringTransactionsActivity.this, R.color.income_bg));
            }

            h.btnEdit.setOnClickListener(v -> showAddOrEditDialog(r));
            h.btnDelete.setOnClickListener(v -> confirmDelete(r));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView imgCategory;
            TextView tvNote, tvCategoryAndFreq, tvAmount, tvNextDate, tvDueStatus;
            ImageButton btnEdit, btnDelete;

            VH(View v) {
                super(v);
                imgCategory = v.findViewById(R.id.imgCategory);
                tvNote = v.findViewById(R.id.tvNote);
                tvCategoryAndFreq = v.findViewById(R.id.tvCategoryAndFreq);
                tvAmount = v.findViewById(R.id.tvAmount);
                tvNextDate = v.findViewById(R.id.tvNextDate);
                tvDueStatus = v.findViewById(R.id.tvDueStatus);
                btnEdit = v.findViewById(R.id.btnEdit);
                btnDelete = v.findViewById(R.id.btnDelete);
            }
        }
    }
}
