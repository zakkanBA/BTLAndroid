package com.example.btland.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.btland.adapters.MessageAdapter;
import com.example.btland.databinding.ActivityChatBinding;
import com.example.btland.models.Message;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private String receiverId;
    private String currentUserId;
    private String conversationId;
    private String currentUserName = "Bạn";
    private String receiverName = "Người dùng";
    private FirebaseFirestore db;
    private final List<Message> messageList = new ArrayList<>();
    private MessageAdapter adapter;
    private boolean conversationReady;
    private boolean isSending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        receiverId = getIntent().getStringExtra("receiverId");
        currentUserId = FirebaseAuth.getInstance().getUid();
        db = FirebaseFirestore.getInstance();

        if (receiverId == null || currentUserId == null) {
            finish();
            return;
        }

        conversationId = buildConversationId(currentUserId, receiverId);
        adapter = new MessageAdapter(messageList, currentUserId);
        binding.recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerMessages.setAdapter(adapter);
        binding.btnSend.setEnabled(false);

        loadParticipantNames();
        ensureConversationDocument(() -> {
            conversationReady = true;
            binding.btnSend.setEnabled(true);
        });
        loadMessages();
        markMessagesAsRead();

        binding.btnSend.setOnClickListener(v -> sendMessage());
    }

    private String buildConversationId(String firstUserId, String secondUserId) {
        String[] ids = new String[]{firstUserId, secondUserId};
        Arrays.sort(ids);
        return ids[0] + "_" + ids[1];
    }

    private void loadParticipantNames() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = documentSnapshot.getString("name");
                    if (name != null && !name.isEmpty()) {
                        currentUserName = name;
                    }
                    ensureConversationDocument(null);
                });

        db.collection("users").document(receiverId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = documentSnapshot.getString("name");
                    if (name != null && !name.isEmpty()) {
                        receiverName = name;
                        binding.txtChatTitle.setText(name);
                    }
                    ensureConversationDocument(null);
                });
    }

    private void ensureConversationDocument(@androidx.annotation.Nullable Runnable onReady) {
        Map<String, Object> participantNames = new HashMap<>();
        participantNames.put(currentUserId, currentUserName);
        participantNames.put(receiverId, receiverName);

        Map<String, Object> unreadCounts = new HashMap<>();
        unreadCounts.put(currentUserId, 0L);
        unreadCounts.put(receiverId, 0L);

        Map<String, Object> conversation = new HashMap<>();
        conversation.put("conversationId", conversationId);
        conversation.put("userIds", Arrays.asList(currentUserId, receiverId));
        conversation.put("participantNames", participantNames);
        conversation.put("unreadCounts", unreadCounts);
        conversation.put("lastTimestamp", Timestamp.now());

        db.collection("conversations")
                .document(conversationId)
                .set(conversation, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    conversationReady = true;
                    binding.btnSend.setEnabled(true);
                    if (onReady != null) {
                        onReady.run();
                    }
                })
                .addOnFailureListener(e -> {
                    conversationReady = false;
                    binding.btnSend.setEnabled(false);
                    Toast.makeText(this, "Không tạo được hội thoại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadMessages() {
        db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener((value, error) -> {
                    if (value == null) {
                        return;
                    }

                    messageList.clear();
                    for (var doc : value) {
                        Message msg = doc.toObject(Message.class);
                        if (msg != null) {
                            messageList.add(msg);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        binding.recyclerMessages.scrollToPosition(messageList.size() - 1);
                    }
                    markMessagesAsRead();
                });
    }

    private void sendMessage() {
        if (isSending) {
            return;
        }

        String text = binding.edtMessage.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        if (!conversationReady) {
            ensureConversationDocument(() -> sendMessageInternal(text));
            return;
        }

        sendMessageInternal(text);
    }

    private void sendMessageInternal(String text) {
        isSending = true;
        binding.btnSend.setEnabled(false);

        String messageId = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document()
                .getId();

        Message msg = new Message(currentUserId, receiverId, text, Timestamp.now(), false);
        msg.setMessageId(messageId);

        db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .set(msg)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> participantNames = new HashMap<>();
                    participantNames.put(currentUserId, currentUserName);
                    participantNames.put(receiverId, receiverName);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("conversationId", conversationId);
                    updates.put("userIds", Arrays.asList(currentUserId, receiverId));
                    updates.put("lastMessage", text);
                    updates.put("lastTimestamp", Timestamp.now());
                    updates.put("participantNames", participantNames);
                    updates.put("unreadCounts." + currentUserId, 0L);
                    updates.put("unreadCounts." + receiverId, FieldValue.increment(1));

                    db.collection("conversations")
                            .document(conversationId)
                            .set(updates, SetOptions.merge())
                            .addOnSuccessListener(result -> {
                                binding.edtMessage.setText("");
                                binding.edtMessage.requestFocus();
                                isSending = false;
                                binding.btnSend.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                isSending = false;
                                binding.btnSend.setEnabled(true);
                                Toast.makeText(this, "Không cập nhật được hội thoại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    isSending = false;
                    binding.btnSend.setEnabled(true);
                    Toast.makeText(this, "Không gửi được tin nhắn: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void markMessagesAsRead() {
        db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("unreadCounts." + currentUserId, 0L);
                        db.collection("conversations")
                                .document(conversationId)
                                .set(updates, SetOptions.merge());
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (var doc : queryDocumentSnapshots) {
                        batch.update(doc.getReference(), "read", true);
                    }
                    batch.commit();

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("unreadCounts." + currentUserId, 0L);
                    db.collection("conversations")
                            .document(conversationId)
                            .set(updates, SetOptions.merge());
                });
    }
}
