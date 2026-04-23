package com.example.btland.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.btland.adapters.MediaPreviewAdapter;
import com.example.btland.databinding.ActivityPostDetailBinding;
import com.example.btland.models.Post;
import com.example.btland.utils.PostRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.List;

public class PostDetailActivity extends AppCompatActivity {

    private ActivityPostDetailBinding binding;
    private Post post;
    private String currentUserId;
    private final MediaPreviewAdapter imageAdapter = new MediaPreviewAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = FirebaseAuth.getInstance().getUid();

        String postJson = getIntent().getStringExtra("post_json");
        if (postJson == null || postJson.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu bài đăng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        post = new Gson().fromJson(postJson, Post.class);
        binding.recyclerImages.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerImages.setAdapter(imageAdapter);

        bindData();
        refreshLatestPost();
        setupActions();
    }

    private void bindData() {
        binding.txtTitle.setText(post.getTitle());
        binding.txtPrice.setText(String.format(java.util.Locale.getDefault(), "%,.0f VNĐ/tháng", post.getPrice()));
        binding.txtArea.setText(String.format(java.util.Locale.getDefault(), "%.1f m²", post.getArea()));
        binding.txtAddress.setText(post.getAddress());
        binding.txtType.setText("rent".equals(post.getType()) ? "Cho thuê" : "Tìm người ở ghép");
        binding.txtRoomType.setText(post.getRoomType() == null ? "Chưa cập nhật loại phòng" : post.getRoomType());
        binding.txtDescription.setText(post.getDescription());
        binding.txtAmenities.setText(formatAmenities(post.getAmenities()));
        binding.txtOwner.setText((post.getOwnerName() == null || post.getOwnerName().isEmpty() ? "Chủ bài đăng" : post.getOwnerName())
                + (post.getOwnerPhone() == null || post.getOwnerPhone().isEmpty() ? "" : " - " + post.getOwnerPhone()));
        binding.txtLocationPrivacy.setVisibility(post.isRoommatePost() ? View.VISIBLE : View.GONE);
        binding.txtStatus.setText(post.isActive() ? "Đang hiển thị" : "Đã ẩn");

        List<String> images = post.getImages();
        imageAdapter.submitItems(images);
        if (images != null && !images.isEmpty()) {
            Glide.with(this)
                    .load(images.get(0))
                    .centerCrop()
                    .into(binding.imgPost);
        } else {
            binding.imgPost.setImageDrawable(null);
        }

        boolean isOwner = currentUserId != null && currentUserId.equals(post.getUserId());
        binding.btnEditPost.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        binding.btnDeletePost.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        binding.btnToggleVisibility.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        binding.btnChat.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        binding.btnToggleVisibility.setText(post.isActive() ? "Ẩn bài đăng" : "Hiện bài đăng");
        binding.btnView360.setVisibility(post.hasPanorama() ? View.VISIBLE : View.GONE);
    }

    private void refreshLatestPost() {
        if (post.getPostId() == null || post.getPostId().isEmpty()) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("posts")
                .document(post.getPostId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Post latestPost = documentSnapshot.toObject(Post.class);
                    if (latestPost != null) {
                        post = latestPost;
                        bindData();
                    }
                });
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
        binding.btnToggleVisibility.setOnClickListener(v -> toggleVisibility());
    }

    private void showDeleteConfirm() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bài đăng")
                .setMessage("Bạn có chắc muốn xóa bài đăng này không?")
                .setPositiveButton("Xóa", (dialog, which) ->
                        PostRepository.deletePost(this, post, false, new PostRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(PostDetailActivity.this, "Đã xóa bài đăng", Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(PostDetailActivity.this, "Xóa bài thất bại", Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void toggleVisibility() {
        PostRepository.setPostActive(post, !post.isActive(), new PostRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                post.setActive(!post.isActive());
                bindData();
                Toast.makeText(PostDetailActivity.this, "Đã cập nhật trạng thái bài đăng", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(PostDetailActivity.this, "Không cập nhật được trạng thái bài đăng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatAmenities(List<String> amenities) {
        if (amenities == null || amenities.isEmpty()) {
            return "Không có tiện ích nào được chọn";
        }
        return android.text.TextUtils.join(", ", amenities);
    }
}
