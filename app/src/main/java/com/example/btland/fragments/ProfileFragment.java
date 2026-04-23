package com.example.btland.fragments;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.btland.activities.CreatePostActivity;
import com.example.btland.activities.EditProfileActivity;
import com.example.btland.activities.MyPostsActivity;
import com.example.btland.activities.WelcomeActivity;
import com.example.btland.databinding.FragmentProfileBinding;
import com.example.btland.utils.ThemePreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private boolean syncingThemeControls;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        binding.btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        binding.btnCreatePost.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreatePostActivity.class)));

        binding.btnMyPosts.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MyPostsActivity.class)));

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        setupThemeControls();
        loadProfile();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        syncThemeControls();
    }

    private void loadProfile() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = documentSnapshot.getString("name");
                    String email = documentSnapshot.getString("email");
                    String phone = documentSnapshot.getString("phone");
                    String avatarUrl = documentSnapshot.getString("avatarUrl");
                    boolean isAdmin = Boolean.TRUE.equals(documentSnapshot.getBoolean("isAdmin"));
                    boolean isBanned = Boolean.TRUE.equals(documentSnapshot.getBoolean("isBanned"));

                    binding.txtName.setText(name != null && !name.isEmpty() ? name : "Chưa có tên");
                    binding.txtEmail.setText(email != null && !email.isEmpty() ? email : "Chưa có email");
                    binding.txtPhone.setText(phone != null && !phone.isEmpty() ? phone : "Chưa có số điện thoại");
                    binding.txtRole.setText(isAdmin ? "Vai trò: Quản trị viên" : "Vai trò: Người dùng");

                    binding.btnCreatePost.setEnabled(!isBanned);
                    binding.btnCreatePost.setAlpha(isBanned ? 0.5f : 1f);

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this)
                                .load(avatarUrl)
                                .circleCrop()
                                .into(binding.imgAvatar);
                    } else {
                        binding.imgAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
                });
    }

    private void setupThemeControls() {
        SensorManager sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        binding.switchAutoTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (syncingThemeControls) {
                return;
            }

            if (isChecked && lightSensor == null) {
                syncingThemeControls = true;
                binding.switchAutoTheme.setChecked(false);
                syncingThemeControls = false;
                Toast.makeText(getContext(), "Thiết bị không có cảm biến ánh sáng", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean currentAutoTheme = ThemePreferences.isAutoThemeEnabled(requireContext());
            if (currentAutoTheme == isChecked) {
                return;
            }

            ThemePreferences.setAutoThemeEnabled(requireContext(), isChecked);
            syncThemeControls();

            if (!isChecked) {
                ThemePreferences.setManualNightMode(
                        requireContext(),
                        ThemePreferences.getManualNightMode(requireContext())
                );
            } else {
                ThemePreferences.applySavedNightMode(requireContext());
            }
            requireActivity().recreate();
        });

        binding.btnLightMode.setOnClickListener(v -> {
            ThemePreferences.setManualNightMode(requireContext(), AppCompatDelegate.MODE_NIGHT_NO);
            requireActivity().recreate();
        });

        binding.btnDarkMode.setOnClickListener(v -> {
            ThemePreferences.setManualNightMode(requireContext(), AppCompatDelegate.MODE_NIGHT_YES);
            requireActivity().recreate();
        });

        if (lightSensor == null) {
            binding.txtThemeHint.setText("Thiết bị này không có cảm biến ánh sáng. Bạn vẫn có thể chọn giao diện sáng hoặc tối thủ công.");
        }
        syncThemeControls();
    }

    private void syncThemeControls() {
        boolean autoTheme = ThemePreferences.isAutoThemeEnabled(requireContext());
        syncingThemeControls = true;
        binding.switchAutoTheme.setChecked(autoTheme);
        syncingThemeControls = false;
        binding.btnLightMode.setEnabled(!autoTheme);
        binding.btnDarkMode.setEnabled(!autoTheme);
        binding.btnLightMode.setAlpha(autoTheme ? 0.5f : 1f);
        binding.btnDarkMode.setAlpha(autoTheme ? 0.5f : 1f);
    }
}
