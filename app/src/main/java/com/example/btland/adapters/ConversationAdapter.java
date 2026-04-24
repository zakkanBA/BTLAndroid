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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private final List<Conversation> list;
    private final String currentUserId;

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
        String conversationDocId = conv.getConversationId();
        String otherUserId = "";

        for (String id : conv.getUserIds()) {
            if (!id.equals(currentUserId)) {
                otherUserId = id;
                break;
            }
        }
        final String finalOtherUserId = otherUserId;

        Map<String, String> participantNames = conv.getParticipantNames();
        String cachedName = participantNames.get(finalOtherUserId);
        boolean hasRealCachedName = cachedName != null
                && !cachedName.trim().isEmpty()
                && !"Người dùng".equalsIgnoreCase(cachedName.trim());

        if (hasRealCachedName) {
            holder.binding.txtUser.setText(cachedName);
        } else {
            holder.itemView.setTag(finalOtherUserId);
            holder.binding.txtUser.setText("Đang tải tên...");
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(finalOtherUserId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!finalOtherUserId.equals(holder.itemView.getTag())) {
                            return;
                        }

                        String name = doc.getString("name");
                        String resolvedName = name != null && !name.trim().isEmpty()
                                ? name.trim()
                                : "Người dùng";
                        holder.binding.txtUser.setText(resolvedName);

                        if (!"Người dùng".equals(resolvedName)
                                && conversationDocId != null
                                && !conversationDocId.isEmpty()) {
                            FirebaseFirestore.getInstance()
                                    .collection("conversations")
                                    .document(conversationDocId)
                                    .update("participantNames." + finalOtherUserId, resolvedName);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (finalOtherUserId.equals(holder.itemView.getTag())) {
                            holder.binding.txtUser.setText("Người dùng");
                        }
                    });
        }

        holder.binding.txtLastMessage.setText(conv.getLastMessage());
        if (conv.getLastTimestamp() != null) {
            String time = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
                    .format(conv.getLastTimestamp().toDate());
            holder.binding.txtTime.setText(time);
        } else {
            holder.binding.txtTime.setText("");
        }

        long unreadCount = conv.getUnreadCounts().getOrDefault(currentUserId, 0L);
        holder.binding.txtUnreadBadge.setVisibility(
                unreadCount > 0 ? android.view.View.VISIBLE : android.view.View.GONE
        );
        holder.binding.txtUnreadBadge.setText(String.valueOf(unreadCount));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ChatActivity.class);
            intent.putExtra("receiverId", finalOtherUserId);
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemConversationBinding binding;

        ViewHolder(ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
