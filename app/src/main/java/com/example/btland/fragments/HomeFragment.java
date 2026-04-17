package com.example.btland.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.btland.adapters.PostAdapter;
import com.example.btland.databinding.FragmentHomeBinding;
import com.example.btland.models.Post;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private final List<Post> allPosts = new ArrayList<>();
    private final List<Post> filteredPosts = new ArrayList<>();
    private PostAdapter adapter;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();

        adapter = new PostAdapter(filteredPosts);
        binding.recyclerPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerPosts.setAdapter(adapter);

        setupSpinner();
        setupEvents();
        loadPosts();

        return binding.getRoot();
    }

    private void setupSpinner() {
        List<String> types = new ArrayList<>();
        types.add("Tất cả");
        types.add("Cho thuê");
        types.add("Ở ghép");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                types
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerType.setAdapter(spinnerAdapter);
    }

    private void setupEvents() {
        binding.btnApplyFilter.setOnClickListener(v -> applyFilters());

        binding.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadPosts() {
        db.collection("posts")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allPosts.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            allPosts.add(post);
                        }
                    }
                    applyFilters();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi load dữ liệu", Toast.LENGTH_SHORT).show()
                );
    }

    private void applyFilters() {
        String keyword = binding.edtSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        String selectedType = binding.spinnerType.getSelectedItem().toString();
        String minPriceText = binding.edtMinPrice.getText().toString().trim();
        String maxPriceText = binding.edtMaxPrice.getText().toString().trim();

        double minPrice = 0;
        double maxPrice = Double.MAX_VALUE;

        try {
            if (!minPriceText.isEmpty()) minPrice = Double.parseDouble(minPriceText);
            if (!maxPriceText.isEmpty()) maxPrice = Double.parseDouble(maxPriceText);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Giá không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        filteredPosts.clear();

        for (Post post : allPosts) {
            boolean matchKeyword =
                    (post.getTitle() != null && post.getTitle().toLowerCase(Locale.ROOT).contains(keyword)) ||
                            (post.getAddress() != null && post.getAddress().toLowerCase(Locale.ROOT).contains(keyword));

            boolean matchType =
                    selectedType.equals("Tất cả") ||
                            (selectedType.equals("Cho thuê") && "rent".equals(post.getType())) ||
                            (selectedType.equals("Ở ghép") && "roommate".equals(post.getType()));

            boolean matchPrice = post.getPrice() >= minPrice && post.getPrice() <= maxPrice;

            if ((keyword.isEmpty() || matchKeyword) && matchType && matchPrice) {
                filteredPosts.add(post);
            }
        }

        adapter.notifyDataSetChanged();
    }
}