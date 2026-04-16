package com.example.cancri

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class GoalsActivity : AppCompatActivity() {

    private lateinit var billsAmountInput: EditText
    private lateinit var debtsAmountInput: EditText
    private lateinit var savingsAmountInput: EditText
    private var isUpdatingAmountText = false
    private lateinit var currencySymbol: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_goals)
        currencySymbol = UserPreferences.getCurrencySymbol(this)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        billsAmountInput = setupGoalRow(R.id.billsGoal, "Bills")
        debtsAmountInput = setupGoalRow(R.id.debtsGoal, "Debts")
        savingsAmountInput = setupGoalRow(R.id.savingsGoals, "Savings Goals")
        billsAmountInput.hint = "${currencySymbol}0.00"
        debtsAmountInput.hint = "${currencySymbol}0.00"
        savingsAmountInput.hint = "${currencySymbol}0.00"

        loadGoalsFromPrefs()

        findViewById<View>(R.id.btnSaveGoals).setOnClickListener {
            saveGoalsToPrefs()
            Toast.makeText(this, "Goals saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupGoalRow(rowId: Int, goalName: String): EditText {
        val row = findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.spendingCatHeading).text = goalName

        val amountInput = row.findViewById<EditText>(R.id.editAmount)
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

                onGoalInputEdited(goalName, rendered)
            }
        })
        return amountInput
    }

    private fun loadGoalsFromPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        billsAmountInput.setText("${currencySymbol}%.2f".format(Locale.UK, prefs.getFloat(KEY_GOAL_BILLS, DEFAULT_BILLS.toFloat())))
        debtsAmountInput.setText("${currencySymbol}%.2f".format(Locale.UK, prefs.getFloat(KEY_GOAL_DEBTS, DEFAULT_DEBTS.toFloat())))
        savingsAmountInput.setText("${currencySymbol}%.2f".format(Locale.UK, prefs.getFloat(KEY_GOAL_SAVINGS, DEFAULT_SAVINGS.toFloat())))
    }

    private fun saveGoalsToPrefs() {
        if (!validateAmountInput(billsAmountInput, "Bills")) return
        if (!validateAmountInput(debtsAmountInput, "Debts")) return
        if (!validateAmountInput(savingsAmountInput, "Savings Goals")) return

        val bills = parseGoalAmount(billsAmountInput.text?.toString(), DEFAULT_BILLS)
        val debts = parseGoalAmount(debtsAmountInput.text?.toString(), DEFAULT_DEBTS)
        val savings = parseGoalAmount(savingsAmountInput.text?.toString(), DEFAULT_SAVINGS)

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putFloat(KEY_GOAL_BILLS, bills.toFloat())
            .putFloat(KEY_GOAL_DEBTS, debts.toFloat())
            .putFloat(KEY_GOAL_SAVINGS, savings.toFloat())
            .apply()
    }

    private fun validateAmountInput(input: EditText, goalName: String): Boolean {
        val amountText = input.text?.toString().orEmpty()
        val normalizedAmount = normalizeAmountInput(amountText)
        if (normalizedAmount.isEmpty() || !isValidAmount(normalizedAmount)) {
            Toast.makeText(this, "Please enter a valid amount for $goalName", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun parseGoalAmount(input: String?, fallback: Double): Double {
        val normalized = normalizeAmountInput(input.orEmpty())
        return normalized.toDoubleOrNull() ?: fallback
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

    private fun onGoalInputEdited(goalName: String, enteredValue: String) {
        // Placeholder intentionally left blank for future goal editing logic.
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
