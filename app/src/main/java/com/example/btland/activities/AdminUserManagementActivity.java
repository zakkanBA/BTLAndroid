package com.example.btland.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.btland.adapters.UserAdminAdapter;
import com.example.btland.databinding.ActivityAdminUserManagementBinding;
import com.example.btland.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminUserManagementActivity extends AppCompatActivity {

    private ActivityAdminUserManagementBinding binding;
    private final List<User> userList = new ArrayList<>();
    private UserAdminAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminUserManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        adapter = new UserAdminAdapter(userList);

        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerUsers.setAdapter(adapter);

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
                    userList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            userList.add(user);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không tải được danh sách user", Toast.LENGTH_SHORT).show()
                );
    }
}