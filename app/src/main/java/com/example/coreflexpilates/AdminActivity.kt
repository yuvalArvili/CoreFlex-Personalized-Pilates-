package com.example.coreflexpilates

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreflexpilates.databinding.ActivityAdminBinding
import com.example.coreflexpilates.model.Lesson
import com.example.coreflexpilates.ui.admin.AddLessonActivity
import com.example.coreflexpilates.ui.admin.AddTrainerActivity
import com.example.coreflexpilates.ui.admin.EditLessonActivity
import com.example.coreflexpilates.ui.home.LessonAdapter
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var adapter: LessonAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val lessons = mutableListOf<Lesson>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Admin Panel"

        adapter = LessonAdapter(
            isAdmin = true,
            onEditClick = { lesson -> editLesson(lesson) },
            onDeleteClick = { lesson -> confirmDelete(lesson) }
        )

        binding.recyclerViewLessons.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewLessons.adapter = adapter

        loadLessons()

        binding.fabAddLesson.setOnClickListener {
            // Navigate to AddLessonActivity (create it separately)
            startActivity(Intent(this, AddLessonActivity::class.java))
        }

        binding.buttonAddTrainer.setOnClickListener {
            // Navigate to AddTrainerActivity (create it separately)
            startActivity(Intent(this, AddTrainerActivity::class.java))
        }
    }

    private fun loadLessons() {
        firestore.collection("lessons")
            .get()
            .addOnSuccessListener { snapshot ->
                lessons.clear()
                for (doc in snapshot) {
                    val lesson = doc.toObject(Lesson::class.java)
                    lessons.add(lesson)
                }
                adapter.updateData(lessons)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load lessons", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDelete(lesson: Lesson) {
        AlertDialog.Builder(this)
            .setTitle("Delete Lesson")
            .setMessage("Are you sure you want to delete this lesson?")
            .setPositiveButton("Delete") { _: DialogInterface, _: Int ->
                deleteLesson(lesson)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteLesson(lesson: Lesson) {
        firestore.collection("lessons")
            .document(lesson.classId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Lesson deleted", Toast.LENGTH_SHORT).show()
                loadLessons()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
    }

    private fun editLesson(lesson: Lesson) {
        val intent = Intent(this, EditLessonActivity::class.java)
        intent.putExtra("lessonId", lesson.classId)
        startActivity(intent)
    }
}
