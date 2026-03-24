/* Cancri - money management app
Programming for Mobile - COMP08068
Team - Matt Miller, Kyle McNamee, Jaimie Neilson, Andrew Gilmour
Date created - 24/03/26
Ver 1.0
 */

package com.example.cancri  // ← Change this to your actual package name

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

        // Slide up from off-screen bottom
        val slideUp = TranslateAnimation(
            0f, 0f,          // X: no horizontal movement
            800f, 0f         // Y: start 800px below, end at natural position
        ).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
        }

        // Fade in at the same time
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 700
        }

        // Combine both into one AnimationSet
        val animSet = AnimationSet(true).apply {
            addAnimation(slideUp)
            addAnimation(fadeIn)
            startOffset = 200   // slight delay so the screen background loads first
            fillAfter = true
        }

        card.startAnimation(animSet)

        val etName = findViewById<EditText>(R.id.etName)
        val btnNext = findViewById<Button>(R.id.btnNext)

        btnNext.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Pass the name to Onboarding3
            val intent = Intent(this, Onboarding3Activity::class.java)
            intent.putExtra("USER_NAME", name)
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}