/* Cancri - money management app
   Programming for Mobile - COMP08068
   Team - Matt Miller, Kyle McNamee, Jaimie Neilson, Andrew Gilmour
   Settings Activity
*/

package com.example.cancri

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("cancri_prefs", MODE_PRIVATE)

        setupNameField()
        setupBackButton()
    }

    private fun setupNameField() {
        val nameInput = findViewById<TextInputEditText>(R.id.inputUserName)

        // Load the saved name so the field isn't blank when they open settings
        val savedName = prefs.getString("user_name", "") ?: ""
        nameInput.setText(savedName)

        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            val newName = nameInput.text?.toString()?.trim()

            if (newName.isNullOrEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit().putString("user_name", newName).apply()
            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
            finish() // Goes back to MainActivity
        }
    }

    private fun setupBackButton() {
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}