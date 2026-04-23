package com.example.btland.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.btland.databinding.ItemMediaPreviewBinding;

import java.util.ArrayList;
import java.util.List;

public class MediaPreviewAdapter extends RecyclerView.Adapter<MediaPreviewAdapter.MediaViewHolder> {

    private final List<String> imageSources = new ArrayList<>();

    public void submitItems(List<String> items) {
        imageSources.clear();
        if (items != null) {
            imageSources.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMediaPreviewBinding binding = ItemMediaPreviewBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new MediaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        String source = imageSources.get(position);
        Glide.with(holder.itemView.getContext())
                .load(source)
                .centerCrop()
                .into(holder.binding.imgPreview);
    }

    @Override
    public int getItemCount() {
        return imageSources.size();
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        final ItemMediaPreviewBinding binding;

        MediaViewHolder(ItemMediaPreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
