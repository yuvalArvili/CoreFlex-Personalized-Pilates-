package com.example.coreflexpilates.model

data class Friendship(
    val user1Id: String = "",
    val user2Id: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
