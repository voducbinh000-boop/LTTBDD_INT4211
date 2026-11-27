package com.example.quanlychitieu.ui.auth;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Patterns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.db.DBHelper;
import com.example.quanlychitieu.data.model.User;
import com.example.quanlychitieu.util.GMailSender;
import com.example.quanlychitieu.util.Utils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;
import java.util.Random;

public class ForgotPasswordActivity extends AppCompatActivity {
    // UI Layouts cho 3 bước
    private LinearLayout layoutStep1, layoutStep2, layoutStep3;
    private TextView tvStep1Indicator, tvStep2Indicator, tvStep3Indicator;

    // Components Bước 1
    private TextInputLayout tilEmail;
    private TextInputEditText edtEmail;
    private MaterialButton btnSendOtp;

    // Components Bước 2
    private TextInputLayout tilOtp;
    private TextInputEditText edtOtp;
    private TextView tvOtpSentMessage, tvCountdown, tvResendOtp;
    private MaterialButton btnVerifyOtp;

    // Components Bước 3
    private TextInputLayout tilNewPass, tilConfirmPass;
    private TextInputEditText edtNewPass, edtConfirmPass;
    private MaterialButton btnSaveNewPassword;

    private DBHelper db;
    private CountDownTimer countDownTimer;

    // Biến lưu trạng thái OTP
    private String validatedEmail = "";
    private String generatedOtp = "";
    private long otpTimestamp = 0;
    private static final long OTP_EXPIRE_TIME_MS = 5 * 60 * 1000; // 5 phút

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        db = DBHelper.get(this);

        initViews();
        setupListeners();
        showStep(1);
    }

    private void initViews() {
        // Indicators
        tvStep1Indicator = findViewById(R.id.tvStep1Indicator);
        tvStep2Indicator = findViewById(R.id.tvStep2Indicator);
        tvStep3Indicator = findViewById(R.id.tvStep3Indicator);

        // Layouts
        layoutStep1 = findViewById(R.id.layoutStep1);
        layoutStep2 = findViewById(R.id.layoutStep2);
        layoutStep3 = findViewById(R.id.layoutStep3);

        // Step 1
        tilEmail = findViewById(R.id.tilEmail);
        edtEmail = findViewById(R.id.edtEmail);
        btnSendOtp = findViewById(R.id.btnSendOtp);

        // Step 2
        tilOtp = findViewById(R.id.tilOtp);
        edtOtp = findViewById(R.id.edtOtp);
        tvOtpSentMessage = findViewById(R.id.tvOtpSentMessage);
        tvCountdown = findViewById(R.id.tvCountdown);
        tvResendOtp = findViewById(R.id.tvResendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);

        // Step 3
        tilNewPass = findViewById(R.id.tilNewPass);
        tilConfirmPass = findViewById(R.id.tilConfirmPass);
        edtNewPass = findViewById(R.id.edtNewPass);
        edtConfirmPass = findViewById(R.id.edtConfirmPass);
        btnSaveNewPassword = findViewById(R.id.btnSaveNewPassword);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvBackToStep1).setOnClickListener(v -> showStep(1));

        btnSendOtp.setOnClickListener(v -> handleSendOtp());
        tvResendOtp.setOnClickListener(v -> handleSendOtp());
        btnVerifyOtp.setOnClickListener(v -> handleVerifyOtp());
        btnSaveNewPassword.setOnClickListener(v -> handleSaveNewPassword());
    }

    private void showStep(int step) {
        layoutStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        layoutStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        layoutStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        int colorWine = getResources().getColor(R.color.wine, getTheme());
        int colorGray = getResources().getColor(R.color.text_gray, getTheme());

        tvStep1Indicator.setBackgroundTintList(ColorStateList.valueOf(step >= 1 ? colorWine : colorGray));
        tvStep2Indicator.setBackgroundTintList(ColorStateList.valueOf(step >= 2 ? colorWine : colorGray));
        tvStep3Indicator.setBackgroundTintList(ColorStateList.valueOf(step >= 3 ? colorWine : colorGray));
    }

    private void handleSendOtp() {
        tilEmail.setError(null);
        String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";

        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập địa chỉ Gmail");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Định dạng email không hợp lệ");
            return;
        }

        // Kiểm tra xem Email có tồn tại trong cơ sở dữ liệu không
        User user = db.getUserByEmail(email);
        if (user == null) {
            tilEmail.setError("Không tìm thấy tài khoản với email này trong hệ thống");
            return;
        }

        validatedEmail = email;
        generatedOtp = String.format(Locale.US, "%06d", new Random().nextInt(1000000));
        otpTimestamp = System.currentTimeMillis();

        btnSendOtp.setEnabled(false);
        btnSendOtp.setText("Đang gửi mã OTP...");

        GMailSender.sendOtpEmail(email, generatedOtp, new GMailSender.Callback() {
            @Override
            public void onSuccess() {
                btnSendOtp.setEnabled(true);
                btnSendOtp.setText("Gửi mã OTP qua Gmail");
                Utils.toast(ForgotPasswordActivity.this, "Đã gửi mã OTP đến " + email);

                tvOtpSentMessage.setText("Mã OTP 6 chữ số đã được gửi đến Gmail:\n" + email);
                showStep(2);
                startResendTimer();
            }

            @Override
            public void onFailure(String error) {
                btnSendOtp.setEnabled(true);
                btnSendOtp.setText("Gửi mã OTP qua Gmail");

                // Nếu chưa cấu hình App Password hoặc bị lỗi gửi mail, khởi chạy chế độ Giả lập / Demo để test
                if ("NO_CONFIG".equals(error) || error.contains("AuthenticationFailed")) {
                    showDemoOtpDialog(generatedOtp, email);
                } else {
                    new MaterialAlertDialogBuilder(ForgotPasswordActivity.this)
                            .setTitle("Không thể gửi email")
                            .setMessage("Gửi email qua Gmail thất bại: " + error + "\n\nBạn có muốn xem mã OTP để tiếp tục chạy thử không?")
                            .setPositiveButton("Xem mã OTP (Demo)", (d, w) -> showDemoOtpDialog(generatedOtp, email))
                            .setNegativeButton("Thử lại", null)
                            .show();
                }
            }
        });
    }

    private void showDemoOtpDialog(String otp, String email) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Mã OTP Xác Thực (Chế độ Thử nghiệm)")
                .setMessage("Mã OTP khôi phục mật khẩu của bạn cho email " + email + " là:\n\n" + otp + "\n\n(Lưu ý: Để gửi Gmail thực tế, vui lòng điền App Password vào GMailSender.java)")
                .setPositiveButton("Nhập mã OTP này", (d, w) -> {
                    if (edtOtp != null) edtOtp.setText(otp);
                    tvOtpSentMessage.setText("Mã OTP thử nghiệm đã tạo cho Gmail:\n" + email);
                    showStep(2);
                    startResendTimer();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void startResendTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        tvResendOtp.setVisibility(View.GONE);
        tvCountdown.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvCountdown.setText(String.format(Locale.US, "Gửi lại mã sau (%ds)", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                tvCountdown.setVisibility(View.GONE);
                tvResendOtp.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void handleVerifyOtp() {
        tilOtp.setError(null);
        String inputOtp = edtOtp.getText() != null ? edtOtp.getText().toString().trim() : "";

        if (inputOtp.isEmpty()) {
            tilOtp.setError("Vui lòng nhập mã OTP 6 chữ số");
            return;
        }
        if (inputOtp.length() < 6) {
            tilOtp.setError("Mã OTP phải đủ 6 chữ số");
            return;
        }

        // Kiểm tra thời gian hết hạn OTP (5 phút)
        if (System.currentTimeMillis() - otpTimestamp > OTP_EXPIRE_TIME_MS) {
            tilOtp.setError("Mã OTP đã hết hạn (sau 5 phút). Vui lòng gửi lại mã mới");
            return;
        }

        // Kiểm tra khớp mã OTP
        if (!inputOtp.equals(generatedOtp)) {
            tilOtp.setError("Mã OTP không chính xác. Vui lòng kiểm tra lại");
            return;
        }

        Utils.toast(this, "Xác thực OTP thành công!");
        showStep(3);
    }

    private void handleSaveNewPassword() {
        tilNewPass.setError(null);
        tilConfirmPass.setError(null);

        String newPass = edtNewPass.getText() != null ? edtNewPass.getText().toString() : "";
        String confirmPass = edtConfirmPass.getText() != null ? edtConfirmPass.getText().toString() : "";

        boolean ok = true;
        if (newPass.length() < 6) {
            tilNewPass.setError("Mật khẩu mới phải từ 6 ký tự trở lên");
            ok = false;
        }
        if (!newPass.equals(confirmPass)) {
            tilConfirmPass.setError("Mật khẩu xác nhận không khớp");
            ok = false;
        }
        if (!ok) return;

        if (db.resetPassword(validatedEmail, newPass)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Thành công")
                    .setMessage("Mật khẩu cho tài khoản " + validatedEmail + " đã được đổi thành công. Bạn có thể đăng nhập bằng mật khẩu mới ngay bây giờ!")
                    .setPositiveButton("Đăng nhập ngay", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
        } else {
            Utils.toast(this, "Lỗi: Không thể cập nhật mật khẩu mới vào cơ sở dữ liệu");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
