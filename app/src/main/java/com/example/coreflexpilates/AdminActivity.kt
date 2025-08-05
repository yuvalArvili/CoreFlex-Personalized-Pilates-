package com.example.coreflexpilates

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.coreflexpilates.databinding.ActivityAdminBinding
import com.example.coreflexpilates.ui.admin.AddLessonActivity
import com.example.coreflexpilates.ui.admin.AddTrainerActivity

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Get NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_admin) as NavHostFragment
        val navController = navHostFragment.navController

        // navigate to the home fragment with isAdmin flag set to true
        if (savedInstanceState == null) {
            navController.navigate(R.id.navigation_home, Bundle().apply { putBoolean("isAdmin", true) })
        }

        // Set up bottom navigation item selection listener to navigate between admin fragments
        binding.adminBottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val bundle = Bundle().apply { putBoolean("isAdmin", true) }
                    navController.navigate(R.id.navigation_home, bundle)
                    true
                }
                R.id.nav_trainers -> {
                    navController.navigate(R.id.adminTrainerListFragment)
                    true
                }
                R.id.nav_users -> {
                    navController.navigate(R.id.adminUsersFragment)
                    true
                }
                else -> false
            }
        }

        //add new lesson
        binding.fabAddLesson.setOnClickListener {
            startActivity(Intent(this, AddLessonActivity::class.java))
        }

        // add new trainer
        binding.buttonAddTrainer.setOnClickListener {
            startActivity(Intent(this, AddTrainerActivity::class.java))
        }

        // Logout button
        binding.buttonLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    //dialog to confirm logout action
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                // On confirmation, navigate to AuthActivity and finish current activity
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
