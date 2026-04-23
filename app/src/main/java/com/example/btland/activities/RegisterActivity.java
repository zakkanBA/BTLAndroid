package com.example.btland.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.btland.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = binding.edtName.getText().toString().trim();
        String email = binding.edtEmail.getText().toString().trim();
        String phone = binding.edtPhone.getText().toString().trim();
        String password = binding.edtPassword.getText().toString().trim();
        String confirmPassword = binding.edtConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không đúng định dạng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || task.getResult().getUser() == null) {
                        String message = task.getException() != null
                                && task.getException().getMessage() != null
                                && task.getException().getMessage().contains("email address is already in use")
                                ? "Email đã được sử dụng"
                                : "Đăng ký thất bại";
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = task.getResult().getUser().getUid();
                    Map<String, Object> user = new HashMap<>();
                    user.put("userId", uid);
                    user.put("name", name);
                    user.put("email", email);
                    user.put("phone", phone);
                    user.put("avatarUrl", "");
                    user.put("isAdmin", false);
                    user.put("isBanned", false);

                    db.collection("users").document(uid)
                            .set(user)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, MainActivity.class));
                                finishAffinity();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Lưu người dùng thất bại", Toast.LENGTH_SHORT).show()
                            );
                });
    }
}
