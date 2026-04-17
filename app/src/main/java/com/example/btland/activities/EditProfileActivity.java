package com.example.btland.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.btland.databinding.ActivityEditProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            finish();
            return;
        }

        loadProfile();

        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.edtName.setText(documentSnapshot.getString("name"));
                    binding.edtPhone.setText(documentSnapshot.getString("phone"));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không tải được thông tin", Toast.LENGTH_SHORT).show()
                );
    }

    private void saveProfile() {
        String name = binding.edtName.getText().toString().trim();
        String phone = binding.edtPhone.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("phone", phone);

        db.collection("users").document(uid)
                .update(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                );
    }
}