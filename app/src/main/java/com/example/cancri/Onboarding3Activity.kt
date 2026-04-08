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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Onboarding3Activity : AppCompatActivity() {

    private val selectedGoals = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_onboarding3)

        // ── Slide up animation ────────────────────────────────────────────
        val card = findViewById<LinearLayout>(R.id.onboarding3Card)
        val slideUp = TranslateAnimation(0f, 0f, 800f, 0f).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
        }
        val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 700 }
        val animSet = AnimationSet(true).apply {
            addAnimation(slideUp)
            addAnimation(fadeIn)
            startOffset = 200
            fillAfter = true
        }
        card.startAnimation(animSet)

        // ── Shield button references ──────────────────────────────────────
        val btnSaveMoney     = findViewById<LinearLayout>(R.id.btnSaveMoney)
        val btnTrackSpending = findViewById<LinearLayout>(R.id.btnTrackSpending)
        val btnInvestGrow    = findViewById<LinearLayout>(R.id.btnInvestGrow)
        val btnBudgetBetter  = findViewById<LinearLayout>(R.id.btnBudgetBetter)

        val imgSaveMoney     = findViewById<ImageView>(R.id.imgSaveMoney)
        val imgTrackSpending = findViewById<ImageView>(R.id.imgTrackSpending)
        val imgInvestGrow    = findViewById<ImageView>(R.id.imgInvestGrow)
        val imgBudgetBetter  = findViewById<ImageView>(R.id.imgBudgetBetter)

        // Map each button container to its goal name and shield image
        val goalMap = mapOf(
            btnSaveMoney     to Pair("Save Money",     imgSaveMoney),
            btnTrackSpending to Pair("Track Spending", imgTrackSpending),
            btnInvestGrow    to Pair("Invest & Grow",  imgInvestGrow),
            btnBudgetBetter  to Pair("Budget Better",  imgBudgetBetter)
        )

        goalMap.forEach { (container, pair) ->
            val (goalName, imageView) = pair
            container.setOnClickListener {
                if (selectedGoals.contains(goalName)) {
                    selectedGoals.remove(goalName)
                    imageView.alpha = 1f
                    container.alpha = 0.6f
                } else {
                    selectedGoals.add(goalName)
                    imageView.alpha = 1f
                    container.alpha = 1f
                    container.animate().scaleX(1.08f).scaleY(1.08f).setDuration(100)
                        .withEndAction {
                            container.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        }.start()
                }
            }
            container.alpha = 0.6f
        }

        // ── Navigation ───────────────────────────────────────────────────
        val btnFinish = findViewById<Button>(R.id.btnFinish)

        btnFinish.setOnClickListener {
            if (selectedGoals.isEmpty()) {
                Toast.makeText(this, "Please select at least one goal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // ── Navigate to WelcomeActivity instead of MainActivity ──
            // Name is already saved in SharedPreferences by Onboarding2Activity.
            // Goals can be saved here if needed in future.
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }
}