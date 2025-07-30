package com.example.coreflexpilates.model

data class FriendRequest(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val status: String = "pending" // "pending", "accepted", "declined"
)
