package com.example.coreflexpilates.ui.register

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.coreflexpilates.R
import com.example.coreflexpilates.MainActivity
import com.example.coreflexpilates.ui.login.LoginFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val nameEdit = view.findViewById<EditText>(R.id.editTextName)
        val emailEdit = view.findViewById<EditText>(R.id.editTextEmail)
        val passwordEdit = view.findViewById<EditText>(R.id.editTextPassword)
        val confirmEdit = view.findViewById<EditText>(R.id.editTextConfirmPassword)
        val buttonRegister = view.findViewById<Button>(R.id.buttonRegister)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        buttonRegister.setOnClickListener {
            val name = nameEdit.text.toString().trim()
            val email = emailEdit.text.toString().trim()
            val password = passwordEdit.text.toString()
            val confirm = confirmEdit.text.toString()

            // Validate input fields
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            buttonRegister.isEnabled = false

            // Create new user with Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val uid = auth.currentUser?.uid
                    if (uid == null) {
                        Toast.makeText(requireContext(), "Registration failed (no UID)", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        buttonRegister.isEnabled = true
                        return@addOnSuccessListener
                    }

                    // Get Firebase Cloud Messaging token for push notifications
                    FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { token ->
                            val userData = hashMapOf(
                                "uid" to uid,
                                "name" to name,
                                "email" to email,
                                "fcmToken" to token
                            )

                            // Save user data in Firestore under 'users' collection
                            firestore.collection("users").document(uid).set(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(requireContext(), MainActivity::class.java))
                                    requireActivity().finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Firestore save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    progressBar.visibility = View.GONE
                                    buttonRegister.isEnabled = true
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Token error: ${e.message}", Toast.LENGTH_SHORT).show()
                            progressBar.visibility = View.GONE
                            buttonRegister.isEnabled = true
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Auth failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    buttonRegister.isEnabled = true
                }
        }

        // Link to switch to LoginFragment
        view.findViewById<TextView>(R.id.textLogin).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.auth_fragment_container, LoginFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}
