package com.example.coreflexpilates

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.coreflexpilates.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.fragment_home)
        supportActionBar?.hide()

        FirebaseApp.initializeApp(this)

        auth = FirebaseAuth.getInstance()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

        val logoutButton = findViewById<Button>(R.id.buttonLogout)


        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            // Go back to login/register screen
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish() // prevent going back with back button
        }


    }
}
