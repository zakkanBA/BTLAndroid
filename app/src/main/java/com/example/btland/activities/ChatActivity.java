package com.example.btland.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.btland.adapters.MessageAdapter;
import com.example.btland.databinding.ActivityChatBinding;
import com.example.btland.models.Message;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;

    private String receiverId;
    private String currentUserId;
    private String conversationId;

    private FirebaseFirestore db;

    private List<Message> messageList;
    private MessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        receiverId = getIntent().getStringExtra("receiverId");
        currentUserId = FirebaseAuth.getInstance().getUid();
        db = FirebaseFirestore.getInstance();

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList, currentUserId);

        binding.recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerMessages.setAdapter(adapter);

        createConversationIfNeeded();

        binding.btnSend.setOnClickListener(v -> sendMessage());
    }

    private void createConversationIfNeeded() {
        conversationId = currentUserId + "_" + receiverId;

        loadMessages();
    }

    private void loadMessages() {
        db.collection("messages")
                .whereEqualTo("conversationId", conversationId)
                .orderBy("timestamp")
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;

                    messageList.clear();
                    for (var doc : value) {
                        Message msg = doc.toObject(Message.class);
                        messageList.add(msg);
                    }
                    adapter.notifyDataSetChanged();

                    // 👉 auto scroll xuống cuối
                    binding.recyclerMessages.scrollToPosition(messageList.size() - 1);
                });
    }

    private void sendMessage() {
        String text = binding.edtMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        Message msg = new Message(currentUserId, text, Timestamp.now());

        db.collection("messages")
                .add(new java.util.HashMap<String, Object>() {{
                    put("conversationId", conversationId);
                    put("senderId", msg.getSenderId());
                    put("content", msg.getContent());
                    put("timestamp", msg.getTimestamp());
                }});
        db.collection("conversations")
                .document(conversationId)
                .set(new java.util.HashMap<String, Object>() {{
                    put("conversationId", conversationId);
                    put("userIds", java.util.Arrays.asList(currentUserId, receiverId));
                    put("lastMessage", text);
                    put("lastTimestamp", Timestamp.now());
                }});
        binding.edtMessage.setText("");
        binding.edtMessage.requestFocus();
    }
}