package com.example.cancri

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cancri.data.UserPreferences
import com.example.cancri.notifications.NotificationScheduler
import java.util.Locale

class Onboarding3Activity : AppCompatActivity() {

    private lateinit var billsAmountInput: EditText
    private lateinit var debtsAmountInput: EditText
    private lateinit var savingsAmountInput: EditText
    private var isUpdatingAmountText = false
    private lateinit var currencySymbol: String
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_onboarding3)
        userPreferences = UserPreferences(this)
        currencySymbol = userPreferences.getCurrencySymbol()

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

        billsAmountInput = findViewById(R.id.billsAmount)
        debtsAmountInput = findViewById(R.id.debtsAmount)
        savingsAmountInput = findViewById(R.id.savingsAmount)
        billsAmountInput.hint = "${currencySymbol}0.00"
        debtsAmountInput.hint = "${currencySymbol}0.00"
        savingsAmountInput.hint = "${currencySymbol}0.00"
        val btnFinish = findViewById<Button>(R.id.btnFinish)

        attachCurrencyValidation(billsAmountInput)
        attachCurrencyValidation(debtsAmountInput)
        attachCurrencyValidation(savingsAmountInput)

        loadGoalsFromPrefs()

        btnFinish.setOnClickListener {
            if (!validateAmountInput(billsAmountInput, "Monthly Bills")) return@setOnClickListener
            if (!validateAmountInput(debtsAmountInput, "Monthly Debt Payoff Goal")) return@setOnClickListener
            if (!validateAmountInput(savingsAmountInput, "Monthly Savings Goal")) return@setOnClickListener

            saveGoalsToPrefs()
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(UserPreferences.KEY_ONBOARDING_COMPLETED, true)
                .apply()
            NotificationScheduler.scheduleDaily(this)
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    private fun attachCurrencyValidation(amountInput: EditText) {
        amountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingAmountText) return

                val normalizedAmount = normalizeAmountInput(s?.toString().orEmpty())
                val rendered = if (normalizedAmount.isEmpty()) "" else "$currencySymbol$normalizedAmount"
                val current = s?.toString().orEmpty()

                if (rendered != current) {
                    isUpdatingAmountText = true
                    amountInput.setText(rendered)
                    amountInput.setSelection(rendered.length)
                    isUpdatingAmountText = false
                }
            }
        })
    }

    private fun loadGoalsFromPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        billsAmountInput.setText("${currencySymbol}%.2f".format(Locale.UK, prefs.getFloat(KEY_GOAL_BILLS, DEFAULT_BILLS.toFloat())))
        debtsAmountInput.setText("${currencySymbol}%.2f".format(Locale.UK, prefs.getFloat(KEY_GOAL_DEBTS, DEFAULT_DEBTS.toFloat())))
        savingsAmountInput.setText("${currencySymbol}%.2f".format(Locale.UK, prefs.getFloat(KEY_GOAL_SAVINGS, DEFAULT_SAVINGS.toFloat())))
    }

    private fun validateAmountInput(input: EditText, label: String): Boolean {
        val amountText = input.text?.toString().orEmpty()
        val normalizedAmount = normalizeAmountInput(amountText)
        if (normalizedAmount.isEmpty() || !isValidAmount(normalizedAmount)) {
            Toast.makeText(this, "Please enter a valid amount for $label", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveGoalsToPrefs() {
        val bills = normalizeAmountInput(billsAmountInput.text?.toString().orEmpty()).toDoubleOrNull() ?: DEFAULT_BILLS
        val debts = normalizeAmountInput(debtsAmountInput.text?.toString().orEmpty()).toDoubleOrNull() ?: DEFAULT_DEBTS
        val savings = normalizeAmountInput(savingsAmountInput.text?.toString().orEmpty()).toDoubleOrNull() ?: DEFAULT_SAVINGS

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putFloat(KEY_GOAL_BILLS, bills.toFloat())
            .putFloat(KEY_GOAL_DEBTS, debts.toFloat())
            .putFloat(KEY_GOAL_SAVINGS, savings.toFloat())
            .apply()
    }

    private fun normalizeAmountInput(input: String): String {
        val withoutSymbol = input.replace(currencySymbol, "").replace(",", "").trim()
        if (withoutSymbol.isEmpty()) return ""

        val builder = StringBuilder()
        var seenDot = false
        var decimals = 0

        withoutSymbol.forEach { char ->
            when {
                char.isDigit() && (!seenDot || decimals < 2) -> {
                    builder.append(char)
                    if (seenDot) decimals++
                }
                char == '.' && !seenDot -> {
                    if (builder.isEmpty()) builder.append('0')
                    builder.append('.')
                    seenDot = true
                }
            }
        }

        return builder.toString()
    }

    private fun isValidAmount(amount: String): Boolean {
        return amount.matches(Regex("^\\d+(\\.\\d{1,2})?$"))
    }

    companion object {
        private const val PREFS_NAME = "cancri_prefs"
        private const val KEY_GOAL_BILLS = "goal_bills"
        private const val KEY_GOAL_DEBTS = "goal_debts"
        private const val KEY_GOAL_SAVINGS = "goal_savings"

        private const val DEFAULT_BILLS = 900.0
        private const val DEFAULT_DEBTS = 200.0
        private const val DEFAULT_SAVINGS = 300.0
    }
}

