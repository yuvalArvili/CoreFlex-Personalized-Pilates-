package com.example.coreflexpilates.ui.admin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.Trainer
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditTrainerActivity : AppCompatActivity() {

    private lateinit var imageTrainer: ShapeableImageView
    private lateinit var buttonSelectImage: Button
    private lateinit var buttonDeleteImage: ImageButton
    private lateinit var editTrainerName: EditText
    private lateinit var editTrainerEmail: EditText
    private lateinit var checkboxBeginners: CheckBox
    private lateinit var checkboxIntermediate: CheckBox
    private lateinit var checkboxAdvanced: CheckBox
    private lateinit var buttonSave: Button

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var selectedImageUri: Uri? = null
    private var imageDeleted: Boolean = false

    private lateinit var trainerId: String
    private lateinit var currentTrainer: Trainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_trainer) // Reusing AddTrainer layout for editing

        // Show back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        trainerId = intent.getStringExtra("trainerId") ?: return finish()

        imageTrainer = findViewById(R.id.imageTrainer)
        buttonSelectImage = findViewById(R.id.buttonSelectImage)
        buttonDeleteImage = findViewById(R.id.buttonDelete)
        editTrainerName = findViewById(R.id.editTrainerName)
        editTrainerEmail = findViewById(R.id.editTrainerEmail)
        checkboxBeginners = findViewById(R.id.checkboxBeginners)
        checkboxIntermediate = findViewById(R.id.checkboxIntermediate)
        checkboxAdvanced = findViewById(R.id.checkboxAdvanced)
        buttonSave = findViewById(R.id.buttonSaveTrainer)

        // Load existing trainer data
        loadTrainerData()

        // Open image picker for selecting new profile image
        buttonSelectImage.setOnClickListener {
            selectImage()
        }

        //Delete current image locally and update UI
        buttonDeleteImage.setOnClickListener {
            selectedImageUri = null
            imageDeleted = true
            imageTrainer.setImageResource(R.drawable.baseline_perm_identity_24) // default placeholder
            buttonDeleteImage.visibility = ImageButton.GONE
        }

        // Save changes to Firestore
        buttonSave.setOnClickListener {
            updateTrainer()
        }
    }

    // Fetch trainer document by trainerId
    private fun loadTrainerData() {
        db.collection("trainers").document(trainerId).get()
            .addOnSuccessListener { doc ->
                val trainer = doc.toObject(Trainer::class.java)
                if (trainer != null) {
                    currentTrainer = trainer
                    editTrainerName.setText(trainer.name)
                    editTrainerEmail.setText(trainer.email)

                    // Set specialties checkboxes according to saved data
                    checkboxBeginners.isChecked = "Beginners" in trainer.specialties
                    checkboxIntermediate.isChecked = "Intermediate" in trainer.specialties
                    checkboxAdvanced.isChecked = "Advanced" in trainer.specialties

                    // Load image from URL or show placeholder
                    if (!trainer.imageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(trainer.imageUrl)
                            .placeholder(R.drawable.baseline_perm_identity_24)
                            .circleCrop()
                            .into(imageTrainer)
                        buttonDeleteImage.visibility = ImageButton.VISIBLE
                    } else {
                        imageTrainer.setImageResource(R.drawable.baseline_perm_identity_24)
                        buttonDeleteImage.visibility = ImageButton.GONE
                    }
                }
            }
    }

    private fun updateTrainer() {
        val name = editTrainerName.text.toString().trim()
        val email = editTrainerEmail.text.toString().trim()

        val specialties = mutableListOf<String>()
        if (checkboxBeginners.isChecked) specialties.add("Beginners")
        if (checkboxIntermediate.isChecked) specialties.add("Intermediate")
        if (checkboxAdvanced.isChecked) specialties.add("Advanced")

        // Basic validation to ensure name and email are not empty
        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        fun saveTrainer(imageUrl: String) {
            val updatedTrainer = Trainer(
                id = trainerId,
                name = name,
                email = email,
                specialties = specialties,
                imageUrl = imageUrl
            )
            db.collection("trainers").document(trainerId).set(updatedTrainer)
                .addOnSuccessListener {
                    Toast.makeText(this, "Trainer updated", Toast.LENGTH_SHORT).show()
                    finish() // Close activity after successful update
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                }
        }

        // If a new image was selected
        if (selectedImageUri != null) {
            val ref = storage.reference.child("trainers/$trainerId.jpg")
            ref.putFile(selectedImageUri!!)
                .continueWithTask { task -> ref.downloadUrl }
                .addOnSuccessListener { uri ->
                    saveTrainer(uri.toString())
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                }
        } else if (imageDeleted) {
            // If image was deleted
            storage.reference.child("trainers/$trainerId.jpg").delete()
                .addOnSuccessListener {
                    saveTrainer("")
                }
                .addOnFailureListener {
                    // Even if deletion fails
                    saveTrainer("")
                }
        } else {
            // If no image change
            saveTrainer(currentTrainer.imageUrl ?: "")
        }
    }

    // Pick an image from device storage
    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, 101)
    }

    // Handles image selection result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            imageDeleted = false
            Glide.with(this)
                .load(selectedImageUri)
                .circleCrop()
                .into(imageTrainer)

            buttonDeleteImage.visibility = ImageButton.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
