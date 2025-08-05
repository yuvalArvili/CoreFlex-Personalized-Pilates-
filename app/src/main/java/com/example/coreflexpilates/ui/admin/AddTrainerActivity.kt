package com.example.coreflexpilates.ui.admin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.coreflexpilates.R
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AddTrainerActivity : AppCompatActivity() {

    private lateinit var editTrainerName: EditText
    private lateinit var editTrainerEmail: EditText
    private lateinit var checkboxBeginners: CheckBox
    private lateinit var checkboxIntermediate: CheckBox
    private lateinit var checkboxAdvanced: CheckBox
    private lateinit var imageTrainer: ShapeableImageView
    private lateinit var buttonSelectImage: Button
    private lateinit var buttonDeleteImage: ImageButton

    private var selectedImageUri: Uri? = null
    private var currentImageUrl: String? = null

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_trainer)

        // Enable the back arrow in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editTrainerName = findViewById(R.id.editTrainerName)
        editTrainerEmail = findViewById(R.id.editTrainerEmail)
        checkboxBeginners = findViewById(R.id.checkboxBeginners)
        checkboxIntermediate = findViewById(R.id.checkboxIntermediate)
        checkboxAdvanced = findViewById(R.id.checkboxAdvanced)
        imageTrainer = findViewById(R.id.imageTrainer)
        buttonSelectImage = findViewById(R.id.buttonSelectImage)
        buttonDeleteImage = findViewById(R.id.buttonDelete)

        val buttonSave = findViewById<Button>(R.id.buttonSaveTrainer)

        buttonSelectImage.setOnClickListener {
            openImagePicker()
        }

        // Remove image locally and update UI
        buttonDeleteImage.setOnClickListener {
            selectedImageUri = null
            currentImageUrl = null
            imageTrainer.setImageResource(R.drawable.baseline_perm_identity_24)
            buttonDeleteImage.visibility = ImageButton.GONE
            Toast.makeText(this, "Image deleted locally. Save to apply.", Toast.LENGTH_SHORT).show()
        }

        // Save button click to upload and save trainer data
        buttonSave.setOnClickListener {
            saveTrainer()
        }
    }

    // Opens image picker to choose an image
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Store the selected image URI
            selectedImageUri = result.data?.data
            selectedImageUri?.let {
                // Load the selected image into the ImageView
                Glide.with(this)
                    .load(it)
                    .circleCrop()
                    .into(imageTrainer)
                // Make delete button visible now that an image is selected
                buttonDeleteImage.visibility = ImageButton.VISIBLE
            }
        }
    }

    // Validates input and saves the trainer data
    private fun saveTrainer() {
        val name = editTrainerName.text.toString().trim()
        val email = editTrainerEmail.text.toString().trim()
        val specialties = mutableListOf<String>()

        // Collect selected specialties
        if (checkboxBeginners.isChecked) specialties.add("Beginners")
        if (checkboxIntermediate.isChecked) specialties.add("Intermediate")
        if (checkboxAdvanced.isChecked) specialties.add("Advanced")

        // Validate required fields
        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Ensure at least one specialty is selected
        if (specialties.isEmpty()) {
            Toast.makeText(this, "Please select at least one specialty", Toast.LENGTH_SHORT).show()
            return
        }

        val trainerId = UUID.randomUUID().toString()

        // If an image is selected, upload it first
        if (selectedImageUri != null) {
            val imageRef = storage.reference.child("trainer_images/$trainerId.jpg")
            imageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    // After upload get the download URL
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        currentImageUrl = uri.toString()
                        // Save trainer data with image URL
                        saveTrainerToFirestore(trainerId, name, email, specialties, currentImageUrl)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                }
        } else {
            // If no image selected
            saveTrainerToFirestore(trainerId, name, email, specialties, currentImageUrl)
        }
    }

    // Saves the trainer data to Firestore
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
            "imageUrl" to (imageUrl ?: "")
        )

        firestore.collection("trainers")
            .document(trainerId)
            .set(trainerData)
            .addOnSuccessListener {
                Toast.makeText(this, "Trainer saved successfully", Toast.LENGTH_SHORT).show()
                finish()  // Close activity after success
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

