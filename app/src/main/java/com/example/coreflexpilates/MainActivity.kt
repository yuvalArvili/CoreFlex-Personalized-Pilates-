package com.example.coreflexpilates

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.coreflexpilates.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        // Get the currently signed-in user (if any)
        val currentUser = auth.currentUser
        val uid = currentUser?.uid

        // Log user ID and email for debug
        Log.d("USER_CHECK", "UID: ${currentUser?.uid}")
        Log.d("USER_CHECK", "Email: ${currentUser?.email}")

        if (uid != null) {
            // If user is signed in, fetch their document from Firestore 'users' collection
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    val role = doc.getString("role")
                    Log.d("USER_CHECK", "Role: $role")

                    if (role == "admin") {
                        startActivity(Intent(this, AdminActivity::class.java))
                        finish()
                    } else {
                        showUserNavigation()
                    }
                }
                .addOnFailureListener {
                    Log.e("USER_CHECK", "Failed to fetch role", it)
                    showUserNavigation()
                }
        } else {
            showUserNavigation()
        }
    }

    // Setup the bottom navigation view and navigation controller for regular users
    private fun showUserNavigation() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
    }
}
