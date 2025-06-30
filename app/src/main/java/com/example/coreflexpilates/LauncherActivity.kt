package com.example.coreflexpilates

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // the user already connected
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // the user not connected
            startActivity(Intent(this, AuthActivity::class.java))
        }

        finish() // cant go back
    }
}
