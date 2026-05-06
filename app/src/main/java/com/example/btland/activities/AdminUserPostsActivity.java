package com.example.btland.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.btland.adapters.AdminPostAdapter;
import com.example.btland.databinding.ActivityAdminUserPostsBinding;
import com.example.btland.models.Post;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminUserPostsActivity extends AppCompatActivity {

    private ActivityAdminUserPostsBinding binding;
    private final List<Post> allPosts = new ArrayList<>();
    private final List<Post> visiblePosts = new ArrayList<>();
    private AdminPostAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminUserPostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        adapter = new AdminPostAdapter(visiblePosts);

        binding.recyclerAdminPosts.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerAdminPosts.setAdapter(adapter);
        binding.edtSearchPosts.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                applyPostSearch();
            }
        });

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
                        Toast.makeText(this, "Không tải được bài đăng", Toast.LENGTH_SHORT).show()
                );
    }

    private void applyPostSearch() {
        String keyword = binding.edtSearchPosts.getText().toString().trim().toLowerCase(Locale.ROOT);
        visiblePosts.clear();
        for (Post post : allPosts) {
            if (keyword.isEmpty() || matchesPost(post, keyword)) {
                visiblePosts.add(post);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private boolean matchesPost(Post post, String keyword) {
        return contains(post.getTitle(), keyword)
                || contains(post.getDescription(), keyword)
                || contains(post.getAddress(), keyword)
                || contains(post.getDistrict(), keyword)
                || contains(post.getRoomType(), keyword)
                || contains(post.getOwnerName(), keyword)
                || contains(post.getPostId(), keyword)
                || contains(post.getStatus(), keyword)
                || (post.isAdminHiddenByBan() && "khóa".contains(keyword));
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
