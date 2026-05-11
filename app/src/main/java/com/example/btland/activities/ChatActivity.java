package com.example.btland.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.btland.adapters.MessageAdapter;
import com.example.btland.databinding.ActivityChatBinding;
import com.example.btland.models.Message;
import com.example.btland.offline.CachedMessage;
import com.example.btland.offline.OfflineDatabase;
import com.example.btland.offline.PendingMessagePayload;
import com.example.btland.offline.PendingSyncManager;
import com.example.btland.utils.NetworkUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private boolean isSending;
    private boolean cacheLoaded = false;
    private final Set<String> pendingIds = new HashSet<>();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

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

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSend.setOnClickListener(v -> sendMessage());

        // Đọc tên đã lưu từ lần online trước (dùng khi offline)
        SharedPreferences prefs = getPrefs();
        String savedMyName = prefs.getString("name_" + currentUserId, null);
        String savedReceiverName = prefs.getString("name_" + receiverId, null);
        if (savedMyName != null) currentUserName = savedMyName;
        if (savedReceiverName != null) {
            receiverName = savedReceiverName;
            binding.txtChatTitle.setText(receiverName);
        }

        // Load cache ngay lập tức — nút gửi LUÔN enabled từ đầu
        loadCachedMessages();

        if (NetworkUtils.isOnline(this)) {
            loadParticipantNames();
        }
        // Nếu offline thì không gọi Firestore gì cả — Firestore sẽ không callback
    }

    @Override
    protected void onResume() {
        super.onResume();
        PendingSyncManager.getInstance(this).setMessageSyncedListener(messageId -> {
            pendingIds.remove(messageId);
            for (int i = 0; i < messageList.size(); i++) {
                if (messageId.equals(messageList.get(i).getMessageId())) {
                    messageList.get(i).setPending(false);
                    adapter.notifyItemChanged(i);
                    break;
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        PendingSyncManager.getInstance(this).removeMessageSyncedListener();
    }

    // ─── Cache ───────────────────────────────────────────────────────────────

    private void loadCachedMessages() {
        dbExecutor.execute(() -> {
            try {
                List<CachedMessage> cached = OfflineDatabase.getInstance(this)
                        .cachedMessageDao()
                        .getByConversation(conversationId);
                List<Message> converted = new ArrayList<>();
                for (CachedMessage cm : cached) converted.add(toMessage(cm));
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    // Nếu user đã gửi tin trong lúc cache đang load → chỉ merge, không clear
                    if (cacheLoaded) {
                        // Chỉ thêm các tin từ cache chưa có trong list (không đụng pending)
                        Set<String> existingIds = new HashSet<>();
                        for (Message m : messageList) {
                            if (m.getMessageId() != null) existingIds.add(m.getMessageId());
                        }
                        boolean changed = false;
                        for (Message c : converted) {
                            if (c.getMessageId() != null && !existingIds.contains(c.getMessageId())) {
                                messageList.add(0, c); // thêm vào đầu vì cache đã sort theo thời gian
                                changed = true;
                            }
                        }
                        if (changed) {
                            adapter.notifyDataSetChanged();
                            scrollToBottom();
                        }
                        return;
                    }
                    // Giữ lại các tin pending đang chờ gửi, không bị clear khi cache load xong
                    List<Message> pending = new ArrayList<>();
                    for (Message m : messageList) {
                        if (m.isPending()) pending.add(m);
                    }
                    messageList.clear();
                    messageList.addAll(converted);
                    // Gộp lại các tin pending chưa có trong cache
                    for (Message p : pending) {
                        boolean alreadyIn = false;
                        for (Message c : converted) {
                            if (p.getMessageId() != null && p.getMessageId().equals(c.getMessageId())) {
                                alreadyIn = true;
                                break;
                            }
                        }
                        if (!alreadyIn) messageList.add(p);
                    }
                    adapter.notifyDataSetChanged();
                    scrollToBottom();
                    cacheLoaded = true;
                });
            } catch (Exception e) {
                // ignore
            }
        });
    }

    private void cacheMessages(List<Message> messages) {
        dbExecutor.execute(() -> {
            try {
                List<CachedMessage> rows = new ArrayList<>();
                for (Message m : messages) {
                    if (m.getMessageId() == null || m.isPending()) continue;
                    rows.add(toCached(m, conversationId));
                }
                if (!rows.isEmpty()) {
                    OfflineDatabase.getInstance(this).cachedMessageDao().insertAll(rows);
                }
            } catch (Exception e) {
                // ignore
            }
        });
    }

    private static CachedMessage toCached(Message m, String convId) {
        CachedMessage cm = new CachedMessage();
        cm.messageId = m.getMessageId() != null ? m.getMessageId() : "";
        cm.conversationId = convId;
        cm.senderId = m.getSenderId() != null ? m.getSenderId() : "";
        cm.receiverId = m.getReceiverId() != null ? m.getReceiverId() : "";
        cm.content = m.getContent() != null ? m.getContent() : "";
        cm.timestampMs = m.getTimestamp() != null ? m.getTimestamp().toDate().getTime() : 0L;
        cm.read = m.isRead();
        return cm;
    }

    private static Message toMessage(CachedMessage cm) {
        Timestamp ts = cm.timestampMs > 0 ? new Timestamp(new Date(cm.timestampMs)) : null;
        Message m = new Message(cm.senderId, cm.receiverId, cm.content, ts, cm.read);
        m.setMessageId(cm.messageId);
        return m;
    }

    // ─── Load tên từ Firestore (chỉ khi online) ──────────────────────────────

    private String buildConversationId(String a, String b) {
        String[] ids = {a, b};
        Arrays.sort(ids);
        return ids[0] + "_" + ids[1];
    }

    private void loadParticipantNames() {
        final int[] done = {0};

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    if (name != null && !name.isEmpty()) {
                        currentUserName = name;
                        getPrefs().edit().putString("name_" + currentUserId, name).apply();
                    }
                    if (++done[0] == 2) onBothNamesLoaded();
                })
                .addOnFailureListener(e -> { if (++done[0] == 2) onBothNamesLoaded(); });

        db.collection("users").document(receiverId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    if (name != null && !name.isEmpty()) {
                        receiverName = name;
                        binding.txtChatTitle.setText(name);
                        getPrefs().edit().putString("name_" + receiverId, name).apply();
                    }
                    String avatarUrl = doc.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this).load(avatarUrl)
                                .transform(new CircleCrop())
                                .placeholder(android.R.drawable.sym_def_app_icon)
                                .into(binding.imgReceiverAvatar);
                    } else {
                        binding.imgReceiverAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
                    if (++done[0] == 2) onBothNamesLoaded();
                })
                .addOnFailureListener(e -> { if (++done[0] == 2) onBothNamesLoaded(); });
    }

    private void onBothNamesLoaded() {
        ensureConversationDocument(() -> {
            loadMessages();
            markMessagesAsRead();
        });
    }

    // ─── Firestore ────────────────────────────────────────────────────────────

    private void ensureConversationDocument(Runnable onReady) {
        Map<String, Object> participantNames = new HashMap<>();
        participantNames.put(currentUserId, currentUserName);
        participantNames.put(receiverId, receiverName);

        Map<String, Object> conv = new HashMap<>();
        conv.put("conversationId", conversationId);
        conv.put("userIds", Arrays.asList(currentUserId, receiverId));
        conv.put("participantNames", participantNames);
        conv.put("lastTimestamp", Timestamp.now());

        db.collection("conversations").document(conversationId)
                .set(conv, SetOptions.merge())
                .addOnSuccessListener(u -> { if (onReady != null) onReady.run(); })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadMessages() {
        db.collection("conversations").document(conversationId)
                .collection("messages").orderBy("timestamp")
                .addSnapshotListener((value, error) -> {
                    if (value == null || isDestroyed()) return;

                    List<Message> fromServer = new ArrayList<>();
                    for (var doc : value) {
                        Message msg = doc.toObject(Message.class);
                        if (msg != null) fromServer.add(msg);
                    }

                    cacheMessages(fromServer);

                    // Giữ lại các tin đang pending chưa có trên server
                    List<Message> merged = new ArrayList<>(fromServer);
                    for (Message p : messageList) {
                        if (!p.isPending()) continue;
                        boolean found = false;
                        for (Message s : fromServer) {
                            if (p.getMessageId() != null && p.getMessageId().equals(s.getMessageId())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) merged.add(p);
                        else pendingIds.remove(p.getMessageId());
                    }

                    merged.sort((a, b) -> {
                        long ta = a.getTimestamp() != null ? a.getTimestamp().toDate().getTime() : Long.MAX_VALUE;
                        long tb = b.getTimestamp() != null ? b.getTimestamp().toDate().getTime() : Long.MAX_VALUE;
                        return Long.compare(ta, tb);
                    });

                    messageList.clear();
                    messageList.addAll(merged);
                    adapter.notifyDataSetChanged();
                    scrollToBottom();
                    markMessagesAsRead();
                });
    }

    // ─── Gửi ─────────────────────────────────────────────────────────────────

    private void sendMessage() {
        if (isSending) return;
        String text = binding.edtMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        isSending = true;
        binding.btnSend.setEnabled(false);

        String messageId = java.util.UUID.randomUUID().toString().replace("-", "");

        if (!NetworkUtils.isOnline(this)) {
            queueOfflineMessage(messageId, text);
            return;
        }

        Message msg = new Message(currentUserId, receiverId, text, Timestamp.now(), false);
        msg.setMessageId(messageId);

        db.collection("conversations").document(conversationId)
                .collection("messages").document(messageId).set(msg)
                .addOnSuccessListener(u -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("conversationId", conversationId);
                    updates.put("userIds", Arrays.asList(currentUserId, receiverId));
                    updates.put("lastMessage", text);
                    updates.put("lastTimestamp", Timestamp.now());
                    updates.put("unreadCounts." + currentUserId, 0L);
                    updates.put("unreadCounts." + receiverId, FieldValue.increment(1));
                    Map<String, Object> names = new HashMap<>();
                    names.put(currentUserId, currentUserName);
                    names.put(receiverId, receiverName);
                    updates.put("participantNames", names);

                    db.collection("conversations").document(conversationId)
                            .set(updates, SetOptions.merge())
                            .addOnSuccessListener(r -> {
                                binding.edtMessage.setText("");
                                binding.edtMessage.requestFocus();
                                isSending = false;
                                binding.btnSend.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                isSending = false;
                                binding.btnSend.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    isSending = false;
                    binding.btnSend.setEnabled(true);
                    Toast.makeText(this, "Gửi thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void queueOfflineMessage(String messageId, String text) {
        long createdAt = System.currentTimeMillis();

        // Đảm bảo loadCachedMessages không thể overwrite sau khi ta đã add tin pending
        cacheLoaded = true;

        // Hiển thị tin lên UI NGAY LẬP TỨC với trạng thái "Đang gửi..."
        // Không chờ background thread queue xong mới show
        Message local = new Message(currentUserId, receiverId, text,
                new Timestamp(new Date(createdAt)), false);
        local.setMessageId(messageId);
        local.setPending(true);
        pendingIds.add(messageId);

        messageList.add(local);
        adapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
        binding.edtMessage.setText("");
        binding.edtMessage.requestFocus();
        isSending = false;
        binding.btnSend.setEnabled(true);

        // Queue vào background sau khi đã show UI
        PendingMessagePayload payload = new PendingMessagePayload();
        payload.conversationId = conversationId;
        payload.messageId = messageId;
        payload.senderId = currentUserId;
        payload.receiverId = receiverId;
        payload.senderName = currentUserName;
        payload.receiverName = receiverName;
        payload.content = text;
        payload.createdAt = createdAt;

        PendingSyncManager.getInstance(this).queueMessage(payload, new PendingSyncManager.QueueCallback() {
            @Override
            public void onSuccess() {
                // Đã queue thành công, tin đang hiện "Đang gửi..." rồi — không cần làm gì thêm
            }

            @Override
            public void onError(String errorMessage) {
                // Queue thất bại → xoá tin khỏi UI và báo lỗi
                pendingIds.remove(messageId);
                for (int i = messageList.size() - 1; i >= 0; i--) {
                    if (messageId.equals(messageList.get(i).getMessageId())) {
                        messageList.remove(i);
                        adapter.notifyItemRemoved(i);
                        break;
                    }
                }
                Toast.makeText(ChatActivity.this, "Không lưu được tin offline: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── Đọc ─────────────────────────────────────────────────────────────────

    private void markMessagesAsRead() {
        if (!NetworkUtils.isOnline(this)) return;
        db.collection("conversations").document(conversationId)
                .collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("read", false).get()
                .addOnSuccessListener(snap -> {
                    Map<String, Object> upd = new HashMap<>();
                    upd.put("unreadCounts." + currentUserId, 0L);
                    if (!snap.isEmpty()) {
                        WriteBatch batch = db.batch();
                        for (var doc : snap) batch.update(doc.getReference(), "read", true);
                        batch.commit();
                    }
                    db.collection("conversations").document(conversationId).set(upd, SetOptions.merge());
                });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void scrollToBottom() {
        if (!messageList.isEmpty())
            binding.recyclerMessages.scrollToPosition(messageList.size() - 1);
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences("btland_chat_prefs", MODE_PRIVATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}