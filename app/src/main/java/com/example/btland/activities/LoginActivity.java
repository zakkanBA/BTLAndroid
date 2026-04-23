package com.example.btland.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.btland.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
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

        binding.btnLogin.setOnClickListener(v -> {
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
                        if (!task.isSuccessful() || auth.getCurrentUser() == null) {
                            Toast.makeText(this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String uid = auth.getCurrentUser().getUid();
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
                    });
        });
    }
}
