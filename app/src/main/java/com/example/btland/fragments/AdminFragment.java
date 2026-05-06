package com.example.btland.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.btland.utils.AdminUserActions;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminFragment extends Fragment {

    private FragmentAdminBinding binding;
    private final List<Post> allPosts = new ArrayList<>();
    private final List<Post> visiblePosts = new ArrayList<>();
    private final List<User> allUsers = new ArrayList<>();
    private final List<User> visibleUsers = new ArrayList<>();
    private AdminPostAdapter postAdapter;
    private UserAdminAdapter userAdapter;
    private FirebaseFirestore db;
    private int selectedTabIndex;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();

        postAdapter = new AdminPostAdapter(visiblePosts, true);
        userAdapter = new UserAdminAdapter(visibleUsers, this::setUserBanned);

        binding.recyclerAdminPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerAdminPosts.setAdapter(postAdapter);
        binding.recyclerAdminUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerAdminUsers.setAdapter(userAdapter);

        binding.edtAdminSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                applyCurrentSearch();
            }
        });

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
        selectedTabIndex = index;
        boolean showPosts = index == 0;
        binding.recyclerAdminPosts.setVisibility(showPosts ? View.VISIBLE : View.GONE);
        binding.recyclerAdminUsers.setVisibility(showPosts ? View.GONE : View.VISIBLE);
        binding.edtAdminSearch.setHint(showPosts
                ? "Tìm bài theo tiêu đề, địa chỉ, chủ bài"
                : "Tìm tài khoản theo tên, email, số điện thoại");
        applyCurrentSearch();
    }

    private void checkAdminAndLoad() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            showBlockedState("Bạn chưa đăng nhập.");
            return;
        }

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean isAdmin = Boolean.TRUE.equals(documentSnapshot.getBoolean("isAdmin"));
                    if (!isAdmin) {
                        showBlockedState("Tài khoản này không có quyền quản trị.");
                        return;
                    }

                    binding.txtAdminState.setVisibility(View.GONE);
                    binding.tabLayoutAdmin.setVisibility(View.VISIBLE);
                    binding.edtAdminSearch.setVisibility(View.VISIBLE);
                    loadPosts();
                    loadUsers();
                })
                .addOnFailureListener(e -> {
                    showBlockedState("Không tải được quyền quản trị.");
                    Toast.makeText(getContext(), "Không kiểm tra được quyền admin", Toast.LENGTH_SHORT).show();
                });
    }

    private void showBlockedState(String message) {
        if (binding == null) {
            return;
        }
        binding.txtAdminState.setVisibility(View.VISIBLE);
        binding.txtAdminState.setText(message);
        binding.tabLayoutAdmin.setVisibility(View.GONE);
        binding.edtAdminSearch.setVisibility(View.GONE);
        binding.recyclerAdminPosts.setVisibility(View.GONE);
        binding.recyclerAdminUsers.setVisibility(View.GONE);
    }

    private void loadPosts() {
        db.collection("posts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allPosts.clear();
                    for (var doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            allPosts.add(post);
                        }
                    }
                    applyPostSearch();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Không tải được danh sách bài đăng", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allUsers.clear();
                    for (var doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            allUsers.add(user);
                        }
                    }
                    applyUserSearch();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Không tải được danh sách người dùng", Toast.LENGTH_SHORT).show()
                );
    }

    private void applyCurrentSearch() {
        if (binding == null) {
            return;
        }
        if (selectedTabIndex == 0) {
            applyPostSearch();
        } else {
            applyUserSearch();
        }
    }

    private void applyPostSearch() {
        if (binding == null) {
            return;
        }
        String keyword = binding.edtAdminSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        visiblePosts.clear();
        for (Post post : allPosts) {
            if (keyword.isEmpty() || matchesPost(post, keyword)) {
                visiblePosts.add(post);
            }
        }
        postAdapter.notifyDataSetChanged();
    }

    private void applyUserSearch() {
        if (binding == null) {
            return;
        }
        String keyword = binding.edtAdminSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        visibleUsers.clear();
        for (User user : allUsers) {
            if (keyword.isEmpty() || matchesUser(user, keyword)) {
                visibleUsers.add(user);
            }
        }
        userAdapter.notifyDataSetChanged();
    }

    private boolean matchesPost(Post post, String keyword) {
        return contains(post.getTitle(), keyword)
                || contains(post.getDescription(), keyword)
                || contains(post.getAddress(), keyword)
                || contains(post.getDistrict(), keyword)
                || contains(post.getRoomType(), keyword)
                || contains(post.getOwnerName(), keyword)
                || contains(post.getOwnerPhone(), keyword)
                || contains(post.getPostId(), keyword)
                || contains(post.getStatus(), keyword)
                || (post.isAdminHiddenByBan() && "khóa".contains(keyword));
    }

    private boolean matchesUser(User user, String keyword) {
        return contains(user.getName(), keyword)
                || contains(user.getEmail(), keyword)
                || contains(user.getPhone(), keyword)
                || contains(user.getUserId(), keyword)
                || (user.isBanned() && "khóa".contains(keyword))
                || (!user.isBanned() && "hoạt động".contains(keyword));
    }

    private void setUserBanned(User user, boolean banned, int position) {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (user.getUserId() != null && user.getUserId().equals(currentUid)) {
            Toast.makeText(getContext(), "Không thể khóa tài khoản admin đang đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        AdminUserActions.setUserBanned(user.getUserId(), banned, new AdminUserActions.ActionCallback() {
            @Override
            public void onSuccess() {
                user.setBanned(banned);
                applyUserSearch();
                loadPosts();
                Toast.makeText(getContext(),
                        banned ? "Đã khóa tài khoản và tạm ẩn bài đăng" : "Đã mở khóa tài khoản và khôi phục bài đăng",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getContext(),
                        "Không cập nhật được tài khoản: " + errorMessage,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
