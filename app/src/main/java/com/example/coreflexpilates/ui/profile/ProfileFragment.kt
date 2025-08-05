package com.example.coreflexpilates.ui.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.coreflexpilates.AuthActivity
import com.example.coreflexpilates.R
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileFragment : Fragment() {

    companion object {
        private const val REQUEST_CODE_SELECT_IMAGE = 101 // Request code for image picker intent
    }

    private lateinit var editName: EditText
    private lateinit var spinnerLevel: Spinner
    private lateinit var buttonSave: Button
    private lateinit var buttonSelectImage: Button
    private lateinit var buttonDelete: ImageButton
    private lateinit var imageProfile: ShapeableImageView
    private lateinit var requestButton: Button
    private lateinit var buttonLogout: Button
    private lateinit var textSubscriptionType: TextView
    private lateinit var textSubscriptionExpiry: TextView
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null
    private var currentProfileImageUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        editName = view.findViewById(R.id.editName)
        spinnerLevel = view.findViewById(R.id.spinnerLevel)
        buttonSave = view.findViewById(R.id.buttonSave)
        buttonSelectImage = view.findViewById(R.id.buttonSelectImage)
        buttonDelete = view.findViewById(R.id.buttonDelete)
        imageProfile = view.findViewById(R.id.imageProfile)
        requestButton = view.findViewById(R.id.requestButton)
        buttonLogout = view.findViewById(R.id.buttonLogout)
        textSubscriptionType = view.findViewById(R.id.textSubscriptionType)
        textSubscriptionExpiry = view.findViewById(R.id.textSubscriptionExpiry)

        setupLevelSpinner()
        loadUserProfile()
        buttonSelectImage.setOnClickListener {
            openImagePicker()
        }

        // Handle delete image button click
        buttonDelete.setOnClickListener {
            selectedImageUri = null
            imageProfile.setImageResource(R.drawable.baseline_perm_identity_24)
            currentProfileImageUrl = ""

            Toast.makeText(requireContext(), "Profile image deleted locally. Save to apply changes.", Toast.LENGTH_SHORT).show()
        }

        buttonSave.setOnClickListener {
            saveUserProfile()
        }

        // Handle logout button click with dialog
        buttonLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(requireContext(), AuthActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Navigate to friend requests screen
        requestButton.setOnClickListener {
            findNavController().navigate(R.id.friendRequestsFragment)
        }

        return view
    }

    // Sets up the user level spinner with predefined levels
    private fun setupLevelSpinner() {
        val levels = arrayOf("Beginner", "Intermediate", "Advanced")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLevel.adapter = adapter
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    editName.setText(doc.getString("name") ?: "")

                    val level = doc.getString("level") ?: "Beginner"
                    val position = (spinnerLevel.adapter as ArrayAdapter<String>).getPosition(level)
                    spinnerLevel.setSelection(position)

                    currentProfileImageUrl = doc.getString("profileImageUrl")
                    if (!currentProfileImageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(currentProfileImageUrl)
                            .placeholder(R.drawable.baseline_perm_identity_24)
                            .circleCrop()
                            .into(imageProfile)
                    } else {
                        imageProfile.setImageResource(R.drawable.baseline_perm_identity_24)
                    }

                    val subscriptionType = doc.getString("subscriptionFrequency") ?: "No subscription"
                    textSubscriptionType.text = "Subscription Type: $subscriptionType"

                    val expiryTimestamp = doc.getTimestamp("subscriptionExpiry")
                    if (expiryTimestamp != null) {
                        val expiryDate = expiryTimestamp.toDate()
                        val formattedDate = android.text.format.DateFormat.format("dd/MM/yyyy", expiryDate)
                        textSubscriptionExpiry.text = "Subscription Expiry: $formattedDate"
                    } else {
                        textSubscriptionExpiry.text = "Subscription Expiry: N/A"
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        fun saveProfile(imageUrl: String?) {
            val data = hashMapOf(
                "name" to editName.text.toString(),
                "level" to spinnerLevel.selectedItem.toString(),
                "profileImageUrl" to (imageUrl ?: "")
            )

            firestore.collection("users").document(userId)
                .update(data as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Saved successfully!", Toast.LENGTH_SHORT).show()
                    currentProfileImageUrl = imageUrl
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
                }
        }

        if (selectedImageUri != null) {
            // Upload new image to Firebase Storage
            val ref = storage.reference.child("users/$userId/profile.jpg")
            ref.putFile(selectedImageUri!!)
                .continueWithTask { task -> ref.downloadUrl }
                .addOnSuccessListener { uri ->
                    saveProfile(uri.toString())
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
                }
        } else {
            // No new image selected, just update other profile data
            saveProfile(currentProfileImageUrl)
        }
    }

    // Opens image picker to select profile picture
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE)
    }

    // Handle image picker result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let {
                Glide.with(this)
                    .load(it)
                    .circleCrop()
                    .into(imageProfile)
            }
        }
    }
}
