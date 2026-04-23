package com.example.btland.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btland.activities.AdminUserPostsActivity;
import com.example.btland.databinding.ItemAdminUserBinding;
import com.example.btland.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserAdminAdapter extends RecyclerView.Adapter<UserAdminAdapter.UserViewHolder> {

    private final List<User> userList;

    public UserAdminAdapter(List<User> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminUserBinding binding = ItemAdminUserBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.binding.txtName.setText(user.getName() != null ? user.getName() : "Không có tên");
        holder.binding.txtEmail.setText(user.getEmail() != null ? user.getEmail() : "Không có email");
        holder.binding.txtStatus.setText(user.isBanned() ? "Trạng thái: Đã khóa" : "Trạng thái: Hoạt động");
        holder.binding.btnToggleBan.setText(user.isBanned() ? "Mở khóa" : "Khóa");

        holder.binding.btnToggleBan.setOnClickListener(v -> {
            boolean newStatus = !user.isBanned();
            String action = newStatus ? "khóa" : "mở khóa";

            new AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có chắc muốn " + action + " tài khoản này?")
                    .setPositiveButton("Đồng ý", (dialog, which) ->
                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(user.getUserId())
                                    .update("isBanned", newStatus)
                                    .addOnSuccessListener(unused -> {
                                        user.setBanned(newStatus);
                                        notifyItemChanged(position);
                                        Toast.makeText(holder.itemView.getContext(), "Đã cập nhật trạng thái tài khoản", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(holder.itemView.getContext(), "Không cập nhật được tài khoản", Toast.LENGTH_SHORT).show()
                                    ))
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        holder.binding.btnViewPosts.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), AdminUserPostsActivity.class);
            intent.putExtra("userId", user.getUserId());
            intent.putExtra("userName", user.getName());
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final ItemAdminUserBinding binding;

        UserViewHolder(ItemAdminUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
