package com.example.coreflexpilates.model

data class Schedule(
    val date: String = "",
    val day: String = "",
    val time: String = ""
)

data class Lesson(
    val title: String = "",
    val schedule: Schedule = Schedule(),
    val capacity: Int = 0,
    val bookedCount: Int = 0,
    val trainerId: String = "",
    val classId: String = ""
)