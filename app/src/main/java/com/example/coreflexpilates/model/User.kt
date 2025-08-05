package com.example.coreflexpilates.model

data class User(
    var uid: String = "",
    val name: String = "",
    val email: String = "",
    val fcmToken: String? = null,
    val role: String = "user",
    val subscriptionActive: Boolean = true
)
