package com.example.coreflexpilates

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.coreflexpilates.databinding.ActivityAdminBinding
import com.example.coreflexpilates.ui.admin.AddLessonActivity
import com.example.coreflexpilates.ui.admin.AddTrainerActivity
import com.example.coreflexpilates.ui.admin.AdminTrainerListFragment
import com.example.coreflexpilates.ui.home.HomeFragment

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment.newInstance(true))
            .commit()

        binding.fabAddLesson.setOnClickListener {
            startActivity(Intent(this, AddLessonActivity::class.java))
        }

        binding.buttonAddTrainer.setOnClickListener {
            startActivity(Intent(this, AddTrainerActivity::class.java))
        }

        binding.buttonLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.adminBottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, HomeFragment.newInstance(true))
                        .commit()
                    true
                }
                R.id.nav_trainers -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, AdminTrainerListFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
