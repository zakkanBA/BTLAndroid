package com.example.btland.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.example.btland.databinding.ItemMessageBinding;
import com.example.btland.models.Message;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private List<Message> list;
    private String currentUserId;

    public MessageAdapter(List<Message> list, String currentUserId) {
        this.list = list;
        this.currentUserId = currentUserId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemMessageBinding binding = ItemMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Message msg = list.get(position);

        holder.binding.txtMessage.setText(msg.getContent());

        // format time
        if (msg.getTimestamp() != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
            String time = sdf.format(msg.getTimestamp().toDate());
            holder.binding.txtTime.setText(time);
        }

        // trái/phải
        if (msg.getSenderId().equals(currentUserId)) {
            // tin của mình → bên phải
            holder.binding.container.setBackgroundColor(0xFF27D3C5);

            FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams) holder.binding.container.getLayoutParams();
            params.gravity = android.view.Gravity.END;
            holder.binding.container.setLayoutParams(params);

        } else {
            // tin người khác → bên trái
            holder.binding.container.setBackgroundColor(0xFF444444);

            FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams) holder.binding.container.getLayoutParams();
            params.gravity = android.view.Gravity.START;
            holder.binding.container.setLayoutParams(params);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemMessageBinding binding;

        public ViewHolder(ItemMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}