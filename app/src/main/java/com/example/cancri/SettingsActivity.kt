/* Cancri - money management app
   Programming for Mobile - COMP08068
   Team - Matt Miller, Kyle McNamee, Jaimie Neilson, Andrew Gilmour
   Settings Activity
*/

package com.example.cancri

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("cancri_prefs", MODE_PRIVATE)

        setupBackButton()
        setupSaveAndReturnButton()
    }

    override fun onResume() {
        super.onResume()
        updateHero()
        setupProfileRow()
    }

    private fun updateHero() {
//        val heroName = findViewById<TextView>(R.id.settingsHeroName)
//        val avatar   = findViewById<TextView>(R.id.settingsAvatar)
        val nameRow  = findViewById<TextView>(R.id.inputUserName)

        val firstName = prefs.getString("user_first_name", "") ?: ""
        val lastName  = prefs.getString("user_last_name", "") ?: ""
        val color     = prefs.getString("avatar_color", "#52B788") ?: "#52B788"

        val initials = buildString {
            if (firstName.isNotEmpty()) append(firstName.first().uppercase())
            if (lastName.isNotEmpty()) append(lastName.first().uppercase())
        }

        val displayName = firstName.ifEmpty {
            prefs.getString("user_name", "there") ?: "there"
        }

//        heroName.text = displayName
//        avatar.text   = initials.ifEmpty { "?" }
        nameRow.text  = if (firstName.isEmpty()) "Tap to edit" else "$firstName $lastName".trim()

//        avatar.backgroundTintList = android.content.res.ColorStateList.valueOf(
//            android.graphics.Color.parseColor(color)
//        )
    }

    private fun setupProfileRow() {
        findViewById<LinearLayout>(R.id.settingsNameRow).setOnClickListener {
//            startActivity(Intent(this, EditProfileActivity::class.java)) TODO: error here??
        }
    }

    private fun setupBackButton() {
//        findViewById<Button>(R.id.btnBack).setOnClickListener {
//            finish()
//        }
    }

    private fun setupSaveAndReturnButton() {
        findViewById<Button>(R.id.btnSaveAndReturn).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}