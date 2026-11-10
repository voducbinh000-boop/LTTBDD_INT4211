package com.example.quanlychitieu;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

/** Thêm mới hoặc chỉnh sửa giao dịch (nếu có extra "id"). */
public class AddTransactionActivity extends AppCompatActivity {
    private TabLayout tabType;
    private Spinner spCategory;
    private TextInputLayout tilAmount;
    private TextInputEditText edtAmount, edtNote, edtDate;
    private DBHelper db;

    private String isoDate = Utils.today();
    private long editId = -1;
    private Transaction editing;
    private List<Category> categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);
        db = DBHelper.get(this);

        tabType = findViewById(R.id.tabType);
        spCategory = findViewById(R.id.spCategory);
        tilAmount = findViewById(R.id.tilAmount);
        edtAmount = findViewById(R.id.edtAmount);
        edtNote = findViewById(R.id.edtNote);
        edtDate = findViewById(R.id.edtDate);
        MaterialButton btnSave = findViewById(R.id.btnSave);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        editId = getIntent().getLongExtra("id", -1);
        String type = getIntent().getStringExtra("type");
        if (editId != -1) {
            editing = db.getTransaction(editId);
            if (editing == null) { finish(); return; }
            type = editing.type;
            ((TextView) findViewById(R.id.tvHeader)).setText("Sửa giao dịch");
            btnSave.setText("Cập nhật giao dịch");
            isoDate = editing.date;
            edtAmount.setText(String.valueOf(editing.amount));
            edtNote.setText(editing.note);
        }
        if (type == null) type = "expense";

        tabType.selectTab(tabType.getTabAt("income".equals(type) ? 0 : 1));
        loadCategories();
        tabType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { loadCategories(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        edtDate.setText(Utils.display(isoDate));
        edtDate.setOnClickListener(v -> pickDate());
        btnSave.setOnClickListener(v -> save());
    }

    private String currentType() { return tabType.getSelectedTabPosition() == 0 ? "income" : "expense"; }

    private void loadCategories() {
        long uid = Session.userId(this);
        categories = db.getCategories(uid, currentType());
        ArrayAdapter<Category> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spCategory.setAdapter(ad);
        // Khi sửa: chọn lại đúng danh mục cũ
        if (editing != null && editing.type.equals(currentType())) {
            for (int i = 0; i < categories.size(); i++)
                if (categories.get(i).id == editing.categoryId) spCategory.setSelection(i);
        }
    }

    private void pickDate() {
        String[] p = isoDate.split("-");
        new DatePickerDialog(this, (view, y, m, d) -> {
            isoDate = Utils.toIso(y, m + 1, d);
            edtDate.setText(Utils.display(isoDate));
        }, Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2])).show();
    }

    private void save() {
        tilAmount.setError(null);
        if (categories == null || categories.isEmpty()) {
            Utils.toast(this, "Chưa có danh mục, hãy thêm trong phần Cá nhân");
            return;
        }
        long amount = Utils.parseAmount(edtAmount.getText().toString());
        if (amount <= 0) {
            tilAmount.setError("Vui lòng nhập số tiền hợp lệ (lớn hơn 0)");
            Utils.toast(this, "Dữ liệu không hợp lệ");
            return;
        }
        Category cat = (Category) spCategory.getSelectedItem();

        Transaction t = new Transaction();
        t.type = currentType();
        t.categoryId = cat.id;
        t.amount = amount;
        t.note = edtNote.getText().toString().trim();
        t.date = isoDate;

        long uid = Session.userId(this);
        if (editId != -1) {
            t.id = editId;
            db.updateTransaction(uid, t);
            Utils.toast(this, "Đã cập nhật giao dịch");
        } else {
            db.insertTransaction(uid, t);
            Utils.toast(this, "Lưu giao dịch thành công");
        }
        if ("expense".equals(t.type)) {
            NotificationHelper.checkAndNotifyBudget(this, uid);
        }
        finish();
    }
}
