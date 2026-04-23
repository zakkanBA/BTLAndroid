package com.example.btland.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.btland.R;
import com.example.btland.adapters.PostAdapter;
import com.example.btland.databinding.FragmentHomeBinding;
import com.example.btland.models.Post;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final String TYPE_ALL = "Tất cả";
    private static final String TYPE_RENT = "Cho thuê";
    private static final String TYPE_ROOMMATE = "Ở ghép";
    private static final String ROOM_TYPE_ALL = "Tất cả loại phòng";
    private static final String SORT_NEWEST = "Mới nhất";
    private static final String SORT_PRICE_ASC = "Giá tăng dần";
    private static final String SORT_PRICE_DESC = "Giá giảm dần";
    private static final String SORT_DISTANCE = "Khoảng cách gần nhất";

    private FragmentHomeBinding binding;
    private final List<Post> allPosts = new ArrayList<>();
    private final List<Post> filteredPosts = new ArrayList<>();
    private PostAdapter adapter;
    private FirebaseFirestore db;
    private FusedLocationProviderClient locationClient;
    private ActivityResultLauncher<String> requestLocationPermissionLauncher;
    private double userLat;
    private double userLng;
    private boolean isFilterVisible;
    private CancellationTokenSource locationTokenSource;

    public HomeFragment() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        locationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        adapter = new PostAdapter(filteredPosts);
        binding.recyclerPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerPosts.setAdapter(adapter);

        requestLocationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        fetchCurrentLocation(this::applyFilters);
                    } else {
                        fallbackToNewestSort("Cần cấp quyền vị trí để sắp xếp theo khoảng cách.");
                    }
                });

        setupSpinners();
        setupEvents();
        updateFilterPanelVisibility(false);
        updateFilterSummary();
        loadPosts();

        return binding.getRoot();
    }

    private void setupSpinners() {
        setupSpinner(binding.spinnerType, new String[]{TYPE_ALL, TYPE_RENT, TYPE_ROOMMATE});
        setupSpinner(binding.spinnerRoomType, new String[]{ROOM_TYPE_ALL, "Phòng trọ", "Chung cư mini", "Nhà nguyên căn"});
        setupSpinner(binding.spinnerSort, new String[]{SORT_NEWEST, SORT_PRICE_ASC, SORT_PRICE_DESC, SORT_DISTANCE});
    }

    private void setupSpinner(android.widget.Spinner spinner, String[] items) {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                items
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
    }

    private void setupEvents() {
        binding.btnToggleFilter.setOnClickListener(v -> updateFilterPanelVisibility(!isFilterVisible));
        binding.btnApplyFilter.setOnClickListener(v -> {
            applyFilters();
            updateFilterPanelVisibility(false);
        });
        binding.btnResetFilter.setOnClickListener(v -> {
            resetFilters();
            updateFilterPanelVisibility(false);
        });

        binding.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void updateFilterPanelVisibility(boolean visible) {
        isFilterVisible = visible;
        binding.cardFilterPanel.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.btnToggleFilter.setImageResource(visible ? R.drawable.ic_close : R.drawable.ic_filter_list);
        binding.btnToggleFilter.setContentDescription(visible ? "Ẩn bộ lọc" : "Hiện bộ lọc");
    }

    private void loadPosts() {
        db.collection("posts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allPosts.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        if (post != null && post.isActive()) {
                            allPosts.add(post);
                        }
                    }
                    applyFilters();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Không tải được dữ liệu bài đăng", Toast.LENGTH_SHORT).show()
                );
    }

    private void applyFilters() {
        if (binding == null) {
            return;
        }

        String keyword = binding.edtSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        String selectedType = String.valueOf(binding.spinnerType.getSelectedItem());
        String selectedRoomType = String.valueOf(binding.spinnerRoomType.getSelectedItem());
        String selectedSort = String.valueOf(binding.spinnerSort.getSelectedItem());
        String districtKeyword = binding.edtDistrict.getText().toString().trim().toLowerCase(Locale.ROOT);

        double minPrice = parseDoubleOrDefault(binding.edtMinPrice.getText().toString().trim(), 0d);
        double maxPrice = parseDoubleOrDefault(binding.edtMaxPrice.getText().toString().trim(), Double.MAX_VALUE);
        double minArea = parseDoubleOrDefault(binding.edtMinArea.getText().toString().trim(), 0d);
        double maxArea = parseDoubleOrDefault(binding.edtMaxArea.getText().toString().trim(), Double.MAX_VALUE);

        filteredPosts.clear();
        List<String> requiredAmenities = collectRequiredAmenities();

        for (Post post : allPosts) {
            boolean matchKeyword = keyword.isEmpty()
                    || containsIgnoreCase(post.getTitle(), keyword)
                    || containsIgnoreCase(post.getAddress(), keyword);

            boolean matchType = TYPE_ALL.equals(selectedType)
                    || (TYPE_RENT.equals(selectedType) && "rent".equals(post.getType()))
                    || (TYPE_ROOMMATE.equals(selectedType) && "roommate".equals(post.getType()));

            boolean matchRoomType = ROOM_TYPE_ALL.equals(selectedRoomType)
                    || selectedRoomType.equals(post.getRoomType());

            boolean matchDistrict = districtKeyword.isEmpty()
                    || containsIgnoreCase(post.getDistrict(), districtKeyword)
                    || containsIgnoreCase(post.getAddress(), districtKeyword);

            boolean matchPrice = post.getPrice() >= minPrice && post.getPrice() <= maxPrice;
            boolean matchArea = post.getArea() >= minArea && post.getArea() <= maxArea;
            boolean matchAmenities = requiredAmenities.isEmpty()
                    || (post.getAmenities() != null && post.getAmenities().containsAll(requiredAmenities));

            if (matchKeyword && matchType && matchRoomType && matchDistrict && matchPrice && matchArea && matchAmenities) {
                filteredPosts.add(post);
            }
        }

        applySort(selectedSort);
        updateFilterSummary();
        binding.txtEmptyPosts.setVisibility(filteredPosts.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private void applySort(String selectedSort) {
        if (SORT_PRICE_ASC.equals(selectedSort)) {
            filteredPosts.sort(Comparator.comparingDouble(Post::getPrice));
            return;
        }

        if (SORT_PRICE_DESC.equals(selectedSort)) {
            filteredPosts.sort((left, right) -> Double.compare(right.getPrice(), left.getPrice()));
            return;
        }

        if (SORT_DISTANCE.equals(selectedSort)) {
            if (userLat == 0d && userLng == 0d) {
                requestLocationForDistanceSort();
                return;
            }
            filteredPosts.sort(Comparator.comparingDouble(post ->
                    distanceKm(userLat, userLng, post.getLat(), post.getLng())));
            return;
        }

        filteredPosts.sort((left, right) -> compareByCreatedAt(left.getCreatedAt(), right.getCreatedAt()));
    }

    private int compareByCreatedAt(Timestamp leftTs, Timestamp rightTs) {
        if (rightTs == null && leftTs == null) {
            return 0;
        }
        if (rightTs == null) {
            return -1;
        }
        if (leftTs == null) {
            return 1;
        }
        return rightTs.compareTo(leftTs);
    }

    private void requestLocationForDistanceSort() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation(this::applyFilters);
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Quyền vị trí")
                .setMessage("Ứng dụng cần quyền vị trí để sắp xếp bài đăng theo khoảng cách gần nhất.")
                .setPositiveButton("Tiếp tục", (dialog, which) ->
                        requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void fetchCurrentLocation(Runnable onReady) {
        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        applyCurrentLocation(location, onReady);
                        return;
                    }
                    requestFreshLocation(onReady);
                })
                .addOnFailureListener(e -> requestFreshLocation(onReady));
    }

    private void requestFreshLocation(Runnable onReady) {
        locationTokenSource = new CancellationTokenSource();
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, locationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        fallbackToNewestSort("Không lấy được vị trí hiện tại để sắp xếp theo khoảng cách.");
                        return;
                    }
                    applyCurrentLocation(location, onReady);
                })
                .addOnFailureListener(e ->
                        fallbackToNewestSort("Không lấy được vị trí hiện tại để sắp xếp theo khoảng cách."));
    }

    private void applyCurrentLocation(Location location, Runnable onReady) {
        userLat = location.getLatitude();
        userLng = location.getLongitude();
        onReady.run();
    }

    private void fallbackToNewestSort(String message) {
        if (binding == null) {
            return;
        }
        binding.spinnerSort.setSelection(0);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        applyFilters();
    }

    private void updateFilterSummary() {
        if (binding == null) {
            return;
        }

        List<String> chips = new ArrayList<>();
        String keyword = binding.edtSearch.getText().toString().trim();
        String district = binding.edtDistrict.getText().toString().trim();
        double minPrice = parseDoubleOrDefault(binding.edtMinPrice.getText().toString().trim(), 0d);
        double maxPrice = parseDoubleOrDefault(binding.edtMaxPrice.getText().toString().trim(), Double.MAX_VALUE);
        double minArea = parseDoubleOrDefault(binding.edtMinArea.getText().toString().trim(), 0d);
        double maxArea = parseDoubleOrDefault(binding.edtMaxArea.getText().toString().trim(), Double.MAX_VALUE);

        if (!keyword.isEmpty()) {
            chips.add("Từ khóa: " + keyword);
        }
        if (!TYPE_ALL.equals(String.valueOf(binding.spinnerType.getSelectedItem()))) {
            chips.add(String.valueOf(binding.spinnerType.getSelectedItem()));
        }
        if (!ROOM_TYPE_ALL.equals(String.valueOf(binding.spinnerRoomType.getSelectedItem()))) {
            chips.add(String.valueOf(binding.spinnerRoomType.getSelectedItem()));
        }
        if (minPrice > 0d || maxPrice < Double.MAX_VALUE) {
            chips.add("Giá");
        }
        if (minArea > 0d || maxArea < Double.MAX_VALUE) {
            chips.add("Diện tích");
        }
        if (!district.isEmpty()) {
            chips.add("Khu vực: " + district);
        }
        List<String> amenities = collectRequiredAmenities();
        if (!amenities.isEmpty()) {
            chips.add(TextUtils.join(", ", amenities));
        }
        String sort = String.valueOf(binding.spinnerSort.getSelectedItem());
        if (!SORT_NEWEST.equals(sort)) {
            chips.add("Sắp xếp: " + sort);
        }

        binding.txtFilterSummary.setText(
                chips.isEmpty()
                        ? "Đang hiển thị tất cả bài đăng"
                        : "Lọc theo: " + TextUtils.join(" • ", chips)
        );
    }

    private double parseDoubleOrDefault(String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private List<String> collectRequiredAmenities() {
        List<String> amenities = new ArrayList<>();
        if (binding.cbFilterWifi.isChecked()) {
            amenities.add("Wi-Fi");
        }
        if (binding.cbFilterParking.isChecked()) {
            amenities.add("Chỗ để xe");
        }
        if (binding.cbFilterPrivateWc.isChecked()) {
            amenities.add("WC riêng");
        }
        if (binding.cbFilterAirConditioner.isChecked()) {
            amenities.add("Máy lạnh");
        }
        return amenities;
    }

    private void resetFilters() {
        binding.edtSearch.setText("");
        binding.edtMinPrice.setText("");
        binding.edtMaxPrice.setText("");
        binding.edtMinArea.setText("");
        binding.edtMaxArea.setText("");
        binding.edtDistrict.setText("");
        binding.cbFilterWifi.setChecked(false);
        binding.cbFilterParking.setChecked(false);
        binding.cbFilterPrivateWc.setChecked(false);
        binding.cbFilterAirConditioner.setChecked(false);
        binding.spinnerType.setSelection(0);
        binding.spinnerRoomType.setSelection(0);
        binding.spinnerSort.setSelection(0);
        applyFilters();
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        if (lat2 == 0d && lon2 == 0d) {
            return Double.MAX_VALUE;
        }

        double earthRadius = 6371d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2d) * Math.sin(dLat / 2d)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2d) * Math.sin(dLon / 2d);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return earthRadius * c;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationTokenSource != null) {
            locationTokenSource.cancel();
        }
        binding = null;
    }
}
