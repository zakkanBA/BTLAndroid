package com.example.btland.activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.btland.databinding.ActivityCreatePostBinding;
import com.example.btland.utils.CloudinaryHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreatePostActivity extends AppCompatActivity {

    private ActivityCreatePostBinding binding;

    private final List<Uri> selectedImageUris = new ArrayList<>();
    private Uri cameraImageUri;
    private Uri panoramaImageUri;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ActivityResultLauncher<PickVisualMediaRequest> pickImagesLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> pickPanoramaLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestReadPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initLaunchers();
        initViews();
    }

    private void initViews() {
        binding.radioRent.setChecked(true);
        updateImageCount();
        updatePanoramaStatus();

        binding.btnPickImages.setOnClickListener(v -> openGallery());
        binding.btnCaptureImage.setOnClickListener(v -> openCamera());
        binding.btnCreatePanorama.setOnClickListener(v -> openPanoramaPicker());
        binding.btnCreatePost.setOnClickListener(v -> validateAndCreatePost());
    }

    private void initLaunchers() {
        pickImagesLauncher = registerForActivityResult(
                new ActivityResultContracts.PickMultipleVisualMedia(3),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        selectedImageUris.clear();
                        selectedImageUris.addAll(uris);
                        showPreview();
                        updateImageCount();
                    }
                });

        pickPanoramaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        panoramaImageUri = uri;
                        updatePanoramaStatus();
                        Toast.makeText(this, "Đã chọn ảnh panorama", Toast.LENGTH_SHORT).show();
                    }
                });

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        selectedImageUris.clear();
                        selectedImageUris.add(cameraImageUri);
                        showPreview();
                        updateImageCount();
                    } else {
                        Toast.makeText(this, "Chụp ảnh thất bại", Toast.LENGTH_SHORT).show();
                    }
                });

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        Toast.makeText(this, "Bạn đã từ chối quyền camera", Toast.LENGTH_SHORT).show();
                    }
                });

        requestReadPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchGallery();
                    } else {
                        Toast.makeText(this, "Bạn đã từ chối quyền đọc ảnh", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launchGallery();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            launchGallery();
        } else {
            requestReadPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void launchGallery() {
        pickImagesLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        );
    }

    private void openPanoramaPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launchPanoramaPicker();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            launchPanoramaPicker();
        } else {
            requestReadPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void launchPanoramaPicker() {
        pickPanoramaLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        );
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "btland_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            cameraImageUri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
            );

            if (cameraImageUri != null) {
                takePictureLauncher.launch(cameraImageUri);
            } else {
                Toast.makeText(this, "Không tạo được ảnh camera", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi mở camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showPreview() {
        if (!selectedImageUris.isEmpty()) {
            Glide.with(this)
                    .load(selectedImageUris.get(0))
                    .centerCrop()
                    .into(binding.imgPreview);
        }
    }

    private void updateImageCount() {
        binding.txtImageCount.setText("Đã chọn: " + selectedImageUris.size() + " ảnh");
    }

    private void updatePanoramaStatus() {
        binding.txtPanoramaStatus.setText(
                panoramaImageUri == null ? "Chưa có ảnh panorama" : "Đã có ảnh panorama"
        );
    }

    private void validateAndCreatePost() {
        String title = binding.edtTitle.getText().toString().trim();
        String description = binding.edtDescription.getText().toString().trim();
        String priceText = binding.edtPrice.getText().toString().trim();
        String areaText = binding.edtArea.getText().toString().trim();
        String address = binding.edtAddress.getText().toString().trim();
        String district = binding.edtDistrict.getText().toString().trim();
        String type = binding.radioRent.isChecked() ? "rent" : "roommate";

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty() || description.isEmpty() || priceText.isEmpty()
                || areaText.isEmpty() || address.isEmpty() || district.isEmpty()) {
            Toast.makeText(this, "Nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 ảnh", Toast.LENGTH_SHORT).show();
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

        binding.btnCreatePost.setEnabled(false);

        getLatLngFromAddress(address, new OnLocationResult() {
            @Override
            public void onSuccess(double lat, double lng) {
                uploadImagesAndSavePost(title, description, price, area, address, district, lat, lng, type);
            }

            @Override
            public void onError(String error) {
                binding.btnCreatePost.setEnabled(true);
                Toast.makeText(CreatePostActivity.this,
                        "Không tìm được vị trí từ địa chỉ", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getLatLngFromAddress(String addressText, OnLocationResult callback) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> result = geocoder.getFromLocationName(addressText, 1);

                if (result != null && !result.isEmpty()) {
                    double lat = result.get(0).getLatitude();
                    double lng = result.get(0).getLongitude();
                    runOnUiThread(() -> callback.onSuccess(lat, lng));
                } else {
                    runOnUiThread(() -> callback.onError("Không tìm thấy"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    interface OnLocationResult {
        void onSuccess(double lat, double lng);
        void onError(String error);
    }

    private void uploadImagesAndSavePost(String title, String description, double price, double area,
                                         String address, String district, double lat, double lng, String type) {
        List<String> uploadedUrls = new ArrayList<>();
        uploadSingleImage(0, uploadedUrls, title, description, price, area, address, district, lat, lng, type);
    }

    private void uploadSingleImage(int index, List<String> uploadedUrls, String title, String description,
                                   double price, double area, String address, String district,
                                   double lat, double lng, String type) {
        if (index >= selectedImageUris.size()) {
            uploadPanoramaIfNeeded(title, description, price, area, address, district, lat, lng, type, uploadedUrls);
            return;
        }

        Uri imageUri = selectedImageUris.get(index);

        CloudinaryHelper.uploadImage(this, imageUri, new CloudinaryHelper.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                runOnUiThread(() -> {
                    uploadedUrls.add(imageUrl);
                    uploadSingleImage(index + 1, uploadedUrls, title, description, price, area,
                            address, district, lat, lng, type);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    binding.btnCreatePost.setEnabled(true);
                    Toast.makeText(CreatePostActivity.this,
                            "Upload ảnh thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void uploadPanoramaIfNeeded(String title, String description, double price, double area,
                                        String address, String district, double lat, double lng,
                                        String type, List<String> imageUrls) {
        if (panoramaImageUri == null) {
            savePostToFirestore(title, description, price, area, address, district, lat, lng, type, imageUrls, "");
            return;
        }

        CloudinaryHelper.uploadImage(this, panoramaImageUri, new CloudinaryHelper.UploadCallback() {
            @Override
            public void onSuccess(String panoramaUrl) {
                runOnUiThread(() ->
                        savePostToFirestore(title, description, price, area, address, district, lat, lng, type, imageUrls, panoramaUrl)
                );
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    binding.btnCreatePost.setEnabled(true);
                    Toast.makeText(CreatePostActivity.this,
                            "Upload ảnh panorama thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void savePostToFirestore(String title, String description, double price, double area,
                                     String address, String district, double lat, double lng,
                                     String type, List<String> imageUrls, String panoramaUrl) {
        String uid = auth.getCurrentUser().getUid();
        String postId = db.collection("posts").document().getId();

        Map<String, Object> post = new HashMap<>();
        post.put("postId", postId);
        post.put("userId", uid);
        post.put("title", title);
        post.put("description", description);
        post.put("price", price);
        post.put("area", area);
        post.put("address", address);
        post.put("district", district);
        post.put("lat", lat);
        post.put("lng", lng);
        post.put("type", type);
        post.put("images", imageUrls);
        post.put("panoramaImage", panoramaUrl);
        post.put("status", "active");
        post.put("createdAt", Timestamp.now());

        db.collection("posts")
                .document(postId)
                .set(post)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đăng bài thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnCreatePost.setEnabled(true);
                    Toast.makeText(this, "Lưu bài thất bại", Toast.LENGTH_SHORT).show();
                });
    }
}