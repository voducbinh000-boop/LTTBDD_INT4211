package com.example.quanlychitieu;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout tilUser, tilPass;
    private TextInputEditText edtUser, edtPass;
    private DBHelper db;

    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        db = DBHelper.get(this);

        tilUser = findViewById(R.id.tilUser);
        tilPass = findViewById(R.id.tilPass);
        edtUser = findViewById(R.id.edtUser);
        edtPass = findViewById(R.id.edtPass);

        findViewById(R.id.btnLogin).setOnClickListener(v -> doLogin());
        findViewById(R.id.btnGoogle).setOnClickListener(v -> doGoogleSignIn());
        findViewById(R.id.tvForgotPassword).setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
        findViewById(R.id.tvGoRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        setupGoogleSignIn();
    }

    private void setupGoogleSignIn() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                firebaseAuthWithGoogle(account.getIdToken(), account);
                            }
                        } catch (ApiException e) {
                            showGoogleError("Đăng nhập Google thất bại (Mã lỗi: " + e.getStatusCode() + ")");
                        }
                    }
                }
        );

        try {
            // Lấy Client ID tự động từ google-services.json nếu đã cấu hình
            int resId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
            String webClientId = resId != 0 ? getString(resId) : null;

            GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail();

            if (webClientId != null && !webClientId.isEmpty()) {
                builder.requestIdToken(webClientId);
            }

            googleSignInClient = GoogleSignIn.getClient(this, builder.build());
        } catch (Exception e) {
            googleSignInClient = null;
        }
    }

    private void doGoogleSignIn() {
        if (googleSignInClient == null) {
            showConfigGuide();
            return;
        }
        try {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        } catch (Exception e) {
            showConfigGuide();
        }
    }

    private void firebaseAuthWithGoogle(String idToken, GoogleSignInAccount account) {
        if (idToken == null) {
            // Nếu chạy trên môi trường thử nghiệm không có idToken từ Web Client
            // vẫn xử lý đồng bộ GoogleSignInAccount vào SQLite
            String email = account.getEmail();
            String name = account.getDisplayName();
            String id = account.getId();

            User u = db.syncFirebaseUser(name, email, id);
            Session.login(this, u.id);
            Utils.toast(this, "Đăng nhập Google thành công!");
            navigateToHome();
            return;
        }

        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
            auth.signInWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser fbUser = auth.getCurrentUser();
                            if (fbUser != null) {
                                String email = fbUser.getEmail();
                                String name = fbUser.getDisplayName();
                                String uid = fbUser.getUid();

                                User u = db.syncFirebaseUser(name, email, uid);
                                Session.login(this, u.id);
                                Utils.toast(this, "Đăng nhập Google thành công!");
                                navigateToHome();
                            }
                        } else {
                            String err = task.getException() != null ? task.getException().getMessage() : "Lỗi xác thực Firebase";
                            showGoogleError(err);
                        }
                    });
        } catch (Exception e) {
            // Fallback khi Firebase chưa liên kết SHA-1
            String email = account.getEmail();
            String name = account.getDisplayName();
            String id = account.getId();

            User u = db.syncFirebaseUser(name, email, id);
            Session.login(this, u.id);
            Utils.toast(this, "Đăng nhập Google thành công (Chế độ tương thích)");
            navigateToHome();
        }
    }

    private void showGoogleError(String errorMsg) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Đăng nhập Google không thành công")
                .setMessage(errorMsg + "\n\nLưu ý: Để đăng nhập Google thực tế, bạn cần thêm mã SHA-1 của máy tính vào Firebase Console và tải file google-services.json vào thư mục app/.")
                .setPositiveButton("Xem hướng dẫn", (d, w) -> showConfigGuide())
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showConfigGuide() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cấu hình Firebase")
                .setMessage("Để sử dụng đầy đủ tính năng Đăng nhập Google & Firebase Auth:\n\n1. Mở file HUONG_DAN_CAU_HINH_FIREBASE.md trong project.\n2. Thêm SHA-1 và tải file google-services.json từ Firebase Console vào thư mục app/.\n\nHiện tại bạn có thể đăng nhập hoặc đăng ký tài khoản cục bộ thông thường.")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private void doLogin() {
        tilUser.setError(null);
        tilPass.setError(null);
        String user = edtUser.getText() != null ? edtUser.getText().toString().trim() : "";
        String pass = edtPass.getText() != null ? edtPass.getText().toString() : "";

        boolean ok = true;
        if (user.isEmpty()) {
            tilUser.setError("Vui lòng nhập email hoặc tên đăng nhập");
            ok = false;
        }
        if (pass.isEmpty()) {
            tilPass.setError("Vui lòng nhập mật khẩu");
            ok = false;
        }
        if (!ok) return;

        User u = db.login(user, pass);
        if (u == null) {
            tilPass.setError("Sai tài khoản hoặc mật khẩu");
            Utils.toast(this, "Đăng nhập thất bại: Tài khoản hoặc mật khẩu không chính xác");
            return;
        }

        Session.login(this, u.id);
        Utils.toast(this, "Đăng nhập thành công! Chào mừng " + u.name);
        navigateToHome();
    }

    private void navigateToHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
