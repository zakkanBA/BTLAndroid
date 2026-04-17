package com.example.btland.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.example.btland.activities.ChatActivity;
import com.example.btland.databinding.ItemConversationBinding;
import com.example.btland.models.Conversation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private List<Conversation> list;
    private String currentUserId;

    public ConversationAdapter(List<Conversation> list) {
        this.list = list;
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemConversationBinding binding = ItemConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Conversation conv = list.get(position);
        String tempId = "";
        for (String id : conv.getUserIds()) {
            if (!id.equals(currentUserId)) {
                tempId = id;
                break;
            }
        }

        final String otherUserId = tempId; // ✅ fix

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(otherUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    holder.binding.txtUser.setText(
                            name != null ? name : "Người dùng"
                    );
                });        holder.binding.txtLastMessage.setText(conv.getLastMessage());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ChatActivity.class);
            intent.putExtra("receiverId", otherUserId);
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemConversationBinding binding;

        public ViewHolder(ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}