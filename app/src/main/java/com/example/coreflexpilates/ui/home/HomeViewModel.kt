package com.example.coreflexpilates.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.coreflexpilates.model.Lesson
import com.google.firebase.firestore.FirebaseFirestore

class HomeViewModel : ViewModel() {

    private val _lessons = MutableLiveData<List<Lesson>>()
    val lessons: LiveData<List<Lesson>> = _lessons

    init {
        loadLessons()
    }

    private fun loadLessons() {
        val db = FirebaseFirestore.getInstance()
        db.collection("lessons")
            .get()
            .addOnSuccessListener { result ->
                val lessonList = result.map { doc ->
                    doc.toObject(Lesson::class.java)
                }
                _lessons.value = lessonList
            }
            .addOnFailureListener { e ->
            }
    }
}
