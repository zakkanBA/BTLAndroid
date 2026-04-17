package com.example.btland.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.btland.adapters.AdminPostAdapter;
import com.example.btland.databinding.ActivityAdminUserPostsBinding;
import com.example.btland.models.Post;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminUserPostsActivity extends AppCompatActivity {

    private ActivityAdminUserPostsBinding binding;
    private final List<Post> postList = new ArrayList<>();
    private AdminPostAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminUserPostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        adapter = new AdminPostAdapter(postList);

        binding.recyclerAdminPosts.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerAdminPosts.setAdapter(adapter);

        String userId = getIntent().getStringExtra("userId");
        String userName = getIntent().getStringExtra("userName");

        if (userName != null) {
            setTitle("Bài của " + userName);
        }

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Thiếu userId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPosts(userId);
    }

    private void loadPosts(String userId) {
        db.collection("posts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            postList.add(post);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không tải được bài đăng", Toast.LENGTH_SHORT).show()
                );
    }
}