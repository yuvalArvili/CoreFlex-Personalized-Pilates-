package com.example.coreflexpilates.ui.login

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.coreflexpilates.R
import com.example.coreflexpilates.MainActivity
import com.example.coreflexpilates.AdminActivity
import com.example.coreflexpilates.ui.register.RegisterFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var forgotPasswordText: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        auth = FirebaseAuth.getInstance()

        emailEdit = view.findViewById(R.id.editTextEmail)
        passwordEdit = view.findViewById(R.id.editTextPassword)
        forgotPasswordText = view.findViewById(R.id.textForgotPassword) // Make sure this TextView exists in XML

        // Login button
        view.findViewById<Button>(R.id.buttonLogin).setOnClickListener {
            loginUser()
        }

        // Navigate to RegisterFragment
        view.findViewById<TextView>(R.id.textRegister).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.auth_fragment_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        // Forgot password
        forgotPasswordText.setOnClickListener {
            showResetPasswordDialog()
        }

        return view
    }

    // Show a dialog to enter email for password reset
    private fun showResetPasswordDialog() {
        val emailInput = EditText(requireContext())
        emailInput.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        emailInput.hint = "Enter your email"

        AlertDialog.Builder(requireContext())
            .setTitle("Reset Password")
            .setMessage("Enter your email to receive a password reset link")
            .setView(emailInput)
            .setPositiveButton("Send") { dialog, _ ->
                val email = emailInput.text.toString().trim()
                if (email.isNotEmpty()) {
                    // Send password reset email with Firebase Auth
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(requireContext(), "Reset email sent", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Failed to send reset email", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(requireContext(), "Email cannot be empty", Toast.LENGTH_SHORT).show()
                    Log.e("ResetPassword", "Error sending reset email: empty email")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // login with email and password
    private fun loginUser() {
        val email = emailEdit.text.toString()
        val password = passwordEdit.text.toString()

        // validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Sign in with Firebase Authentication
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                val firestore = FirebaseFirestore.getInstance()

                // Update user's FCM token for push notifications
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        firestore.collection("users").document(uid)
                            .update("fcmToken", token)
                    }

                // Fetch user role from Firestore
                firestore.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val role = doc.getString("role")
                        if (role == "admin") {
                            startActivity(Intent(requireContext(), AdminActivity::class.java))
                        } else {
                            startActivity(Intent(requireContext(), MainActivity::class.java))
                        }
                        requireActivity().finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
