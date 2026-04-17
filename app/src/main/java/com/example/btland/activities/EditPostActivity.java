package com.example.btland.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.btland.databinding.ActivityEditPostBinding;
import com.example.btland.models.Post;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class EditPostActivity extends AppCompatActivity {

    private ActivityEditPostBinding binding;
    private FirebaseFirestore db;
    private Post post;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        String postJson = getIntent().getStringExtra("post_json");
        if (postJson == null || postJson.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu bài đăng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        post = new Gson().fromJson(postJson, Post.class);
        bindData();

        binding.btnUpdatePost.setOnClickListener(v -> updatePost());
    }

    private void bindData() {
        binding.edtTitle.setText(post.getTitle());
        binding.edtDescription.setText(post.getDescription());
        binding.edtPrice.setText(String.valueOf(post.getPrice()));
        binding.edtArea.setText(String.valueOf(post.getArea()));
        binding.edtAddress.setText(post.getAddress());
        binding.edtDistrict.setText(post.getDistrict());
        binding.edtLat.setText(String.valueOf(post.getLat()));
        binding.edtLng.setText(String.valueOf(post.getLng()));

        if ("rent".equals(post.getType())) {
            binding.radioRent.setChecked(true);
        } else {
            binding.radioRoommate.setChecked(true);
        }
    }

    private void updatePost() {
        String title = binding.edtTitle.getText().toString().trim();
        String description = binding.edtDescription.getText().toString().trim();
        String priceText = binding.edtPrice.getText().toString().trim();
        String areaText = binding.edtArea.getText().toString().trim();
        String address = binding.edtAddress.getText().toString().trim();
        String district = binding.edtDistrict.getText().toString().trim();
        String latText = binding.edtLat.getText().toString().trim();
        String lngText = binding.edtLng.getText().toString().trim();
        String type = binding.radioRent.isChecked() ? "rent" : "roommate";

        if (title.isEmpty() || description.isEmpty() || priceText.isEmpty() || areaText.isEmpty()
                || address.isEmpty() || district.isEmpty() || latText.isEmpty() || lngText.isEmpty()) {
            Toast.makeText(this, "Nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        double price, area, lat, lng;
        try {
            price = Double.parseDouble(priceText);
            area = Double.parseDouble(areaText);
            lat = Double.parseDouble(latText);
            lng = Double.parseDouble(lngText);
        } catch (Exception e) {
            Toast.makeText(this, "Dữ liệu số không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("title", title);
        updateData.put("description", description);
        updateData.put("price", price);
        updateData.put("area", area);
        updateData.put("address", address);
        updateData.put("district", district);
        updateData.put("lat", lat);
        updateData.put("lng", lng);
        updateData.put("type", type);

        db.collection("posts")
                .document(post.getPostId())
                .update(updateData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                );
    }
}