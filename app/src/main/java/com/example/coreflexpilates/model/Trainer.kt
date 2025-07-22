package com.example.coreflexpilates.model

data class Trainer(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val specialties: List<String> = emptyList()
)