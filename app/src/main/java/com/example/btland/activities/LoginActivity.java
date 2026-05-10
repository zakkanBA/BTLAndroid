package com.example.btland.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btland.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.btnLogin.setOnClickListener(v -> login());
        binding.btnForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void login() {
        String email = binding.edtEmail.getText().toString().trim();
        String password = binding.edtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không đúng định dạng", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (!task.isSuccessful() || user == null) {
                        Toast.makeText(this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    user.reload().addOnCompleteListener(reloadTask -> {
                        FirebaseUser reloadedUser = auth.getCurrentUser();
                        if (reloadedUser == null || !reloadedUser.isEmailVerified()) {
                            if (reloadedUser != null) {
                                reloadedUser.sendEmailVerification();
                            }
                            auth.signOut();
                            Toast.makeText(this, "Email chưa xác minh. Hãy kiểm tra hộp thư để xác nhận tài khoản.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        checkBanAndOpenApp(reloadedUser.getUid());
                    });
                });
    }

    private void checkBanAndOpenApp(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean isBanned = Boolean.TRUE.equals(documentSnapshot.getBoolean("isBanned"));
                    if (isBanned) {
                        auth.signOut();
                        Toast.makeText(this, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finishAffinity();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không đọc được dữ liệu người dùng", Toast.LENGTH_SHORT).show()
                );
    }

    private void showForgotPasswordDialog() {
        EditText input = new EditText(this);
        input.setHint("Email đã đăng ký");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setSingleLine(true);
        input.setText(binding.edtEmail.getText().toString().trim());

        new AlertDialog.Builder(this)
                .setTitle("Lấy lại mật khẩu")
                .setMessage("Nhập email đã đăng ký. Firebase sẽ gửi link đặt lại mật khẩu về email này.")
                .setView(input)
                .setPositiveButton("Gửi link", (dialog, which) -> sendPasswordReset(input.getText().toString().trim()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void sendPasswordReset(String email) {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Đã gửi link đặt lại mật khẩu. Hãy kiểm tra email.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không gửi được email đặt lại mật khẩu: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
