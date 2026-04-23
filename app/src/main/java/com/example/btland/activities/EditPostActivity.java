package com.example.btland.activities;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.btland.databinding.ActivityEditPostBinding;
import com.example.btland.models.Post;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
        ArrayAdapter<String> roomTypeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Phòng trọ", "Chung cư mini", "Nhà nguyên căn"}
        );
        roomTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerRoomType.setAdapter(roomTypeAdapter);

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

        if ("rent".equals(post.getType())) {
            binding.radioRent.setChecked(true);
        } else {
            binding.radioRoommate.setChecked(true);
        }

        if (post.getRoomType() != null) {
            for (int i = 0; i < binding.spinnerRoomType.getCount(); i++) {
                if (post.getRoomType().equals(binding.spinnerRoomType.getItemAtPosition(i))) {
                    binding.spinnerRoomType.setSelection(i);
                    break;
                }
            }
        }

        List<String> amenities = post.getAmenities() == null ? new ArrayList<>() : post.getAmenities();
        binding.cbWifi.setChecked(amenities.contains("Wi-Fi"));
        binding.cbParking.setChecked(amenities.contains("Chỗ để xe"));
        binding.cbPrivateWc.setChecked(amenities.contains("WC riêng"));
        binding.cbAirConditioner.setChecked(amenities.contains("Máy lạnh"));
    }

    private List<String> collectAmenities() {
        List<String> amenities = new ArrayList<>();
        if (binding.cbWifi.isChecked()) amenities.add("Wi-Fi");
        if (binding.cbParking.isChecked()) amenities.add("Chỗ để xe");
        if (binding.cbPrivateWc.isChecked()) amenities.add("WC riêng");
        if (binding.cbAirConditioner.isChecked()) amenities.add("Máy lạnh");
        return amenities;
    }

    private void updatePost() {
        String title = binding.edtTitle.getText().toString().trim();
        String description = binding.edtDescription.getText().toString().trim();
        String priceText = binding.edtPrice.getText().toString().trim();
        String areaText = binding.edtArea.getText().toString().trim();
        String address = binding.edtAddress.getText().toString().trim();
        String district = binding.edtDistrict.getText().toString().trim();
        String type = binding.radioRent.isChecked() ? "rent" : "roommate";
        String roomType = String.valueOf(binding.spinnerRoomType.getSelectedItem());

        if (title.isEmpty() || description.isEmpty() || priceText.isEmpty() || areaText.isEmpty()
                || address.isEmpty() || district.isEmpty()) {
            Toast.makeText(this, "Nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        double area;
        try {
            price = Double.parseDouble(priceText);
            area = Double.parseDouble(areaText);
        } catch (Exception e) {
            Toast.makeText(this, "Giá hoặc diện tích không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        geocodeAddress(address, new OnLocationResult() {
            @Override
            public void onSuccess(double lat, double lng) {
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
                updateData.put("roomType", roomType);
                updateData.put("amenities", collectAmenities());
                updateData.put("updatedAt", Timestamp.now());

                db.collection("posts")
                        .document(post.getPostId())
                        .update(updateData)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(EditPostActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(EditPostActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                        );
            }

            @Override
            public void onError(String message) {
                Toast.makeText(EditPostActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void geocodeAddress(String addressText, OnLocationResult callback) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> result = geocoder.getFromLocationName(addressText, 1);
                if (result != null && !result.isEmpty()) {
                    double lat = result.get(0).getLatitude();
                    double lng = result.get(0).getLongitude();
                    runOnUiThread(() -> callback.onSuccess(lat, lng));
                } else {
                    runOnUiThread(() -> callback.onError("Không tìm được vị trí từ địa chỉ"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> callback.onError("Không cập nhật được vị trí"));
            }
        }).start();
    }

    interface OnLocationResult {
        void onSuccess(double lat, double lng);
        void onError(String message);
    }
}
