package com.example.coreflexpilates

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.coreflexpilates.ui.login.LoginFragment

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_auth)
        supportActionBar?.hide()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.auth_fragment_container, LoginFragment())
                .commit()
        }
    }

}
