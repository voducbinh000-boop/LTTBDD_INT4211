package com.example.quanlychitieu;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout tilName, tilEmail, tilPass, tilConfirm;
    private TextInputEditText edtName, edtEmail, edtPass, edtConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPass = findViewById(R.id.tilPass);
        tilConfirm = findViewById(R.id.tilConfirm);
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPass = findViewById(R.id.edtPass);
        edtConfirm = findViewById(R.id.edtConfirm);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvGoLogin).setOnClickListener(v -> finish());
        findViewById(R.id.btnRegister).setOnClickListener(v -> doRegister());
    }

    private void doRegister() {
        tilName.setError(null); tilEmail.setError(null);
        tilPass.setError(null); tilConfirm.setError(null);
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String pass = edtPass.getText().toString();
        String confirm = edtConfirm.getText().toString();

        boolean ok = true;
        if (name.isEmpty()) { tilName.setError("Vui lòng nhập họ và tên"); ok = false; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { tilEmail.setError("Email không hợp lệ"); ok = false; }
        if (pass.length() < 6) { tilPass.setError("Mật khẩu phải có ít nhất 6 ký tự"); ok = false; }
        if (!pass.equals(confirm)) { tilConfirm.setError("Mật khẩu xác nhận không khớp"); ok = false; }
        if (!ok) return;

        long id = DBHelper.get(this).register(name, email, pass);
        if (id == -1) {
            tilEmail.setError("Email này đã được đăng ký trong hệ thống");
            Utils.toast(this, "Đăng ký thất bại: Email đã tồn tại");
            return;
        }

        try {
            com.google.firebase.auth.FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, pass);
        } catch (Exception ignored) {}

        Session.login(this, id);
        Utils.toast(this, "Đăng ký thành công! Chào mừng " + name);
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}
