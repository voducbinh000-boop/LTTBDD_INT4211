package com.example.quanlychitieu;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SavingsGoalActivity extends AppCompatActivity {
    private DBHelper db;
    private long userId;
    private final List<SavingsGoal> items = new ArrayList<>();
    private GoalAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savings_goal);
        db = DBHelper.get(this);
        userId = Session.userId(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView rv = findViewById(R.id.rvSavings);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GoalAdapter();
        rv.setAdapter(adapter);

        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddOrEditDialog(null));
        loadData();
    }

    private void loadData() {
        items.clear();
        items.addAll(db.getSavingsGoals(userId));
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddOrEditDialog(@Nullable SavingsGoal editing) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);

        EditText edtName = new EditText(this);
        edtName.setHint("Tên mục tiêu (VD: Mua laptop, Mua xe...)");
        if (editing != null) edtName.setText(editing.name);
        layout.addView(edtName);

        EditText edtTarget = new EditText(this);
        edtTarget.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtTarget.setHint("Số tiền cần có (VNĐ)");
        if (editing != null) edtTarget.setText(String.valueOf(editing.targetAmount));
        layout.addView(edtTarget);

        EditText edtCurrent = new EditText(this);
        edtCurrent.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtCurrent.setHint("Số tiền đã có ban đầu (VNĐ)");
        if (editing != null) {
            edtCurrent.setText(String.valueOf(editing.currentAmount));
            edtCurrent.setVisibility(View.GONE); // khi sửa chỉ chỉnh bằng nút Nạp tiền
        }
        layout.addView(edtCurrent);

        TextView tvDatePick = new TextView(this);
        tvDatePick.setPadding(0, pad / 2, 0, pad / 2);
        tvDatePick.setTextColor(ContextCompat.getColor(this, R.color.wine));
        final String[] endDateHolder = {editing != null ? editing.endDate : Utils.today()};
        tvDatePick.setText("Ngày dự kiến hoàn thành: " + Utils.display(endDateHolder[0]));

        tvDatePick.setOnClickListener(v -> {
            String[] p = endDateHolder[0].split("-");
            new DatePickerDialog(this, (view, y, m, d) -> {
                endDateHolder[0] = Utils.toIso(y, m + 1, d);
                tvDatePick.setText("Ngày dự kiến hoàn thành: " + Utils.display(endDateHolder[0]));
            }, Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2])).show();
        });
        layout.addView(tvDatePick);

        new MaterialAlertDialogBuilder(this)
                .setTitle(editing == null ? "Tạo mục tiêu tiết kiệm mới" : "Chỉnh sửa mục tiêu")
                .setView(layout)
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = edtName.getText().toString().trim();
                    long target = Utils.parseAmount(edtTarget.getText().toString());
                    long current = editing == null ? Utils.parseAmount(edtCurrent.getText().toString()) : editing.currentAmount;
                    if (current < 0) current = 0;

                    if (name.isEmpty()) {
                        Utils.toast(this, "Vui lòng nhập tên mục tiêu");
                        return;
                    }
                    if (target <= 0) {
                        Utils.toast(this, "Số tiền mục tiêu không hợp lệ (> 0)");
                        return;
                    }

                    if (editing == null) {
                        SavingsGoal g = new SavingsGoal(0, userId, name, target, current,
                                Utils.today(), endDateHolder[0], current >= target ? "completed" : "active");
                        db.insertSavingsGoal(userId, g);
                        Utils.toast(this, "Đã tạo mục tiêu tiết kiệm");
                    } else {
                        editing.name = name;
                        editing.targetAmount = target;
                        editing.endDate = endDateHolder[0];
                        if (editing.currentAmount >= target) editing.status = "completed";
                        db.updateSavingsGoal(editing);
                        Utils.toast(this, "Đã cập nhật mục tiêu");
                    }
                    loadData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showAddMoneyDialog(SavingsGoal g) {
        EditText edt = new EditText(this);
        edt.setInputType(InputType.TYPE_CLASS_NUMBER);
        edt.setHint("Nhập số tiền nạp thêm (VNĐ)");

        LinearLayout box = new LinearLayout(this);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(edt);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm tiền vào mục tiêu")
                .setMessage("Mục tiêu: " + g.name + "\nĐã có: " + Utils.money(g.currentAmount) + " / " + Utils.money(g.targetAmount))
                .setView(box)
                .setPositiveButton("Nạp tiền", (d, w) -> {
                    long add = Utils.parseAmount(edt.getText().toString());
                    if (add <= 0) {
                        Utils.toast(this, "Số tiền nạp không hợp lệ");
                        return;
                    }
                    db.addSavingsAmount(g.id, add);
                    long newTotal = g.currentAmount + add;

                    if (newTotal >= g.targetAmount) {
                        // Thông báo chúc mừng đạt 100% mục tiêu
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("🎉 XUẤT SẮC HOÀN THÀNH!")
                                .setMessage("Chúc mừng bạn đã hoàn thành mục tiêu \"" + g.name + "\"!\n\nBạn đã tích lũy thành công " +
                                        Utils.money(newTotal) + " / " + Utils.money(g.targetAmount) + " (100%).\nTiếp tục phát huy nhé!")
                                .setPositiveButton("Tuyệt vời!", null)
                                .show();
                    } else {
                        Utils.toast(this, "Đã nạp thêm " + Utils.money(add) + " vào mục tiêu!");
                    }
                    loadData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDeleteGoal(SavingsGoal g) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa mục tiêu")
                .setMessage("Bạn có chắc muốn xóa mục tiêu \"" + g.name + "\"?")
                .setPositiveButton("Xóa", (d, w) -> {
                    db.deleteSavingsGoal(g.id, userId);
                    Utils.toast(this, "Đã xóa mục tiêu");
                    loadData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_savings_goal, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            SavingsGoal g = items.get(pos);
            h.tvGoalName.setText(g.name);
            h.tvGoalDate.setText("Hạn dự kiến: " + Utils.display(g.endDate));
            h.tvGoalAmount.setText("Đã có: " + Utils.money(g.currentAmount) + " / Cần: " + Utils.money(g.targetAmount));

            int p = g.getPercent();
            h.tvGoalPercent.setText(p + "%");
            h.progressGoal.setProgress(p);

            if (g.isCompleted()) {
                h.tvCompletedBadge.setVisibility(View.VISIBLE);
                h.btnAddMoney.setVisibility(View.GONE);
                h.tvGoalPercent.setTextColor(ContextCompat.getColor(SavingsGoalActivity.this, R.color.income));
            } else {
                h.tvCompletedBadge.setVisibility(View.GONE);
                h.btnAddMoney.setVisibility(View.VISIBLE);
                h.tvGoalPercent.setTextColor(ContextCompat.getColor(SavingsGoalActivity.this, R.color.wine));
            }

            h.btnAddMoney.setOnClickListener(v -> showAddMoneyDialog(g));
            h.btnEdit.setOnClickListener(v -> showAddOrEditDialog(g));
            h.btnDelete.setOnClickListener(v -> confirmDeleteGoal(g));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvGoalName, tvGoalDate, tvGoalPercent, tvGoalAmount, tvCompletedBadge;
            ProgressBar progressGoal;
            MaterialButton btnAddMoney;
            ImageButton btnEdit, btnDelete;

            VH(View v) {
                super(v);
                tvGoalName = v.findViewById(R.id.tvGoalName);
                tvGoalDate = v.findViewById(R.id.tvGoalDate);
                tvGoalPercent = v.findViewById(R.id.tvGoalPercent);
                tvGoalAmount = v.findViewById(R.id.tvGoalAmount);
                tvCompletedBadge = v.findViewById(R.id.tvCompletedBadge);
                progressGoal = v.findViewById(R.id.progressGoal);
                btnAddMoney = v.findViewById(R.id.btnAddMoney);
                btnEdit = v.findViewById(R.id.btnEdit);
                btnDelete = v.findViewById(R.id.btnDelete);
            }
        }
    }
}
