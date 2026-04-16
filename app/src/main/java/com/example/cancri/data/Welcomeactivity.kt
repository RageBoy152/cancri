/* Cancri - money management app
   Programming for Mobile - COMP08068
   Team - Matt Miller, Kyle McNamee, Jaimie Neilson, Andrew Gilmour
   Date created - 24/03/26
   Ver 1.0

   WelcomeActivity — shown exactly once after onboarding completes.
   Reads the user's name from SharedPreferences, shows a time-aware greeting,
   then fades out and launches MainActivity.
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
import java.util.Calendar

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_welcome)

        val timeGreeting = findViewById<TextView>(R.id.welcomeTimeGreeting)
        val nameView     = findViewById<TextView>(R.id.welcomeName)
        val subView      = findViewById<TextView>(R.id.welcomeSub)
        val hintLayout   = findViewById<LinearLayout>(R.id.welcomeHint)
        val arrowView    = findViewById<ImageView>(R.id.welcomeArrow)

        // ── Populate name from SharedPreferences ─────────────────────────
        nameView.text = UserPreferences.getDisplayName(this)

        // ── Time-aware greeting ───────────────────────────────────────────
        timeGreeting.text = getTimeGreeting()

        // ── Fade-in animations ────────────────────────────────────────────
        val fadeGreeting = ObjectAnimator.ofFloat(timeGreeting, View.ALPHA, 0f, 1f).apply {
            duration   = 800
            startDelay = 300
        }
        val fadeName = ObjectAnimator.ofFloat(nameView, View.ALPHA, 0f, 1f).apply {
            duration   = 900
            startDelay = 700
        }
        val fadeSub = ObjectAnimator.ofFloat(subView, View.ALPHA, 0f, 1f).apply {
            duration   = 800
            startDelay = 1100
        }
        val fadeHint = ObjectAnimator.ofFloat(hintLayout, View.ALPHA, 0f, 1f).apply {
            duration   = 800
            startDelay = 1600
        }

        AnimatorSet().apply {
            playTogether(fadeGreeting, fadeName, fadeSub, fadeHint)
            start()
        }

        // ── Bouncing arrow ────────────────────────────────────────────────
        Handler(Looper.getMainLooper()).postDelayed({
            ObjectAnimator.ofFloat(arrowView, View.TRANSLATION_Y, 0f, 10f).apply {
                duration    = 700
                repeatCount = ObjectAnimator.INFINITE
                repeatMode  = ObjectAnimator.REVERSE
                start()
            }
        }, 2000)

        // ── After 3s fade out everything and go to MainActivity ───────────
        Handler(Looper.getMainLooper()).postDelayed({
            val rootView = findViewById<View>(android.R.id.content)
            ObjectAnimator.ofFloat(rootView, View.ALPHA, 1f, 0f).apply {
                duration = 600
                start()
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        startActivity(
                            Intent(this@WelcomeActivity, MainActivity::class.java)
                        )
                        overridePendingTransition(0, 0)
                        finish()
                    }
                })
            }
        }, 3000)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun getTimeGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11  -> "Good morning,"
            in 12..17 -> "Good afternoon,"
            else      -> "Good evening,"
        }
    }
}
