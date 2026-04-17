package com.example.btland.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.btland.R;
import com.example.btland.models.Post;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class MapFragment extends Fragment {

    private MapView mapView;
    private FirebaseFirestore db;

    private FusedLocationProviderClient locationClient;
    private double userLat = 0;
    private double userLng = 0;

    private ActivityResultLauncher<String> permissionLauncher;

    public MapFragment() {
        super(R.layout.fragment_map);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) getCurrentLocation();
                    else Toast.makeText(getContext(), "Không có quyền GPS", Toast.LENGTH_SHORT).show();
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Configuration.getInstance().load(
                requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext())
        );
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        mapView = view.findViewById(R.id.osmMap);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        locationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        requestLocationPermission();
    }

    // 🔴 XIN QUYỀN GPS
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            getCurrentLocation();

        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    // 🔴 LẤY VỊ TRÍ HIỆN TẠI
    private void getCurrentLocation() {
        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();

                        GeoPoint userPoint = new GeoPoint(userLat, userLng);
                        mapView.getController().setZoom(13.0);
                        mapView.getController().setCenter(userPoint);

                        addUserMarker(userPoint);
                        loadNearbyPosts();
                    } else {
                        Toast.makeText(getContext(), "Không lấy được vị trí", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 🔵 MARKER USER
    private void addUserMarker(GeoPoint point) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle("Bạn đang ở đây");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    // 🔴 LOAD BÀI GẦN ĐÂY
    private void loadNearbyPosts() {
        db.collection("posts")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (var doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        if (post == null) continue;

                        double lat = post.getLat();
                        double lng = post.getLng();

                        if (lat == 0 || lng == 0) continue;

                        double distance = distanceKm(userLat, userLng, lat, lng);

                        // 🔥 chỉ lấy trong bán kính 5km
                        if (distance <= 5) {
                            addPostMarker(post, lat, lng, distance);
                        }
                    }

                    mapView.invalidate();
                });
    }

    // 🔵 MARKER BÀI ĐĂNG
    private void addPostMarker(Post post, double lat, double lng, double distance) {
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(lat, lng));
        marker.setTitle(post.getTitle());
        marker.setSubDescription(((int) post.getPrice()) + " VNĐ - " + String.format("%.1f km", distance));

        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        mapView.getOverlays().add(marker);
    }

    // 🔥 TÍNH KHOẢNG CÁCH (HAVERSINE)
    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // bán kính trái đất (km)

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}