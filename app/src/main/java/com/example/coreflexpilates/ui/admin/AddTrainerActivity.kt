package com.example.coreflexpilates.ui.admin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.coreflexpilates.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AddTrainerActivity : AppCompatActivity() {

    private lateinit var editTrainerName: EditText
    private lateinit var editTrainerEmail: EditText
    private lateinit var checkboxBeginners: CheckBox
    private lateinit var checkboxIntermediate: CheckBox
    private lateinit var checkboxAdvanced: CheckBox
    private lateinit var imageTrainer: ImageView
    private lateinit var buttonSelectImage: Button

    private var selectedImageUri: Uri? = null

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_trainer)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editTrainerName = findViewById(R.id.editTrainerName)
        editTrainerEmail = findViewById(R.id.editTrainerEmail)
        checkboxBeginners = findViewById(R.id.checkboxBeginners)
        checkboxIntermediate = findViewById(R.id.checkboxIntermediate)
        checkboxAdvanced = findViewById(R.id.checkboxAdvanced)
        imageTrainer = findViewById(R.id.imageTrainer)
        buttonSelectImage = findViewById(R.id.buttonSelectImage)

        val buttonSave = findViewById<Button>(R.id.buttonSaveTrainer)

        buttonSelectImage.setOnClickListener {
            openGallery()
        }

        buttonSave.setOnClickListener {
            saveTrainer()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            imageTrainer.setImageURI(selectedImageUri)
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

        if (selectedImageUri != null) {
            val imageRef = storage.reference.child("trainer_images/$trainerId.jpg")
            imageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        saveTrainerToFirestore(trainerId, name, email, specialties, uri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveTrainerToFirestore(trainerId, name, email, specialties, null)
        }
    }

    private fun saveTrainerToFirestore(
        trainerId: String,
        name: String,
        email: String,
        specialties: List<String>,
        imageUrl: String?
    ) {
        val trainerData = hashMapOf(
            "id" to trainerId,
            "name" to name,
            "email" to email,
            "specialties" to specialties,
            "imageUrl" to imageUrl
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
