package com.example.btland.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btland.activities.AdminUserPostsActivity;
import com.example.btland.databinding.ItemAdminUserBinding;
import com.example.btland.models.User;

import java.util.List;

public class UserAdminAdapter extends RecyclerView.Adapter<UserAdminAdapter.UserViewHolder> {

    public interface BanToggleListener {
        void onToggleBan(User user, boolean banned, int position);
    }

    private final List<User> userList;
    private final BanToggleListener banToggleListener;

    public UserAdminAdapter(List<User> userList, BanToggleListener banToggleListener) {
        this.userList = userList;
        this.banToggleListener = banToggleListener;
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

        holder.binding.txtName.setText(nonEmpty(user.getName(), "Không có tên"));
        holder.binding.txtEmail.setText(nonEmpty(user.getEmail(), "Không có email"));
        holder.binding.txtStatus.setText(user.isBanned() ? "Trạng thái: Đã khóa" : "Trạng thái: Hoạt động");
        holder.binding.btnToggleBan.setText(user.isBanned() ? "Mở khóa" : "Khóa");

        holder.binding.btnToggleBan.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) {
                return;
            }

            User currentUser = userList.get(currentPosition);
            boolean newStatus = !currentUser.isBanned();
            String action = newStatus ? "khóa" : "mở khóa";
            String postMessage = newStatus
                    ? "Các bài đang hiển thị của tài khoản này sẽ bị tạm ẩn."
                    : "Các bài bị ẩn do khóa tài khoản sẽ được hiển thị lại.";

            new AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có chắc muốn " + action + " tài khoản này?\n\n" + postMessage)
                    .setPositiveButton("Đồng ý", (dialog, which) ->
                            banToggleListener.onToggleBan(currentUser, newStatus, currentPosition))
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

    private String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final ItemAdminUserBinding binding;

        UserViewHolder(ItemAdminUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
