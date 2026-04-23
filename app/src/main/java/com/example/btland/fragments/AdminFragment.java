package com.example.btland.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.btland.adapters.AdminPostAdapter;
import com.example.btland.adapters.UserAdminAdapter;
import com.example.btland.databinding.FragmentAdminBinding;
import com.example.btland.models.Post;
import com.example.btland.models.User;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminFragment extends Fragment {

    private FragmentAdminBinding binding;
    private final List<Post> postList = new ArrayList<>();
    private final List<User> userList = new ArrayList<>();
    private AdminPostAdapter postAdapter;
    private UserAdminAdapter userAdapter;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();

        postAdapter = new AdminPostAdapter(postList, true);
        userAdapter = new UserAdminAdapter(userList);

        binding.recyclerAdminPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerAdminPosts.setAdapter(postAdapter);
        binding.recyclerAdminUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerAdminUsers.setAdapter(userAdapter);

        binding.tabLayoutAdmin.addTab(binding.tabLayoutAdmin.newTab().setText("Quản lý bài đăng"));
        binding.tabLayoutAdmin.addTab(binding.tabLayoutAdmin.newTab().setText("Quản lý người dùng"));
        binding.tabLayoutAdmin.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateTab(tab == null ? 0 : tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                updateTab(tab == null ? 0 : tab.getPosition());
            }
        });
        updateTab(0);

        checkAdminAndLoad();
        return binding.getRoot();
    }

    private void updateTab(int index) {
        boolean showPosts = index == 0;
        binding.recyclerAdminPosts.setVisibility(showPosts ? View.VISIBLE : View.GONE);
        binding.recyclerAdminUsers.setVisibility(showPosts ? View.GONE : View.VISIBLE);
    }

    private void checkAdminAndLoad() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            binding.txtAdminState.setVisibility(View.VISIBLE);
            binding.txtAdminState.setText("Bạn chưa đăng nhập.");
            binding.tabLayoutAdmin.setVisibility(View.GONE);
            return;
        }

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean isAdmin = Boolean.TRUE.equals(documentSnapshot.getBoolean("isAdmin"));
                    if (!isAdmin) {
                        binding.txtAdminState.setVisibility(View.VISIBLE);
                        binding.txtAdminState.setText("Tài khoản này không có quyền quản trị.");
                        binding.tabLayoutAdmin.setVisibility(View.GONE);
                        binding.recyclerAdminPosts.setVisibility(View.GONE);
                        binding.recyclerAdminUsers.setVisibility(View.GONE);
                        return;
                    }

                    binding.txtAdminState.setVisibility(View.GONE);
                    loadPosts();
                    loadUsers();
                })
                .addOnFailureListener(e -> {
                    binding.txtAdminState.setVisibility(View.VISIBLE);
                    binding.txtAdminState.setText("Không tải được quyền quản trị.");
                    Toast.makeText(getContext(), "Không kiểm tra được quyền admin", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPosts() {
        db.collection("posts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();
                    for (var doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            postList.add(post);
                        }
                    }
                    postAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Không tải được danh sách bài đăng", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (var doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            userList.add(user);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Không tải được danh sách người dùng", Toast.LENGTH_SHORT).show()
                );
    }
}
