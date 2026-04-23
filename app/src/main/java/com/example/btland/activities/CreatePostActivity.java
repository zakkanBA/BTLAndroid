package com.example.btland.activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.btland.adapters.MediaPreviewAdapter;
import com.example.btland.databinding.ActivityCreatePostBinding;
import com.example.btland.utils.FirebaseStorageHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
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
    private FusedLocationProviderClient locationClient;
    private MediaPreviewAdapter selectedImagesAdapter;
    private MediaPreviewAdapter previewImagesAdapter;

    private double selectedLat;
    private double selectedLng;

    private ActivityResultLauncher<PickVisualMediaRequest> pickImagesLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> pickPanoramaLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestLocationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        initLaunchers();
        initViews();
    }

    private void initViews() {
        binding.radioRent.setChecked(true);

        ArrayAdapter<String> roomTypeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Phòng trọ", "Chung cư mini", "Nhà nguyên căn"}
        );
        roomTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerRoomType.setAdapter(roomTypeAdapter);

        selectedImagesAdapter = new MediaPreviewAdapter();
        binding.recyclerImages.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerImages.setAdapter(selectedImagesAdapter);

        previewImagesAdapter = new MediaPreviewAdapter();
        binding.recyclerPreviewImages.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerPreviewImages.setAdapter(previewImagesAdapter);

        binding.btnPickImages.setOnClickListener(v -> pickImages());
        binding.btnCaptureImage.setOnClickListener(v -> openCameraWithRationale());
        binding.btnCreatePanorama.setOnClickListener(v -> pickPanorama());
        binding.btnUseCurrentLocation.setOnClickListener(v -> useCurrentLocation());
        binding.btnPreviewPost.setOnClickListener(v -> showPreviewCard());
        binding.btnCreatePost.setOnClickListener(v -> validateAndCreatePost());

        updateImageCount();
        updatePanoramaStatus();
        binding.cardPreview.setVisibility(android.view.View.GONE);
    }

    private void initLaunchers() {
        pickImagesLauncher = registerForActivityResult(
                new ActivityResultContracts.PickMultipleVisualMedia(6),
                uris -> {
                    if (uris == null || uris.isEmpty()) {
                        return;
                    }
                    selectedImageUris.clear();
                    selectedImageUris.addAll(uris.subList(0, Math.min(uris.size(), 6)));
                    refreshSelectedImages();
                });

        pickPanoramaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri == null) {
                        return;
                    }
                    if (!isPanoramaAspectValid(uri)) {
                        Toast.makeText(this, "Ảnh 360 nên có tỷ lệ gần 2:1", Toast.LENGTH_LONG).show();
                        return;
                    }
                    panoramaImageUri = uri;
                    updatePanoramaStatus();
                });

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        if (selectedImageUris.size() == 6) {
                            selectedImageUris.remove(selectedImageUris.size() - 1);
                        }
                        selectedImageUris.add(0, cameraImageUri);
                        refreshSelectedImages();
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

        requestLocationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        fetchCurrentLocation();
                    } else {
                        Toast.makeText(this, "Không thể lấy vị trí nếu chưa cấp quyền", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void pickImages() {
        pickImagesLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void pickPanorama() {
        pickPanoramaLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void openCameraWithRationale() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Quyền Camera")
                .setMessage("Ứng dụng cần quyền Camera để chụp ảnh thực tế của phòng ngay trong màn hình đăng tin.")
                .setPositiveButton("Tiếp tục", (dialog, which) ->
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA))
                .setNegativeButton("Hủy", null)
                .show();
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

            if (cameraImageUri == null) {
                Toast.makeText(this, "Không tạo được ảnh camera", Toast.LENGTH_SHORT).show();
                return;
            }
            takePictureLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi mở camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void useCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Quyền Vị trí")
                .setMessage("Ứng dụng cần quyền vị trí để lấy tọa độ GPS khi bạn muốn dùng vị trí hiện tại cho bài đăng.")
                .setPositiveButton("Tiếp tục", (dialog, which) ->
                        requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void fetchCurrentLocation() {
        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        applySelectedLocation(location);
                        return;
                    }
                    requestFreshLocation();
                })
                .addOnFailureListener(e -> requestFreshLocation());
    }

    private void requestFreshLocation() {
        CancellationTokenSource tokenSource = new CancellationTokenSource();
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        Toast.makeText(this, "Không lấy được vị trí hiện tại", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    applySelectedLocation(location);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không lấy được vị trí hiện tại", Toast.LENGTH_SHORT).show());
    }

    private void applySelectedLocation(Location location) {
        selectedLat = location.getLatitude();
        selectedLng = location.getLongitude();
        fillAddressFromLocation(location);
    }

    private void fillAddressFromLocation(Location location) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (results == null || results.isEmpty()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Không đọc được địa chỉ từ GPS", Toast.LENGTH_SHORT).show());
                    return;
                }

                Address address = results.get(0);
                String fullAddress = address.getAddressLine(0);
                String district = address.getSubAdminArea();
                runOnUiThread(() -> {
                    binding.edtAddress.setText(fullAddress);
                    if (district != null) {
                        binding.edtDistrict.setText(district);
                    }
                    Toast.makeText(this, "Đã dùng vị trí hiện tại", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Không suy ra được địa chỉ từ GPS", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void refreshSelectedImages() {
        List<String> sources = new ArrayList<>();
        for (Uri uri : selectedImageUris) {
            sources.add(uri.toString());
        }
        selectedImagesAdapter.submitItems(sources);
        updateImageCount();
    }

    private void updateImageCount() {
        binding.txtImageCount.setText("Ảnh thường: " + selectedImageUris.size() + "/6");
    }

    private void updatePanoramaStatus() {
        binding.txtPanoramaStatus.setText(
                panoramaImageUri == null ? "Chưa có ảnh 360" : "Đã chọn ảnh 360 hợp lệ"
        );
    }

    private List<String> collectAmenities() {
        List<String> amenities = new ArrayList<>();
        if (binding.cbWifi.isChecked()) amenities.add("Wi-Fi");
        if (binding.cbParking.isChecked()) amenities.add("Chỗ để xe");
        if (binding.cbPrivateWc.isChecked()) amenities.add("WC riêng");
        if (binding.cbAirConditioner.isChecked()) amenities.add("Máy lạnh");
        return amenities;
    }

    private void showPreviewCard() {
        String title = binding.edtTitle.getText().toString().trim();
        String address = binding.edtAddress.getText().toString().trim();
        String typeLabel = binding.radioRent.isChecked() ? "Cho thuê" : "Tìm người ở ghép";
        String roomType = String.valueOf(binding.spinnerRoomType.getSelectedItem());
        List<String> amenities = collectAmenities();

        if (title.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Nhập ít nhất tiêu đề và địa chỉ trước khi xem trước", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.cardPreview.setVisibility(android.view.View.VISIBLE);
        binding.txtPreviewSummary.setText(
                "Loại tin: " + typeLabel
                        + "\nLoại phòng: " + roomType
                        + "\nĐịa chỉ: " + address
                        + "\nTiện ích: " + (amenities.isEmpty() ? "Không chọn" : android.text.TextUtils.join(", ", amenities))
                        + "\nẢnh 360: " + (panoramaImageUri == null ? "Không có" : "Có")
        );

        List<String> previewSources = new ArrayList<>();
        for (Uri uri : selectedImageUris) {
            previewSources.add(uri.toString());
        }
        previewImagesAdapter.submitItems(previewSources);
    }

    private void validateAndCreatePost() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = binding.edtTitle.getText().toString().trim();
        String description = binding.edtDescription.getText().toString().trim();
        String priceText = binding.edtPrice.getText().toString().trim();
        String areaText = binding.edtArea.getText().toString().trim();
        String address = binding.edtAddress.getText().toString().trim();
        String district = binding.edtDistrict.getText().toString().trim();
        String type = binding.radioRent.isChecked() ? "rent" : "roommate";
        String roomType = String.valueOf(binding.spinnerRoomType.getSelectedItem());

        if (title.isEmpty() || description.isEmpty() || priceText.isEmpty()
                || areaText.isEmpty() || address.isEmpty() || district.isEmpty()) {
            Toast.makeText(this, "Nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.length() > 100) {
            Toast.makeText(this, "Tiêu đề không được quá 100 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 ảnh thường", Toast.LENGTH_SHORT).show();
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
        ensureUserCanPost(new UserCheckCallback() {
            @Override
            public void onReady(String ownerName, String ownerPhone) {
                resolveLocationIfNeeded(address, new OnLocationResult() {
                    @Override
                    public void onSuccess(double lat, double lng) {
                        createPostWithUploads(title, description, price, area, address, district, type,
                                roomType, ownerName, ownerPhone, lat, lng, collectAmenities());
                    }

                    @Override
                    public void onError(String error) {
                        binding.btnCreatePost.setEnabled(true);
                        Toast.makeText(CreatePostActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onBlocked(String message) {
                binding.btnCreatePost.setEnabled(true);
                Toast.makeText(CreatePostActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void ensureUserCanPost(UserCheckCallback callback) {
        String uid = auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
        if (uid == null) {
            callback.onBlocked("Bạn chưa đăng nhập");
            return;
        }

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean isBanned = Boolean.TRUE.equals(documentSnapshot.getBoolean("isBanned"));
                    if (isBanned) {
                        callback.onBlocked("Tài khoản của bạn đã bị khóa. Bạn không thể đăng tin mới.");
                        return;
                    }

                    String ownerName = documentSnapshot.getString("name");
                    String ownerPhone = documentSnapshot.getString("phone");
                    callback.onReady(ownerName == null ? "" : ownerName, ownerPhone == null ? "" : ownerPhone);
                })
                .addOnFailureListener(e -> callback.onBlocked("Không kiểm tra được trạng thái tài khoản"));
    }

    private void resolveLocationIfNeeded(String address, OnLocationResult callback) {
        if (selectedLat != 0d || selectedLng != 0d) {
            callback.onSuccess(selectedLat, selectedLng);
            return;
        }

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> result = geocoder.getFromLocationName(address, 1);
                if (result != null && !result.isEmpty()) {
                    double lat = result.get(0).getLatitude();
                    double lng = result.get(0).getLongitude();
                    selectedLat = lat;
                    selectedLng = lng;
                    runOnUiThread(() -> callback.onSuccess(lat, lng));
                } else {
                    runOnUiThread(() -> callback.onError("Không tìm được tọa độ từ địa chỉ"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> callback.onError("Không xác định được vị trí từ địa chỉ"));
            }
        }).start();
    }

    private void createPostWithUploads(String title, String description, double price, double area,
                                       String address, String district, String type, String roomType,
                                       String ownerName, String ownerPhone, double lat, double lng,
                                       List<String> amenities) {
        String postId = db.collection("posts").document().getId();
        String storageFolder = "posts/" + postId;
        List<String> uploadedImageUrls = new ArrayList<>();

        uploadRegularImages(0, storageFolder, uploadedImageUrls, new UploadChainCallback() {
            @Override
            public void onSuccess() {
                uploadPanorama(storageFolder, new PanoramaUploadCallback() {
                    @Override
                    public void onComplete(String panoramaUrl, String panoramaPath) {
                        savePostDocument(postId, storageFolder, title, description, price, area, address, district,
                                type, roomType, ownerName, ownerPhone, lat, lng, amenities,
                                uploadedImageUrls, panoramaUrl, panoramaPath);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        binding.btnCreatePost.setEnabled(true);
                        Toast.makeText(CreatePostActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                binding.btnCreatePost.setEnabled(true);
                Toast.makeText(CreatePostActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadRegularImages(int index, String storageFolder, List<String> uploadedImageUrls,
                                     UploadChainCallback callback) {
        if (index >= selectedImageUris.size()) {
            callback.onSuccess();
            return;
        }

        String storagePath = storageFolder + "/image_" + System.currentTimeMillis() + "_" + index + ".jpg";
        FirebaseStorageHelper.uploadFile(selectedImageUris.get(index), storagePath, new FirebaseStorageHelper.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl, String storagePath) {
                uploadedImageUrls.add(downloadUrl);
                uploadRegularImages(index + 1, storageFolder, uploadedImageUrls, callback);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError("Upload ảnh thất bại: " + errorMessage);
            }
        });
    }

    private void uploadPanorama(String storageFolder, PanoramaUploadCallback callback) {
        if (panoramaImageUri == null) {
            callback.onComplete("", "");
            return;
        }

        String storagePath = storageFolder + "/panorama_" + System.currentTimeMillis() + ".jpg";
        FirebaseStorageHelper.uploadFile(panoramaImageUri, storagePath, new FirebaseStorageHelper.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl, String storagePath) {
                callback.onComplete(downloadUrl, storagePath);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError("Upload ảnh 360 thất bại: " + errorMessage);
            }
        });
    }

    private void savePostDocument(String postId, String storageFolder, String title, String description,
                                  double price, double area, String address, String district, String type,
                                  String roomType, String ownerName, String ownerPhone, double lat, double lng,
                                  List<String> amenities, List<String> imageUrls,
                                  String panoramaUrl, String panoramaPath) {
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> post = new HashMap<>();
        post.put("postId", postId);
        post.put("userId", uid);
        post.put("ownerName", ownerName);
        post.put("ownerPhone", ownerPhone);
        post.put("title", title);
        post.put("description", description);
        post.put("price", price);
        post.put("area", area);
        post.put("address", address);
        post.put("district", district);
        post.put("roomType", roomType);
        post.put("lat", lat);
        post.put("lng", lng);
        post.put("type", type);
        post.put("amenities", amenities);
        post.put("images", imageUrls);
        post.put("panoramaImage", panoramaUrl);
        post.put("panoramaPath", panoramaPath);
        post.put("storageFolder", storageFolder);
        post.put("status", "active");
        post.put("active", true);
        post.put("createdAt", Timestamp.now());
        post.put("updatedAt", Timestamp.now());

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

    private boolean isPanoramaAspectValid(Uri uri) {
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(stream, null, options);
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return false;
            }
            double ratio = options.outWidth / (double) options.outHeight;
            return ratio >= 1.85 && ratio <= 2.15;
        } catch (Exception e) {
            return false;
        }
    }

    interface OnLocationResult {
        void onSuccess(double lat, double lng);
        void onError(String error);
    }

    interface UploadChainCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    interface PanoramaUploadCallback {
        void onComplete(String panoramaUrl, String panoramaPath);
        void onError(String errorMessage);
    }

    interface UserCheckCallback {
        void onReady(String ownerName, String ownerPhone);
        void onBlocked(String message);
    }
}
