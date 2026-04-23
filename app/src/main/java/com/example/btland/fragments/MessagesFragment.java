package com.example.btland.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.btland.adapters.ConversationAdapter;
import com.example.btland.databinding.FragmentMessagesBinding;
import com.example.btland.models.Conversation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MessagesFragment extends Fragment {

    private FragmentMessagesBinding binding;
    private final List<Conversation> list = new ArrayList<>();
    private ConversationAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessagesBinding.inflate(inflater, container, false);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        adapter = new ConversationAdapter(list);

        binding.recyclerConversations.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerConversations.setAdapter(adapter);

        loadConversations();
        return binding.getRoot();
    }

    private void loadConversations() {
        if (currentUserId == null) {
            binding.txtEmptyConversations.setVisibility(View.VISIBLE);
            return;
        }

        db.collection("conversations")
                .whereArrayContains("userIds", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (value == null) {
                        return;
                    }

                    list.clear();
                    for (var doc : value) {
                        Conversation conversation = doc.toObject(Conversation.class);
                        if (conversation != null) {
                            list.add(conversation);
                        }
                    }
                    list.sort((left, right) -> {
                        if (left.getLastTimestamp() == null && right.getLastTimestamp() == null) return 0;
                        if (left.getLastTimestamp() == null) return 1;
                        if (right.getLastTimestamp() == null) return -1;
                        return right.getLastTimestamp().compareTo(left.getLastTimestamp());
                    });
                    binding.txtEmptyConversations.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                });
    }
}
