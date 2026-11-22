package com.example.quanlychitieu.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.db.DBHelper;
import com.example.quanlychitieu.data.model.User;
import com.example.quanlychitieu.ui.activity.ManageCategoriesActivity;
import com.example.quanlychitieu.ui.activity.NotificationSettingsActivity;
import com.example.quanlychitieu.ui.activity.RecurringTransactionsActivity;
import com.example.quanlychitieu.ui.activity.SavingsGoalActivity;
import com.example.quanlychitieu.ui.auth.LoginActivity;
import com.example.quanlychitieu.util.Session;
import com.example.quanlychitieu.util.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ProfileFragment extends Fragment {
    private ImageView imgAvatar;
    private TextView tvAvatar, tvName, tvEmail, tvThemeStatus;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && getContext() != null) {
                        long uid = Session.userId(requireContext());
                        String savedPath = Utils.saveAvatarFromUri(requireContext(), uri, uid);
                        if (savedPath != null) {
                            DBHelper.get(requireContext()).updateUserAvatar(uid, savedPath);
                            Utils.toast(requireContext(), "Đã cập nhật ảnh đại diện");
                            loadUserData();
                        } else {
                            Utils.toast(requireContext(), "Không thể lưu ảnh đại diện");
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        imgAvatar = v.findViewById(R.id.imgAvatar);
        tvAvatar = v.findViewById(R.id.tvAvatar);
        tvName = v.findViewById(R.id.tvName);
        tvEmail = v.findViewById(R.id.tvEmail);
        tvThemeStatus = v.findViewById(R.id.tvThemeStatus);

        // Chạm vào ảnh đại diện để đổi hoặc xóa ảnh
        v.findViewById(R.id.layoutAvatarContainer).setOnClickListener(x -> showAvatarDialog());

        // Mở màn hình Mục tiêu tiết kiệm
        v.findViewById(R.id.rowSavings).setOnClickListener(x ->
                startActivity(new Intent(getActivity(), SavingsGoalActivity.class)));

        // Mở màn hình Giao dịch định kỳ
        v.findViewById(R.id.rowRecurring).setOnClickListener(x ->
                startActivity(new Intent(getActivity(), RecurringTransactionsActivity.class)));

        // Mở màn hình Cài đặt thông báo & nhắc nhở
        v.findViewById(R.id.rowNotifications).setOnClickListener(x ->
                startActivity(new Intent(getActivity(), NotificationSettingsActivity.class)));

        // Quản lý danh mục
        v.findViewById(R.id.rowCategories).setOnClickListener(x ->
                startActivity(new Intent(getActivity(), ManageCategoriesActivity.class)));

        // Đổi mật khẩu
        v.findViewById(R.id.rowPassword).setOnClickListener(x -> showChangePassword());

        // Chế độ giao diện (Sáng / Tối)
        v.findViewById(R.id.rowTheme).setOnClickListener(x -> showThemeDialog());
        updateThemeStatus();

        // Sao lưu & khôi phục
        v.findViewById(R.id.rowBackup).setOnClickListener(x -> showBackupDialog());

        // Thông tin ứng dụng
        v.findViewById(R.id.rowInfo).setOnClickListener(x -> showInfo());

        // Đăng xuất
        v.findViewById(R.id.btnLogout).setOnClickListener(x -> confirmLogout());

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    private void loadUserData() {
        if (!isAdded()) return;
        User u = DBHelper.get(requireContext()).getUser(Session.userId(requireContext()));
        if (u != null) {
            tvName.setText(u.name);
            tvEmail.setText(u.email);
            Utils.loadAvatar(requireContext(), u.avatar, imgAvatar, tvAvatar, u.name);
        }
        updateThemeStatus();
    }

    private void showAvatarDialog() {
        long uid = Session.userId(requireContext());
        User u = DBHelper.get(requireContext()).getUser(uid);
        boolean hasAvatar = u != null && u.avatar != null && !u.avatar.isEmpty() && new File(u.avatar).exists();

        String[] options = hasAvatar
                ? new String[]{"Chọn ảnh mới từ thư viện", "Xóa ảnh đại diện"}
                : new String[]{"Chọn ảnh từ thư viện"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ảnh đại diện")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickImageLauncher.launch("image/*");
                    } else if (which == 1 && hasAvatar) {
                        Utils.deleteAvatar(requireContext(), uid);
                        DBHelper.get(requireContext()).updateUserAvatar(uid, null);
                        Utils.toast(requireContext(), "Đã xóa ảnh đại diện");
                        loadUserData();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateThemeStatus() {
        if (tvThemeStatus == null || !isAdded()) return;
        int mode = Session.getThemeMode(requireContext());
        if (mode == Session.THEME_LIGHT) {
            tvThemeStatus.setText("Chế độ sáng");
        } else if (mode == Session.THEME_DARK) {
            tvThemeStatus.setText("Chế độ tối (Dark Mode)");
        } else {
            tvThemeStatus.setText("Theo hệ thống");
        }
    }

    private void showThemeDialog() {
        String[] options = {"Theo hệ thống", "Chế độ sáng", "Chế độ tối"};
        int current = Session.getThemeMode(requireContext());
        int checkedItem = 0;
        if (current == Session.THEME_LIGHT) checkedItem = 1;
        else if (current == Session.THEME_DARK) checkedItem = 2;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chọn chế độ giao diện")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    int newMode;
                    if (which == 1) newMode = Session.THEME_LIGHT;
                    else if (which == 2) newMode = Session.THEME_DARK;
                    else newMode = Session.THEME_SYSTEM;

                    Session.setThemeMode(requireContext(), newMode);
                    updateThemeStatus();
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showChangePassword() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        TextInputLayout tilOld = view.findViewById(R.id.tilOld);
        TextInputLayout tilNew = view.findViewById(R.id.tilNew);
        TextInputLayout tilConfirm = view.findViewById(R.id.tilConfirm);
        TextInputEditText edtOld = view.findViewById(R.id.edtOld);
        TextInputEditText edtNew = view.findViewById(R.id.edtNew);
        TextInputEditText edtConfirm = view.findViewById(R.id.edtConfirm);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đổi mật khẩu")
                .setView(view)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            tilOld.setError(null); tilNew.setError(null); tilConfirm.setError(null);
            String o = edtOld.getText().toString();
            String n = edtNew.getText().toString();
            String c = edtConfirm.getText().toString();
            boolean ok = true;
            if (o.isEmpty()) { tilOld.setError("Vui lòng nhập mật khẩu hiện tại"); ok = false; }
            if (n.length() < 6) { tilNew.setError("Mật khẩu mới phải có ít nhất 6 ký tự"); ok = false; }
            if (!n.equals(c)) { tilConfirm.setError("Mật khẩu xác nhận không khớp"); ok = false; }
            if (!ok) return;
            if (DBHelper.get(requireContext()).changePassword(Session.userId(requireContext()), o, n)) {
                Utils.toast(requireContext(), "Đổi mật khẩu thành công");
                dialog.dismiss();
            } else {
                tilOld.setError("Mật khẩu hiện tại không đúng");
            }
        }));
        dialog.show();
    }

    private File backupFile() {
        File dir = requireContext().getExternalFilesDir(null);
        return new File(dir, "QuanLyChiTieu_backup.db");
    }

    private void showBackupDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sao lưu dữ liệu")
                .setMessage("Tệp sao lưu được lưu trong bộ nhớ riêng của ứng dụng:\n" + backupFile().getPath())
                .setPositiveButton("Sao lưu", (d, w) -> copyDb(true))
                .setNegativeButton("Khôi phục", (d, w) -> copyDb(false))
                .setNeutralButton("Đóng", null)
                .show();
    }

    private void copyDb(boolean backup) {
        try {
            File dbFile = requireContext().getDatabasePath(DBHelper.DB_NAME);
            File bak = backupFile();
            File src = backup ? dbFile : bak;
            File dst = backup ? bak : dbFile;
            if (!src.exists()) {
                Utils.toast(requireContext(), backup ? "Chưa có dữ liệu để sao lưu" : "Chưa có tệp sao lưu");
                return;
            }
            DBHelper.get(requireContext()).close();
            try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            Utils.toast(requireContext(), backup ? "Sao lưu thành công" : "Khôi phục thành công");
            if (!backup && DBHelper.get(requireContext()).getUser(Session.userId(requireContext())) == null) {
                logout();
            }
        } catch (Exception e) {
            Utils.toast(requireContext(), "Lỗi: " + e.getMessage());
        }
    }

    private void showInfo() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Quản Lý Chi Tiêu Cá Nhân")
                .setMessage("Phiên bản 2.0 (Nâng cấp toàn diện)\n\n" +
                        "• Xác thực Firebase Auth & Google Sign-In\n" +
                        "• Quản lý Ngân sách tháng & Hạn mức từng danh mục\n" +
                        "• Mục tiêu tiết kiệm & Tích lũy tài chính\n" +
                        "• Giao dịch định kỳ & Tự động ghi chép\n" +
                        "• Hệ thống thông báo nhắc nhở & Cảnh báo hạn mức\n" +
                        "• Báo cáo tài chính & Xuất PDF, Excel (CSV)\n\n" +
                        "Đồ án Lập trình Android – Material Design 3, Java & SQLite.")
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất tài khoản không?")
                .setPositiveButton("Đăng xuất", (d, w) -> logout())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void logout() {
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception ignored) {}
        Session.logout(requireContext());
        Intent i = new Intent(getActivity(), LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}
