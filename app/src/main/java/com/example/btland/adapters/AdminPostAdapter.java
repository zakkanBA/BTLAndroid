package com.example.btland.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btland.databinding.ItemAdminPostBinding;
import com.example.btland.models.Post;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AdminPostAdapter extends RecyclerView.Adapter<AdminPostAdapter.PostViewHolder> {

    private final List<Post> postList;

    public AdminPostAdapter(List<Post> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminPostBinding binding = ItemAdminPostBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new PostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.binding.txtTitle.setText(post.getTitle());
        holder.binding.txtPrice.setText(((int) post.getPrice()) + " VNĐ");
        holder.binding.txtAddress.setText(post.getAddress());

        holder.binding.btnDeletePost.setOnClickListener(v -> {
            FirebaseFirestore.getInstance()
                    .collection("posts")
                    .document(post.getPostId())
                    .delete()
                    .addOnSuccessListener(unused -> {
                        int currentPos = holder.getBindingAdapterPosition();
                        if (currentPos != RecyclerView.NO_POSITION) {
                            postList.remove(currentPos);
                            notifyItemRemoved(currentPos);
                        }
                        Toast.makeText(holder.itemView.getContext(), "Đã xóa bài đăng", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ItemAdminPostBinding binding;

        public PostViewHolder(ItemAdminPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}