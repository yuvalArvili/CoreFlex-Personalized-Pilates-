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

    private val trainerNameList = mutableListOf<String>()                   // for spinner
    private val trainerNameToIdMap = mutableMapOf<String, String>()        // name → id
    private val trainerIdToNameMap = mutableMapOf<String, String>()        // id → name
    private val lessonLevels = listOf("choose level...", "PILATES | beginners", "PILATES +| intermediate", "PILATES ++| advanced")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLessonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lessonId = intent.getStringExtra("lessonId") ?: return

        setupLessonLevelSpinner()
        setupPickers()
        loadTrainers {
            loadLesson()
        }

        binding.buttonSaveLesson.setOnClickListener {
            updateLesson()
        }
    }

    private fun setupLessonLevelSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lessonLevels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLessonLevel.adapter = adapter
    }

    private fun loadTrainers(onComplete: () -> Unit) {
        firestore.collection("trainers")
            .get()
            .addOnSuccessListener { snapshot ->
                trainerNameList.clear()
                trainerNameToIdMap.clear()
                trainerIdToNameMap.clear()

                for (doc in snapshot) {
                    val name = doc.getString("name")
                    val id = doc.id
                    if (!name.isNullOrEmpty()) {
                        trainerNameList.add(name)
                        trainerNameToIdMap[name] = id
                        trainerIdToNameMap[id] = name
                    }
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    trainerNameList
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerTrainer.adapter = adapter

                onComplete()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load trainers", Toast.LENGTH_SHORT).show()
                finish()
            }
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
                    val titleIndex = lessonLevels.indexOf(lesson.title)
                    if (titleIndex != -1) {
                        binding.spinnerLessonLevel.setSelection(titleIndex)
                    }

                    binding.editDate.setText(lesson.schedule.date)
                    binding.editTime.setText(lesson.schedule.time)
                    binding.editCapacity.setText(lesson.capacity.toString())

                    // get trainer name from ID
                    val trainerName = trainerIdToNameMap[lesson.trainerId]
                    val trainerIndex = trainerNameList.indexOf(trainerName)
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
        val title = binding.spinnerLessonLevel.selectedItem.toString()
        val date = binding.editDate.text.toString().trim()
        val time = binding.editTime.text.toString().trim()
        val capacityStr = binding.editCapacity.text.toString().trim()
        val trainerName = binding.spinnerTrainer.selectedItem?.toString()?.trim() ?: ""
        val trainerId = trainerNameToIdMap[trainerName]

        if (title == "choose level..." || date.isEmpty() || time.isEmpty() ||
            capacityStr.isEmpty() || trainerId.isNullOrEmpty()
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
            capacity = capacity,
            bookedCount = 0,
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
