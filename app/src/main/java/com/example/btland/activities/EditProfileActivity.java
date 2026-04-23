package com.example.btland.activities;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.btland.databinding.ActivityEditProfileBinding;
import com.example.btland.utils.FirebaseStorageHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private FirebaseFirestore db;
    private String uid;
    private Uri avatarUri;
    private ActivityResultLauncher<PickVisualMediaRequest> pickAvatarLauncher;

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

        pickAvatarLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        avatarUri = uri;
                        Glide.with(this).load(uri).circleCrop().into(binding.imgAvatar);
                    }
                });

        binding.btnChangeAvatar.setOnClickListener(v ->
                pickAvatarLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());
        loadProfile();
    }

    private void loadProfile() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.edtName.setText(documentSnapshot.getString("name"));
                    binding.edtPhone.setText(documentSnapshot.getString("phone"));

                    String avatarUrl = documentSnapshot.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this).load(avatarUrl).circleCrop().into(binding.imgAvatar);
                    } else {
                        binding.imgAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
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

        binding.btnSaveProfile.setEnabled(false);

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("phone", phone);

        if (avatarUri == null) {
            saveProfileDocument(data);
            return;
        }

        String storagePath = "avatars/" + uid + "/profile_" + System.currentTimeMillis() + ".jpg";
        FirebaseStorageHelper.uploadFile(avatarUri, storagePath, new FirebaseStorageHelper.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl, String storagePath) {
                data.put("avatarUrl", downloadUrl);
                saveProfileDocument(data);
            }

            @Override
            public void onError(String errorMessage) {
                binding.btnSaveProfile.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, "Upload ảnh thất bại: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfileDocument(Map<String, Object> data) {
        db.collection("users").document(uid)
                .update(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                });
    }
}
