package com.example.btland.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.btland.adapters.UserAdminAdapter;
import com.example.btland.databinding.ActivityAdminUserManagementBinding;
import com.example.btland.models.User;
import com.example.btland.utils.AdminUserActions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminUserManagementActivity extends AppCompatActivity {

    private ActivityAdminUserManagementBinding binding;
    private final List<User> allUsers = new ArrayList<>();
    private final List<User> visibleUsers = new ArrayList<>();
    private UserAdminAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminUserManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        adapter = new UserAdminAdapter(visibleUsers, this::setUserBanned);

        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerUsers.setAdapter(adapter);
        binding.edtSearchUsers.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                applyUserSearch();
            }
        });

        checkAdminAndLoadUsers();
    }

    private void checkAdminAndLoadUsers() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            finish();
            return;
        }

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                    if (!Boolean.TRUE.equals(isAdmin)) {
                        Toast.makeText(this, "Bạn không có quyền admin", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    loadUsers();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Không kiểm tra được quyền admin", Toast.LENGTH_SHORT).show();
                    finish();
                });
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
                        Toast.makeText(this, "Không tải được danh sách user", Toast.LENGTH_SHORT).show()
                );
    }

    private void applyUserSearch() {
        String keyword = binding.edtSearchUsers.getText().toString().trim().toLowerCase(Locale.ROOT);
        visibleUsers.clear();
        for (User user : allUsers) {
            if (keyword.isEmpty() || matchesUser(user, keyword)) {
                visibleUsers.add(user);
            }
        }
        adapter.notifyDataSetChanged();
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
            Toast.makeText(this, "Không thể khóa tài khoản admin đang đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        AdminUserActions.setUserBanned(user.getUserId(), banned, new AdminUserActions.ActionCallback() {
            @Override
            public void onSuccess() {
                user.setBanned(banned);
                applyUserSearch();
                Toast.makeText(AdminUserManagementActivity.this,
                        banned ? "Đã khóa tài khoản và tạm ẩn bài đăng" : "Đã mở khóa tài khoản và khôi phục bài đăng",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AdminUserManagementActivity.this,
                        "Không cập nhật được tài khoản: " + errorMessage,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
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
