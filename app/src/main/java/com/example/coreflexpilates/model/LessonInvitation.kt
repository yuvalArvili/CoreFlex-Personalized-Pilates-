package com.example.coreflexpilates.model

data class LessonInvitation(
    val senderId: String = "",
    val receiverId: String = "",
    val lessonId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending" // "pending", "accepted", "declined"
)
