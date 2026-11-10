package com.example.quanlychitieu;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class ManageCategoriesActivity extends AppCompatActivity {
    private TabLayout tabType;
    private DBHelper db;
    private long userId;
    private List<Category> items = new ArrayList<>();
    private RecyclerView.Adapter<VH> adapter;

    private static final String[] ICON_KEYS = {"food", "shopping", "transport", "salary", "wallet", "other"};
    private static final String[] ICON_LABELS = {"Ăn uống (food)", "Mua sắm (shopping)", "Đi lại (transport)", "Lương (salary)", "Ví tiền (wallet)", "Khác (other)"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_categories);
        db = DBHelper.get(this);
        userId = Session.userId(this);

        tabType = findViewById(R.id.tabType);
        tabType.selectTab(tabType.getTabAt(1)); // mặc định: Chi tiêu

        RecyclerView rv = findViewById(R.id.rvCategories);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerView.Adapter<VH>() {
            @NonNull
            @Override
            public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
                return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_category_manage, p, false));
            }

            @Override
            public void onBindViewHolder(@NonNull VH h, int pos) {
                Category c = items.get(pos);
                h.tvName.setText(c.name);
                h.img.setImageResource(Utils.iconRes(c.icon));
                h.btnEdit.setOnClickListener(v -> showAddOrEditDialog(c));
                h.btnDelete.setOnClickListener(v -> confirmDelete(c));
            }

            @Override
            public int getItemCount() {
                return items.size();
            }
        };
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddOrEditDialog(null));
        tabType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                load();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        load();
    }

    private String currentType() {
        return tabType.getSelectedTabPosition() == 0 ? "income" : "expense";
    }

    private void load() {
        items = db.getCategories(userId, currentType());
        adapter.notifyDataSetChanged();
    }

    private void showAddOrEditDialog(@Nullable Category editing) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        EditText edt = new EditText(this);
        edt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edt.setHint("Tên danh mục");
        if (editing != null) edt.setText(editing.name);
        box.addView(edt);

        TextView lblIcon = new TextView(this);
        lblIcon.setText("Biểu tượng danh mục:");
        lblIcon.setPadding(0, pad / 2, 0, 4);
        box.addView(lblIcon);

        Spinner spIcon = new Spinner(this);
        ArrayAdapter<String> iconAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, ICON_LABELS);
        spIcon.setAdapter(iconAdapter);
        if (editing != null && editing.icon != null) {
            for (int i = 0; i < ICON_KEYS.length; i++) {
                if (ICON_KEYS[i].equalsIgnoreCase(editing.icon)) {
                    spIcon.setSelection(i);
                    break;
                }
            }
        }
        box.addView(spIcon);

        new MaterialAlertDialogBuilder(this)
                .setTitle(editing == null ? "Thêm danh mục mới" : "Chỉnh sửa danh mục")
                .setView(box)
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = edt.getText().toString().trim();
                    if (name.isEmpty()) {
                        Utils.toast(this, "Vui lòng nhập tên danh mục");
                        return;
                    }
                    String icon = ICON_KEYS[spIcon.getSelectedItemPosition()];
                    if (editing == null) {
                        db.addCategory(userId, name, currentType(), icon);
                        Utils.toast(this, "Đã thêm danh mục");
                    } else {
                        db.updateCategory(editing.id, userId, name, icon);
                        Utils.toast(this, "Đã cập nhật danh mục");
                    }
                    load();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDelete(Category c) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa danh mục")
                .setMessage("Bạn có chắc muốn xóa danh mục \"" + c.name + "\"?")
                .setPositiveButton("Xóa", (d, w) -> {
                    if (db.deleteCategory(c.id, userId)) {
                        Utils.toast(this, "Đã xóa danh mục");
                        load();
                    } else {
                        Utils.toast(this, "Không thể xóa: Danh mục này đang có giao dịch phát sinh!");
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tvName;
        ImageButton btnEdit, btnDelete;

        VH(View v) {
            super(v);
            img = v.findViewById(R.id.imgIcon);
            tvName = v.findViewById(R.id.tvName);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
