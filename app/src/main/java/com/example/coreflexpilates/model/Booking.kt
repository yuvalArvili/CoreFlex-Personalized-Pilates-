package com.example.coreflexpilates.model

data class Booking(
    val lessonId: String = "",
    val userId: String = "",
    val timestamp: Long = 0L,
    val fcmToken: String? = null
)
