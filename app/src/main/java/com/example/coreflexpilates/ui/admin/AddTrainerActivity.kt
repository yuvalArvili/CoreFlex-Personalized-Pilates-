package com.example.coreflexpilates.ui.admin

import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.coreflexpilates.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class AddTrainerActivity : AppCompatActivity() {

    private lateinit var editTrainerName: EditText
    private lateinit var editTrainerEmail: EditText
    private lateinit var checkboxBeginners: CheckBox
    private lateinit var checkboxIntermediate: CheckBox
    private lateinit var checkboxAdvanced: CheckBox

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_trainer)

        supportActionBar?.title = "Add New Trainer"

        editTrainerName = findViewById(R.id.editTrainerName)
        editTrainerEmail = findViewById(R.id.editTrainerEmail)
        checkboxBeginners = findViewById(R.id.checkboxBeginners)
        checkboxIntermediate = findViewById(R.id.checkboxIntermediate)
        checkboxAdvanced = findViewById(R.id.checkboxAdvanced)

        val buttonSave = findViewById<android.widget.Button>(R.id.buttonSaveTrainer)
        buttonSave.setOnClickListener {
            saveTrainer()
        }
    }

    private fun saveTrainer() {
        val name = editTrainerName.text.toString().trim()
        val email = editTrainerEmail.text.toString().trim()
        val specialties = mutableListOf<String>()

        if (checkboxBeginners.isChecked) specialties.add("Beginners")
        if (checkboxIntermediate.isChecked) specialties.add("Intermediate")
        if (checkboxAdvanced.isChecked) specialties.add("Advanced")

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (specialties.isEmpty()) {
            Toast.makeText(this, "Please select at least one specialty", Toast.LENGTH_SHORT).show()
            return
        }

        val trainerId = UUID.randomUUID().toString()
        val trainerData = hashMapOf(
            "id" to trainerId,
            "name" to name,
            "email" to email,
            "specialties" to specialties
        )

        firestore.collection("trainers")
            .document(trainerId)
            .set(trainerData)
            .addOnSuccessListener {
                Toast.makeText(this, "Trainer saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save trainer", Toast.LENGTH_SHORT).show()
            }
    }
}
