package com.example.btland.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.btland.activities.AdminUserManagementActivity;
import com.example.btland.activities.CreatePostActivity;
import com.example.btland.activities.EditProfileActivity;
import com.example.btland.activities.MyPostsActivity;
import com.example.btland.activities.WelcomeActivity;
import com.example.btland.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

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

        binding.btnAdmin.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AdminUserManagementActivity.class)));

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        loadProfile();
        loadAdminState();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        loadAdminState();
    }

    private void loadProfile() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = documentSnapshot.getString("name");
                    String email = documentSnapshot.getString("email");
                    String phone = documentSnapshot.getString("phone");

                    binding.txtName.setText(name != null && !name.isEmpty() ? name : "Chưa có tên");
                    binding.txtEmail.setText(email != null && !email.isEmpty() ? email : "Chưa có email");
                    binding.txtPhone.setText(phone != null && !phone.isEmpty() ? phone : "Chưa có số điện thoại");
                });
    }

    private void loadAdminState() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                    binding.btnAdmin.setVisibility(Boolean.TRUE.equals(isAdmin) ? View.VISIBLE : View.GONE);
                });
    }
}