package com.example.coreflexpilates.ui.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.User

class FindFriendsAdapter(
    private var users: List<User>,
    private val friendIds: Set<String>,
    private val onAddClick: (User) -> Unit       // Callback when the "Follow" button is clicked
) : RecyclerView.Adapter<FindFriendsAdapter.UserViewHolder>() {

    private val requestedUserIds = mutableSetOf<String>()

    // Update the displayed list
    fun updateList(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.friendName)
        val email: TextView = itemView.findViewById(R.id.friendEmail)
        val addButton: Button = itemView.findViewById(R.id.buttonAddFriend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.name.text = user.name
        holder.email.text = user.email


        when {
            friendIds.contains(user.uid) -> {

                holder.addButton.text = "FRIEND" // User is already a friend
                holder.addButton.isEnabled = false
            }
            requestedUserIds.contains(user.uid) -> {
                holder.addButton.text = "REQUESTED" // Friend request has already been sent

                holder.addButton.isEnabled = false
            }
            else -> {
                // User is not friend or requested yet
                holder.addButton.text = "FOLLOW"
                holder.addButton.isEnabled = true
                holder.addButton.setOnClickListener {
                    requestedUserIds.add(user.uid)
                    notifyItemChanged(position)
                    onAddClick(user)  // Invoke callback to send friend request
                }
            }
        }
    }

    override fun getItemCount(): Int = users.size
}

