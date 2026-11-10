package com.example.quanlychitieu;

import android.os.Bundle;
import android.util.Patterns;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {
    private TextInputLayout tilEmail, tilNewPass, tilConfirmPass;
    private TextInputEditText edtEmail, edtNewPass, edtConfirmPass;
    private DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        db = DBHelper.get(this);

        tilEmail = findViewById(R.id.tilEmail);
        tilNewPass = findViewById(R.id.tilNewPass);
        tilConfirmPass = findViewById(R.id.tilConfirmPass);
        edtEmail = findViewById(R.id.edtEmail);
        edtNewPass = findViewById(R.id.edtNewPass);
        edtConfirmPass = findViewById(R.id.edtConfirmPass);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSendFirebase).setOnClickListener(v -> sendFirebaseReset());
        findViewById(R.id.btnResetDirect).setOnClickListener(v -> resetDirect());
    }

    private void sendFirebaseReset() {
        tilEmail.setError(null);
        String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập địa chỉ email");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Định dạng email không hợp lệ");
            return;
        }

        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle("Thành công")
                                    .setMessage("Liên kết đặt lại mật khẩu đã được gửi đến email:\n" + email +
                                            "\n\nVui lòng kiểm tra hộp thư đến (hoặc thư mục Spam) và nhấn vào liên kết để đổi mật khẩu.")
                                    .setPositiveButton("Đồng ý", (d, w) -> finish())
                                    .show();
                        } else {
                            String err = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định";
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle("Thông báo")
                                    .setMessage("Gửi email thất bại:\n" + err +
                                            "\n\nLưu ý: Nếu chưa cấu hình dự án Firebase Console, bạn có thể sử dụng tính năng 'Đặt lại mật khẩu trực tiếp' bên dưới!")
                                    .setPositiveButton("Đóng", null)
                                    .show();
                        }
                    });
        } catch (Exception e) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Chưa kết nối Firebase")
                    .setMessage("Để gửi email khôi phục mật khẩu qua máy chủ Firebase, vui lòng cấu hình tệp google-services.json theo hướng dẫn trong HUONG_DAN_CAU_HINH_FIREBASE.md.\n\nHiện tại bạn có thể đặt lại mật khẩu trực tiếp bằng ô bên dưới.")
                    .setPositiveButton("Đồng ý", null)
                    .show();
        }
    }

    private void resetDirect() {
        tilEmail.setError(null);
        tilNewPass.setError(null);
        tilConfirmPass.setError(null);

        String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
        String newPass = edtNewPass.getText() != null ? edtNewPass.getText().toString() : "";
        String confirm = edtConfirmPass.getText() != null ? edtConfirmPass.getText().toString() : "";

        boolean ok = true;
        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            ok = false;
        }
        if (newPass.length() < 6) {
            tilNewPass.setError("Mật khẩu mới phải từ 6 ký tự trở lên");
            ok = false;
        }
        if (!newPass.equals(confirm)) {
            tilConfirmPass.setError("Mật khẩu xác nhận không khớp");
            ok = false;
        }
        if (!ok) return;

        User u = db.getUserByEmail(email);
        if (u == null) {
            tilEmail.setError("Không tìm thấy tài khoản với email này trong hệ thống");
            return;
        }

        if (db.resetPassword(email, newPass)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Thành công")
                    .setMessage("Mật khẩu tài khoản " + email + " đã được cập nhật thành công. Hãy đăng nhập bằng mật khẩu mới!")
                    .setPositiveButton("Đăng nhập ngay", (d, w) -> finish())
                    .show();
        } else {
            Utils.toast(this, "Có lỗi xảy ra khi cập nhật mật khẩu");
        }
    }
}
