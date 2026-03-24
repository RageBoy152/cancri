/* Cancri - money management app
Programming for Mobile - COMP08068
Team - Matt Miller, Kyle McNamee, Jaimie Neilson, Andrew Gilmour
Date created - 24/03/26
Ver 1.0
 */

package com.example.cancri

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash)

        val logoImage  = findViewById<ImageView>(R.id.splashLogoImage)
        val logoText   = findViewById<TextView>(R.id.splashLogoText)
        val container  = findViewById<LinearLayout>(R.id.splashContainer)

        // Make sure both start invisible
        logoImage.alpha = 0f
        logoText.alpha  = 0f

        // ── Step 1: Logo fades in after 300ms ────────────────────────────
        val logoFadeIn = ObjectAnimator.ofFloat(logoImage, View.ALPHA, 0f, 1f).apply {
            duration  = 1000
            startDelay = 300
        }

        // ── Step 2: Text fades in after 900ms ────────────────────────────
        val textFadeIn = ObjectAnimator.ofFloat(logoText, View.ALPHA, 0f, 1f).apply {
            duration  = 900
            startDelay = 900
        }

        // Play both (they run on their own startDelays, no need to sequence)
        AnimatorSet().apply {
            playTogether(logoFadeIn, textFadeIn)
            start()
        }

        // ── Step 3: After 3s, fade out the whole container then navigate ─
        Handler(Looper.getMainLooper()).postDelayed({
            ObjectAnimator.ofFloat(container, View.ALPHA, 1f, 0f).apply {
                duration = 600
                start()
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        startActivity(Intent(this@SplashActivity, Onboarding1Activity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                })
            }
        }, 3000)
    }
}