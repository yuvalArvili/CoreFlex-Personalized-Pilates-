package com.example.coreflexpilates.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.coreflexpilates.R
import com.example.coreflexpilates.MainActivity
import com.example.coreflexpilates.ui.register.RegisterFragment
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        auth = FirebaseAuth.getInstance()

        emailEdit = view.findViewById(R.id.editTextEmail)
        passwordEdit = view.findViewById(R.id.editTextPassword)

        view.findViewById<Button>(R.id.buttonLogin).setOnClickListener {
            loginUser()
        }

        view.findViewById<TextView>(R.id.textRegister).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.auth_fragment_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun loginUser() {
        val email = emailEdit.text.toString()
        val password = passwordEdit.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    requireActivity().finish()
                } else {
                    Toast.makeText(requireContext(), "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
