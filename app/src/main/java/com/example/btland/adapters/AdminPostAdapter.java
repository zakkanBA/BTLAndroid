package com.example.btland.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btland.databinding.ItemAdminPostBinding;
import com.example.btland.models.Post;
import com.example.btland.utils.PostRepository;

import java.util.List;
import java.util.Locale;

public class AdminPostAdapter extends RecyclerView.Adapter<AdminPostAdapter.PostViewHolder> {

    private final List<Post> postList;
    private final boolean adminDelete;

    public AdminPostAdapter(List<Post> postList) {
        this(postList, false);
    }

    public AdminPostAdapter(List<Post> postList, boolean adminDelete) {
        this.postList = postList;
        this.adminDelete = adminDelete;
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
        holder.binding.txtPrice.setText(String.format(Locale.getDefault(), "%,.0f VNĐ", post.getPrice()));
        holder.binding.txtAddress.setText(post.getAddress());

        holder.binding.btnDeletePost.setOnClickListener(v -> new AlertDialog.Builder(holder.itemView.getContext())
                .setTitle("Xóa bài đăng")
                .setMessage("Bạn có chắc muốn xóa bài đăng này?")
                .setPositiveButton("Xóa", (dialog, which) ->
                        PostRepository.deletePost(holder.itemView.getContext(), post, adminDelete, new PostRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                int currentPos = holder.getBindingAdapterPosition();
                                if (currentPos != RecyclerView.NO_POSITION) {
                                    postList.remove(currentPos);
                                    notifyItemRemoved(currentPos);
                                }
                                Toast.makeText(holder.itemView.getContext(), "Đã xóa bài đăng", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(holder.itemView.getContext(), "Xóa bài đăng thất bại", Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton("Hủy", null)
                .show());
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        final ItemAdminPostBinding binding;

        PostViewHolder(ItemAdminPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
