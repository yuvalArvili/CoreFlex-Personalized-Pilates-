package com.example.coreflexpilates.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.databinding.ItemFriendRequestBinding
import com.example.coreflexpilates.model.FriendRequest
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestAdapter(
    private val requests: List<FriendRequest>,
    private val onAcceptClick: (FriendRequest) -> Unit,
    private val onDeclineClick: (FriendRequest) -> Unit
) : RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemFriendRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    override fun getItemCount(): Int = requests.size

    inner class RequestViewHolder(private val binding: ItemFriendRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(request: FriendRequest) {
            FirebaseFirestore.getInstance().collection("users")
                .document(request.senderId)
                .get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: "Unknown"
                    binding.textSenderName.text = name
                }

            binding.buttonAccept.setOnClickListener {
                onAcceptClick(request)
            }

            binding.buttonDecline.setOnClickListener {
                onDeclineClick(request)
            }
        }
    }
}
