package com.example.btland.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.btland.activities.PostDetailActivity;
import com.example.btland.databinding.ItemPostBinding;
import com.example.btland.models.Post;
import com.google.gson.Gson;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final List<Post> postList;

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPostBinding binding = ItemPostBinding.inflate(
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

        if (post.getImages() != null && !post.getImages().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(post.getImages().get(0))
                    .centerCrop()
                    .into(holder.binding.imgPost);
        } else {
            holder.binding.imgPost.setImageDrawable(null);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), PostDetailActivity.class);
            intent.putExtra("post_json", new Gson().toJson(post));
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ItemPostBinding binding;

        public PostViewHolder(ItemPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}