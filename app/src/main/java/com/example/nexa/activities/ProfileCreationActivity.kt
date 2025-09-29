package com.example.nexa.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nexa.R
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class ProfileCreationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if profile is already created
        val prefs = getSharedPreferences("nexa_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", null)
        if (username != null) {
            // Profile exists, go to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_profile_creation)

        val usernameInput = findViewById<EditText>(R.id.username_input)
        val bioInput = findViewById<EditText>(R.id.bio_input)
        val saveButton = findViewById<Button>(R.id.save_button)

        saveButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val bio = bioInput.text.toString().trim()

            if (username.isEmpty()) {
                Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save profile to SharedPreferences
            prefs.edit().apply {
                putString("username", username)
                putString("bio", bio)
                apply()
            }

            Toast.makeText(this, "Profile created", Toast.LENGTH_SHORT).show()

            // Navigate to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}