package com.example.coreflexpilates.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.databinding.ItemInviteFriendBinding
import com.example.coreflexpilates.model.User

class InviteFriendsAdapter(
    private val friends: List<User>,
    private val onInviteClick: (User) -> Unit
) : RecyclerView.Adapter<InviteFriendsAdapter.FriendViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemInviteFriendBinding.inflate(
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

    inner class FriendViewHolder(private val binding: ItemInviteFriendBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: User) {
            binding.friendName.text = friend.name
            binding.friendEmail.text = friend.email
            binding.buttonInvite.setOnClickListener {
                onInviteClick(friend)
            }
        }
    }
}
