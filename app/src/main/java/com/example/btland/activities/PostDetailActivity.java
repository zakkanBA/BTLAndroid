package com.example.btland.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.btland.databinding.ActivityPostDetailBinding;
import com.example.btland.models.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

public class PostDetailActivity extends AppCompatActivity {

    private ActivityPostDetailBinding binding;
    private Post post;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        String postJson = getIntent().getStringExtra("post_json");
        if (postJson == null || postJson.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu bài đăng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        post = new Gson().fromJson(postJson, Post.class);
        bindData();
        setupActions();
    }

    private void bindData() {
        binding.txtTitle.setText(post.getTitle());
        binding.txtPrice.setText(((int) post.getPrice()) + " VNĐ");
        binding.txtArea.setText(post.getArea() + " m²");
        binding.txtAddress.setText(post.getAddress());
        binding.txtType.setText("rent".equals(post.getType()) ? "Cho thuê" : "Ở ghép");
        binding.txtDescription.setText(post.getDescription());

        if (post.getImages() != null && !post.getImages().isEmpty()) {
            Glide.with(this)
                    .load(post.getImages().get(0))
                    .centerCrop()
                    .into(binding.imgPost);
        }

        boolean isOwner = currentUserId != null && currentUserId.equals(post.getUserId());
        binding.btnEditPost.setVisibility(isOwner ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnDeletePost.setVisibility(isOwner ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnChat.setVisibility(isOwner ? android.view.View.GONE : android.view.View.VISIBLE);

        String panoramaUrl = post.getPanoramaImage();
        if (panoramaUrl != null && !panoramaUrl.isEmpty()) {
            binding.btnView360.setVisibility(android.view.View.VISIBLE);
        } else {
            binding.btnView360.setVisibility(android.view.View.GONE);
        }
    }

    private void setupActions() {
        binding.btnView360.setOnClickListener(v -> {
            Intent intent = new Intent(this, PanoramaViewActivity.class);
            intent.putExtra("panorama_url", post.getPanoramaImage());
            startActivity(intent);
        });

        binding.btnChat.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("receiverId", post.getUserId());
            intent.putExtra("postId", post.getPostId());
            startActivity(intent);
        });

        binding.btnEditPost.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditPostActivity.class);
            intent.putExtra("post_json", new Gson().toJson(post));
            startActivity(intent);
        });

        binding.btnDeletePost.setOnClickListener(v -> showDeleteConfirm());
    }

    private void showDeleteConfirm() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bài đăng")
                .setMessage("Bạn có chắc muốn xóa bài đăng này không?")
                .setPositiveButton("Xóa", (dialog, which) -> deletePost())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deletePost() {
        db.collection("posts")
                .document(post.getPostId())
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Xóa bài thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Xóa bài thất bại", Toast.LENGTH_SHORT).show()
                );
    }
}