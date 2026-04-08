/* Cancri - money management app
Programming for Mobile - COMP08068
Team - Matt Miller, Kyle McNamee, Jaimie Neilson, Andrew Gilmour
Date created - 24/03/26
Ver 1.0
Ver 1.1 Created 08/04/26
 */


package com.example.cancri

import android.content.Intent
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class Onboarding1Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_onboarding1)

        val card = findViewById<LinearLayout>(R.id.onboarding1Card)

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

        // Navigate to Onboarding 2 on button tap
        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)
        btnGetStarted.setOnClickListener {
            startActivity(Intent(this, Onboarding2Activity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}