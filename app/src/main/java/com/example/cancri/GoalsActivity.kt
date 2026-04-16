package com.example.cancri

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class GoalsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_goals)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        setupGoalRow(R.id.billsGoal, "Bills")
        setupGoalRow(R.id.debtsGoal, "Debts")
        setupGoalRow(R.id.savingsGoals, "Savings Goals")
    }

    private fun setupGoalRow(rowId: Int, goalName: String) {
        val row = findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.spendingCatHeading).text = goalName

        row.findViewById<EditText>(R.id.editAmount).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                onGoalInputEdited(goalName, s?.toString().orEmpty())
            }
        })
    }

    private fun onGoalInputEdited(goalName: String, enteredValue: String) {
        // Placeholder intentionally left blank for future goal editing logic.
    }
}
