package com.example.btland.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.btland.activities.PostDetailActivity;
import com.example.btland.databinding.FragmentMapBinding;
import com.example.btland.models.Post;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final LatLng DEFAULT_CENTER = new LatLng(10.7769, 106.7009);
    private static final float DEFAULT_ZOOM = 12.5f;
    private static final float USER_ZOOM = 14f;
    private static final double NEARBY_RADIUS_KM = 3d;

    private FragmentMapBinding binding;
    private GoogleMap googleMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient locationClient;
    private ActivityResultLauncher<String> permissionLauncher;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private CancellationTokenSource locationTokenSource;
    private Runnable mapTimeoutRunnable;
    private double userLat;
    private double userLng;
    private boolean mapLoaded;

    public MapFragment() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        locationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        requestCurrentLocation(true);
                    } else {
                        updateStatus("Chưa có quyền vị trí. Đang hiển thị tất cả bài đăng có tọa độ.");
                        loadPosts(null);
                    }
                });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.mapView.onCreate(savedInstanceState);
        binding.mapView.getMapAsync(this);
        binding.btnLocate.setOnClickListener(v -> requestLocationPermission(true));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM));
        googleMap.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Post) {
                Intent intent = new Intent(requireContext(), PostDetailActivity.class);
                intent.putExtra("post_json", new Gson().toJson(tag));
                startActivity(intent);
            }
        });

        updateStatus("Đang tải dữ liệu bản đồ...");
        monitorMapTiles();
        loadPosts(null);
        requestLocationPermission(false);
    }

    private void monitorMapTiles() {
        if (googleMap == null) {
            return;
        }

        mapLoaded = false;
        googleMap.setOnMapLoadedCallback(() -> {
            mapLoaded = true;
            if (mapTimeoutRunnable != null) {
                uiHandler.removeCallbacks(mapTimeoutRunnable);
            }
        });

        if (mapTimeoutRunnable != null) {
            uiHandler.removeCallbacks(mapTimeoutRunnable);
        }

        mapTimeoutRunnable = () -> {
            if (!mapLoaded && binding != null) {
                binding.txtMapStatus.setText(
                        "Nếu bản đồ vẫn trắng, hãy bật Maps SDK for Android, Billing và kiểm tra lại API key."
                );
                binding.txtMapStatus.setVisibility(View.VISIBLE);
            }
        };
        uiHandler.postDelayed(mapTimeoutRunnable, 8000L);
    }

    private void requestLocationPermission(boolean fromLocateButton) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            requestCurrentLocation(fromLocateButton);
            return;
        }

        if (!fromLocateButton) {
            updateStatus("Cho phép vị trí để xem bài gần bạn; nếu chưa cấp quyền app vẫn hiển thị toàn bộ bài có GPS.");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Quyền vị trí")
                .setMessage("Ứng dụng cần quyền vị trí để ưu tiên các bài đăng trong bán kính 3km quanh bạn.")
                .setPositiveButton("Tiếp tục", (dialog, which) ->
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void requestCurrentLocation(boolean moveToUser) {
        updateStatus("Đang lấy GPS hiện tại...");
        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        onLocationReady(location, moveToUser);
                        return;
                    }
                    requestFreshLocation(moveToUser);
                })
                .addOnFailureListener(e -> requestFreshLocation(moveToUser));
    }

    private void requestFreshLocation(boolean moveToUser) {
        locationTokenSource = new CancellationTokenSource();
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, locationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        updateStatus("Không lấy được GPS. Đang hiển thị tất cả bài đăng có tọa độ.");
                        loadPosts(null);
                        return;
                    }
                    onLocationReady(location, moveToUser);
                })
                .addOnFailureListener(e -> {
                    updateStatus("Không lấy được GPS. Đang hiển thị tất cả bài đăng có tọa độ.");
                    loadPosts(null);
                });
    }

    private void onLocationReady(Location location, boolean moveToUser) {
        userLat = location.getLatitude();
        userLng = location.getLongitude();

        if (googleMap != null
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        if (moveToUser && googleMap != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userLat, userLng), USER_ZOOM));
        }
        loadPosts(location);
    }

    private void loadPosts(@Nullable Location userLocation) {
        if (googleMap == null) {
            return;
        }

        db.collection("posts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> allGpsPosts = new ArrayList<>();
                    List<Post> nearbyPosts = new ArrayList<>();

                    for (var doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        if (post == null || !post.isActive() || !hasValidCoordinates(post)) {
                            continue;
                        }

                        allGpsPosts.add(post);
                        if (userLocation != null) {
                            double distance = distanceKm(
                                    userLocation.getLatitude(),
                                    userLocation.getLongitude(),
                                    post.getLat(),
                                    post.getLng()
                            );
                            if (distance <= NEARBY_RADIUS_KM) {
                                nearbyPosts.add(post);
                            }
                        }
                    }

                    if (userLocation != null && !nearbyPosts.isEmpty()) {
                        updateStatus(String.format(
                                Locale.getDefault(),
                                "Đang hiển thị %d bài trong bán kính %.0fkm quanh bạn.",
                                nearbyPosts.size(),
                                NEARBY_RADIUS_KM
                        ));
                        renderPosts(nearbyPosts, userLocation);
                        return;
                    }

                    if (userLocation != null && !allGpsPosts.isEmpty()) {
                        updateStatus("Không có bài trong bán kính 3km. Đang mở rộng ra toàn bộ bài có tọa độ.");
                        renderPosts(allGpsPosts, userLocation);
                        return;
                    }

                    if (!allGpsPosts.isEmpty()) {
                        updateStatus("Đang hiển thị toàn bộ bài đăng đã có tọa độ GPS.");
                        renderPosts(allGpsPosts, null);
                        return;
                    }

                    googleMap.clear();
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM));
                    updateStatus("Chưa có bài đăng nào được gắn tọa độ GPS để hiển thị trên bản đồ.");
                })
                .addOnFailureListener(e -> updateStatus("Không tải được dữ liệu bản đồ từ Firebase."));
    }

    private boolean hasValidCoordinates(Post post) {
        return post.getLat() >= -90d
                && post.getLat() <= 90d
                && post.getLng() >= -180d
                && post.getLng() <= 180d
                && !(post.getLat() == 0d && post.getLng() == 0d);
    }

    private void renderPosts(List<Post> posts, @Nullable Location userLocation) {
        googleMap.clear();

        List<LatLng> points = new ArrayList<>();
        if (userLocation != null) {
            points.add(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()));
        }

        for (Post post : posts) {
            double distance = userLocation == null
                    ? -1d
                    : distanceKm(userLocation.getLatitude(), userLocation.getLongitude(), post.getLat(), post.getLng());

            LatLng point = post.isRoommatePost()
                    ? addProtectedMarker(post, distance)
                    : addExactMarker(post, distance);
            points.add(point);
        }

        if (points.isEmpty()) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM));
            return;
        }

        if (points.size() == 1) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), userLocation == null ? DEFAULT_ZOOM : USER_ZOOM));
            return;
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            boundsBuilder.include(point);
        }
        binding.mapView.post(() ->
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 140)));
    }

    private LatLng addExactMarker(Post post, double distance) {
        LatLng position = new LatLng(post.getLat(), post.getLng());
        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(position)
                .title(post.getTitle())
                .snippet(buildExactSnippet(post, distance)));
        if (marker != null) {
            marker.setTag(post);
        }
        return position;
    }

    private String buildExactSnippet(Post post, double distance) {
        if (distance >= 0d) {
            return String.format(Locale.getDefault(), "%,.0f VNĐ - %.1f km", post.getPrice(), distance);
        }
        return String.format(Locale.getDefault(), "%,.0f VNĐ", post.getPrice());
    }

    private LatLng addProtectedMarker(Post post, double distance) {
        LatLng obfuscated = obfuscate(post);
        googleMap.addCircle(new CircleOptions()
                .center(obfuscated)
                .radius(300d)
                .strokeColor(0x88FF8A00)
                .fillColor(0x22FF8A00)
                .strokeWidth(3f));

        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(obfuscated)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(post.getTitle())
                .snippet(buildProtectedSnippet(distance)));
        if (marker != null) {
            marker.setTag(post);
        }
        return obfuscated;
    }

    private String buildProtectedSnippet(double distance) {
        if (distance >= 0d) {
            return String.format(Locale.getDefault(), "Khu vực gần đúng - %.1f km", distance);
        }
        return "Vị trí đã được làm mờ để bảo vệ quyền riêng tư.";
    }

    private LatLng obfuscate(Post post) {
        long seed = post.getPostId() == null ? 0L : post.getPostId().hashCode();
        Random random = new Random(seed);
        double radiusMeters = 200d + random.nextInt(301);
        double angle = Math.toRadians(random.nextInt(360));

        double offsetLat = (radiusMeters / 111_320d) * Math.cos(angle);
        double offsetLng = (radiusMeters / (111_320d * Math.cos(Math.toRadians(post.getLat())))) * Math.sin(angle);
        return new LatLng(post.getLat() + offsetLat, post.getLng() + offsetLng);
    }

    private void updateStatus(String message) {
        if (binding == null) {
            return;
        }
        binding.txtMapStatus.setText(message);
        binding.txtMapStatus.setVisibility(View.VISIBLE);
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
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
    public void onResume() {
        super.onResume();
        MapView mapView = binding == null ? null : binding.mapView;
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        MapView mapView = binding == null ? null : binding.mapView;
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationTokenSource != null) {
            locationTokenSource.cancel();
        }
        if (mapTimeoutRunnable != null) {
            uiHandler.removeCallbacks(mapTimeoutRunnable);
        }
        MapView mapView = binding == null ? null : binding.mapView;
        if (mapView != null) {
            mapView.onDestroy();
        }
        binding = null;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        MapView mapView = binding == null ? null : binding.mapView;
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }
}
