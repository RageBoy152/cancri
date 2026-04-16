package com.example.cancri

import android.content.Intent
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Onboarding2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_onboarding2)

        val card = findViewById<LinearLayout>(R.id.onboarding2Card)

        val slideUp = TranslateAnimation(0f, 0f, 800f, 0f).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
        }

        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 700
        }

        val animSet = AnimationSet(true).apply {
            addAnimation(slideUp)
            addAnimation(fadeIn)
            startOffset = 200
            fillAfter = true
        }

        card.startAnimation(animSet)

        val etName = findViewById<EditText>(R.id.etName)
        val btnNext = findViewById<Button>(R.id.btnNext)

        val prefs = getSharedPreferences("cancri_prefs", MODE_PRIVATE)
        val existingName = prefs.getString("user_name", "") ?: ""
        if (existingName.isNotBlank()) {
            etName.setText(existingName)
            etName.setSelection(existingName.length)
        }

        btnNext.setOnClickListener {
            val name = etName.text?.toString()?.trim().orEmpty()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString("user_name", name)
                .putString("user_first_name", name)
                .putString("user_last_name", "")
                .apply()

            startActivity(Intent(this, Onboarding3Activity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}

