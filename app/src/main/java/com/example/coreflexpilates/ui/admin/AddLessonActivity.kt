package com.example.coreflexpilates.ui.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.coreflexpilates.databinding.ActivityAddLessonBinding
import com.example.coreflexpilates.model.Lesson
import com.example.coreflexpilates.model.Schedule
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AddLessonActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddLessonBinding
    private val firestore = FirebaseFirestore.getInstance()

    private val lessonLevels = listOf(
        "choose level...",
        "PILATES | Beginners",
        "PILATES +| Intermediate",
        "PILATES ++| Advanced"
    )

    private val levelToSpecialtyMap = mapOf(
        "PILATES | Beginners" to "Beginners",
        "PILATES +| Intermediate" to "Intermediate",
        "PILATES ++| Advanced" to "Advanced"
    )

    private val trainerMap = mutableMapOf<String, List<String>>()
    private val trainerIdMap = mutableMapOf<String, String>()
    private val trainerNamesAll = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLessonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Show back arrow in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Setup the lesson level spinner and load trainers
        setupLessonLevelSpinner()
        loadTrainersFromFirestore()

        // Set click listeners for date/time pickers and save button
        binding.editDate.setOnClickListener { showDatePicker() }
        binding.editTime.setOnClickListener { showTimePicker() }
        binding.buttonSaveLesson.setOnClickListener { saveLesson() }
    }

    // Initialize lesson level spinner with levels and listener to update trainers
    private fun setupLessonLevelSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            lessonLevels
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLessonLevel.adapter = adapter

        // When a level is selected update trainer spinner
        binding.spinnerLessonLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLevel = lessonLevels[position]
                updateTrainerSpinner(selectedLevel)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // Loads trainers from Firestore
    private fun loadTrainersFromFirestore() {
        firestore.collection("trainers")
            .get()
            .addOnSuccessListener { snapshot ->
                trainerMap.clear()
                trainerIdMap.clear()
                for (doc in snapshot) {
                    val name = doc.getString("name")
                    val specialties = doc.get("specialties") as? List<String> ?: emptyList()
                    if (!name.isNullOrEmpty()) {
                        trainerMap[name] = specialties
                        trainerIdMap[name] = doc.id
                    }
                }
                updateTrainerSpinner("choose level...")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load trainers", Toast.LENGTH_SHORT).show()
            }
    }

    // Updates trainer spinner
    private fun updateTrainerSpinner(selectedLevel: String) {
        trainerNamesAll.clear()
        // Default choice in spinner
        trainerNamesAll.add("choose trainer...")

        if (selectedLevel != "choose level...") {
            val requiredSpecialty = levelToSpecialtyMap[selectedLevel]
            if (requiredSpecialty != null) {
                // Filter trainers who have the required specialty for the selected level
                val filtered = trainerMap.filterValues { it.contains(requiredSpecialty) }.keys
                trainerNamesAll.addAll(filtered.sorted())
            }
        }

        val trainerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            trainerNamesAll
        )
        trainerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTrainer.adapter = trainerAdapter
    }

    // Show date picker dialog
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, y, m, d -> binding.editDate.setText("%04d-%02d-%02d".format(y, m + 1, d)) },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    // Show time picker dialog
    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val dialog = TimePickerDialog(
            this,
            { _, h, m -> binding.editTime.setText("%02d:%02d".format(h, m)) },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        dialog.show()
    }

    // Validates inputs and saves the new lesson in Firestore
    private fun saveLesson() {
        val title = binding.spinnerLessonLevel.selectedItem.toString()
        val date = binding.editDate.text.toString().trim()
        val time = binding.editTime.text.toString().trim()
        val capacityStr = binding.editCapacity.text.toString().trim()
        val trainerName = binding.spinnerTrainer.selectedItem?.toString()?.trim() ?: ""

        if (title == "choose level..." || trainerName == "choose trainer..." ||
            date.isEmpty() || time.isEmpty() || capacityStr.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val capacity = capacityStr.toIntOrNull()
        if (capacity == null || capacity <= 0) {
            Toast.makeText(this, "Capacity must be a positive number", Toast.LENGTH_SHORT).show()
            return
        }

        val trainerId = trainerIdMap[trainerName]
        if (trainerId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid trainer selected", Toast.LENGTH_SHORT).show()
            return
        }

        val classId = UUID.randomUUID().toString()
        val schedule = Schedule(date = date, day = "", time = time)

        val lesson = Lesson(
            title = title,
            schedule = schedule,
            capacity = capacity,
            bookedCount = 0,
            trainerId = trainerId,
            classId = classId
        )

        // Save the lesson in Firestore lessons collection
        firestore.collection("lessons")
            .document(classId)
            .set(lesson)
            .addOnSuccessListener {
                Toast.makeText(this, "Lesson added successfully", Toast.LENGTH_SHORT).show()
                finish()  // Close activity on success
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add lesson", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
