package com.example.btland.adapters;
//est
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.example.btland.R;
import com.example.btland.databinding.ItemMessageBinding;
import com.example.btland.models.Message;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private final List<Message> list;
    private final String currentUserId;

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
        if (msg.getTimestamp() != null) {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(msg.getTimestamp().toDate());
            holder.binding.txtTime.setText(time);
        } else {
            holder.binding.txtTime.setText("");
        }

        boolean isCurrentUser = msg.getSenderId() != null && msg.getSenderId().equals(currentUserId);
        holder.binding.txtReadStatus.setText(isCurrentUser ? (msg.isRead() ? "Đã đọc" : "Đã gửi") : "");
        holder.binding.txtReadStatus.setVisibility(isCurrentUser ? View.VISIBLE : View.GONE);

        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) holder.binding.container.getLayoutParams();
        if (isCurrentUser) {
            holder.binding.container.setBackgroundResource(R.drawable.bg_message_outgoing);
            params.gravity = Gravity.END;
        } else {
            holder.binding.container.setBackgroundResource(R.drawable.bg_message_incoming);
            params.gravity = Gravity.START;
        }
        holder.binding.container.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemMessageBinding binding;

        ViewHolder(ItemMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
