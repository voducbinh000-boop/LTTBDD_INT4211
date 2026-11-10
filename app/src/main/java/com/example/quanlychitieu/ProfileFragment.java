package com.example.quanlychitieu;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

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
    private TextView tvAvatar, tvName, tvEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        tvAvatar = v.findViewById(R.id.tvAvatar);
        tvName = v.findViewById(R.id.tvName);
        tvEmail = v.findViewById(R.id.tvEmail);

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
        User u = DBHelper.get(requireContext()).getUser(Session.userId(requireContext()));
        if (u != null) {
            tvName.setText(u.name);
            tvEmail.setText(u.email);
            tvAvatar.setText(u.name.isEmpty() ? "?" : u.name.substring(0, 1).toUpperCase());
        }
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
