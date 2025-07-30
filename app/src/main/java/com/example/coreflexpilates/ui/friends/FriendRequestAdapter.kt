package com.example.coreflexpilates.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.databinding.ItemFriendRequestBinding
import com.example.coreflexpilates.model.FriendRequest

class FriendRequestsAdapter(
    private var requests: List<FriendRequest>,
    private val userNames: Map<String, String>,
    private val onAcceptClick: (FriendRequest) -> Unit,
    private val onDeclineClick: (FriendRequest) -> Unit
) : RecyclerView.Adapter<FriendRequestsAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(val binding: ItemFriendRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(request: FriendRequest) {
            val senderName = userNames[request.senderId] ?: request.senderId
            binding.textSenderId.text = "Request from: $senderName"
            binding.buttonAccept.setOnClickListener { onAcceptClick(request) }
            binding.buttonDecline.setOnClickListener { onDeclineClick(request) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemFriendRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    override fun getItemCount(): Int = requests.size

    fun updateRequests(newRequests: List<FriendRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }
}
