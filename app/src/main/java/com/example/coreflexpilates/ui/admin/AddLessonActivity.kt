package com.example.coreflexpilates.ui.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.coreflexpilates.databinding.ActivityAddLessonBinding
import com.example.coreflexpilates.model.Lesson
import com.example.coreflexpilates.model.Schedule
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AddLessonActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddLessonBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val trainerList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLessonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Add New Lesson"

        loadTrainers()

        binding.editDate.setOnClickListener {
            showDatePicker()
        }

        binding.editTime.setOnClickListener {
            showTimePicker()
        }

        binding.buttonSaveLesson.setOnClickListener {
            saveLesson()
        }
    }

    private fun loadTrainers() {
        firestore.collection("trainers")
            .get()
            .addOnSuccessListener { snapshot ->
                trainerList.clear()
                for (doc in snapshot) {
                    val name = doc.getString("name")
                    if (!name.isNullOrEmpty()) {
                        trainerList.add(name)
                    }
                }
                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    trainerList
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerTrainer.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load trainers", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(this, { _, y, m, d ->
            val dateStr = "%04d-%02d-%02d".format(y, m + 1, d)
            binding.editDate.setText(dateStr)
        }, year, month, day)

        dialog.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val dialog = TimePickerDialog(this, { _, h, m ->
            val timeStr = "%02d:%02d".format(h, m)
            binding.editTime.setText(timeStr)
        }, hour, minute, true)

        dialog.show()
    }

    private fun saveLesson() {
        val title = binding.editTitle.text.toString().trim()
        val date = binding.editDate.text.toString().trim()
        val time = binding.editTime.text.toString().trim()
        val location = binding.editLocation.text.toString().trim()
        val capacityStr = binding.editCapacity.text.toString().trim()
        val trainerId = binding.spinnerTrainer.selectedItem?.toString()?.trim() ?: ""

        if (title.isEmpty() || date.isEmpty() || time.isEmpty() ||
            location.isEmpty() || capacityStr.isEmpty() || trainerId.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val capacity = capacityStr.toIntOrNull()
        if (capacity == null || capacity <= 0) {
            Toast.makeText(this, "Capacity must be a positive number", Toast.LENGTH_SHORT).show()
            return
        }

        val classId = UUID.randomUUID().toString()
        val schedule = Schedule(date = date, day = "", time = time)
        val lesson = Lesson(
            title = title,
            schedule = schedule,
            location = location,
            capacity = capacity,
            trainerId = trainerId,
            classId = classId
        )

        firestore.collection("lessons")
            .document(classId)
            .set(lesson)
            .addOnSuccessListener {
                Toast.makeText(this, "Lesson added successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add lesson", Toast.LENGTH_SHORT).show()
            }
    }
}
