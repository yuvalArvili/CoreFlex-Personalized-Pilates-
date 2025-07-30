package com.example.coreflexpilates.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.databinding.ItemFriendRowBinding
import com.example.coreflexpilates.model.User

class FriendsListAdapter(
    private val friends: List<User>,
    private val onShowLessonsClick: (User) -> Unit
) : RecyclerView.Adapter<FriendsListAdapter.FriendViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(friends[position])
    }

    override fun getItemCount(): Int = friends.size

    inner class FriendViewHolder(private val binding: ItemFriendRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: User) {
            binding.friendName.text = friend.name
            binding.friendEmail.text = friend.email
            binding.buttonShowLessons.setOnClickListener {
                onShowLessonsClick(friend)
            }
        }
    }
}
