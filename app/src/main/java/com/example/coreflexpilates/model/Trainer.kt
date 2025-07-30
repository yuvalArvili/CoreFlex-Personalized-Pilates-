package com.example.coreflexpilates.model

data class Trainer(
    var id: String = "",
    val name: String = "",
    val email: String = "",
    val specialties: List<String> = emptyList(),
    val imageUrl: String? = null
)