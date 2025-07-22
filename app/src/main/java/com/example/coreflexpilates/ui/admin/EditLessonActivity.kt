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

class EditLessonActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddLessonBinding
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var lessonId: String
    private var trainerList = listOf("trainer1", "trainer2", "trainer3") // Load dynamically if needed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLessonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Edit Lesson"

        // Get lessonId from intent
        lessonId = intent.getStringExtra("lessonId") ?: return

        setupTrainerSpinner()
        setupPickers()
        loadLesson()

        binding.buttonSaveLesson.setOnClickListener {
            updateLesson()
        }
    }

    private fun setupTrainerSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, trainerList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTrainer.adapter = adapter
    }

    private fun setupPickers() {
        binding.editDate.setOnClickListener { showDatePicker() }
        binding.editTime.setOnClickListener { showTimePicker() }
    }

    private fun loadLesson() {
        firestore.collection("lessons").document(lessonId)
            .get()
            .addOnSuccessListener { doc ->
                val lesson = doc.toObject(Lesson::class.java)
                if (lesson != null) {
                    binding.editTitle.setText(lesson.title)
                    binding.editDate.setText(lesson.schedule.date)
                    binding.editTime.setText(lesson.schedule.time)
                    binding.editLocation.setText(lesson.location)
                    binding.editCapacity.setText(lesson.capacity.toString())

                    val trainerIndex = trainerList.indexOf(lesson.trainerId)
                    if (trainerIndex != -1) {
                        binding.spinnerTrainer.setSelection(trainerIndex)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load lesson", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun updateLesson() {
        val title = binding.editTitle.text.toString().trim()
        val date = binding.editDate.text.toString().trim()
        val time = binding.editTime.text.toString().trim()
        val location = binding.editLocation.text.toString().trim()
        val capacityStr = binding.editCapacity.text.toString().trim()
        val trainerId = binding.spinnerTrainer.selectedItem.toString()

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

        val updatedLesson = Lesson(
            title = title,
            schedule = Schedule(date = date, day = "", time = time),
            location = location,
            capacity = capacity,
            trainerId = trainerId,
            classId = lessonId
        )

        firestore.collection("lessons").document(lessonId)
            .set(updatedLesson)
            .addOnSuccessListener {
                Toast.makeText(this, "Lesson updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update lesson", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            val dateStr = "%04d-%02d-%02d".format(y, m + 1, d)
            binding.editDate.setText(dateStr)
        }, year, month, day).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, h, m ->
            val timeStr = "%02d:%02d".format(h, m)
            binding.editTime.setText(timeStr)
        }, hour, minute, true).show()
    }
}
