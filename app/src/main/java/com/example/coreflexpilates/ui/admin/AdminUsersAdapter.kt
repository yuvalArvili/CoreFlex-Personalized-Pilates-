package com.example.coreflexpilates.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.coreflexpilates.databinding.ItemAdminUserBinding
import com.example.coreflexpilates.model.User

class AdminUsersAdapter(
    private val users: List<User>,
    private val userImagesMap: Map<String, String?>,
    private val onUserClick: (User) -> Unit            // Callback when a user item is clicked
) : RecyclerView.Adapter<AdminUsersAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemAdminUserBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.textUserName.text = user.name
            binding.textUserEmail.text = user.email

            val imageUrl = userImagesMap[user.uid]

            if (!imageUrl.isNullOrEmpty()) {
                // Load image from URL with Glide
                Glide.with(binding.imageProfile.context)
                    .load(imageUrl)
                    .transform(CircleCrop())
                    .into(binding.imageProfile)
            } else {
                // Set default user icon
                binding.imageProfile.setImageResource(com.example.coreflexpilates.R.drawable.baseline_perm_identity_24)
            }

            // Handle click on the entire user item
            binding.root.setOnClickListener {
                onUserClick(user)  // Trigger callback with the clicked user
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemAdminUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size
}

