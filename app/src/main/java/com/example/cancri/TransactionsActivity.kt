package com.example.cancri

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cancri.data.AppDatabase
import com.example.cancri.data.model.TransactionModel
import com.example.cancri.ui.AddTransactionBottomSheet
import com.example.cancri.ui.NavbarFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionsActivity : AppCompatActivity(), NavbarFragment.Listener {

    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase
    private val categoryNames = listOf("Bills", "Subscriptions", "Debts", "Savings Goals")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_transactions)

        database = AppDatabase.getDatabase(this, dbScope)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        lifecycleScope.launch {
            database.getTransactionDao().observeAll().collect { transactions ->
                withContext(Dispatchers.Main) { populateList(transactions) }
            }
        }
    }

    private fun populateList(transactions: List<TransactionModel>) {
        val container = findViewById<LinearLayout>(R.id.editTransactionsContainer)
        container.removeAllViews()

        if (transactions.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No transactions yet"
                textSize = 14f
                setTextColor(getColor(R.color.text_tertiary))
                setPadding(0, 24, 0, 0)
            }
            container.addView(empty)
            return
        }

        transactions.forEach { transaction ->
            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_transaction_edit, container, false)

            row.findViewById<TextView>(R.id.editTransactionLogo).apply {
                text = transaction.description.firstOrNull()?.uppercase() ?: "?"
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    getColor(R.color.green_primary)
                )
            }

            row.findViewById<TextView>(R.id.editTransactionName).text = transaction.description
            row.findViewById<TextView>(R.id.editTransactionAmount).text =
                UserPreferences.formatCurrency(this, transaction.amount)
            row.findViewById<TextView>(R.id.editTransactionCategory).text =
                transaction.category ?: "Uncategorized"
            row.findViewById<TextView>(R.id.btnEditTransaction).text =
                if (transaction.subscriptionId != null) "Edit Subscription" else "Edit"

            row.findViewById<View>(R.id.btnEditTransaction).setOnClickListener {
                if (transaction.subscriptionId != null) {
                    startActivity(
                        Intent(this, SubscriptionsActivity::class.java).apply {
                            putExtra(
                                SubscriptionsActivity.EXTRA_EDIT_SUBSCRIPTION_ID,
                                transaction.subscriptionId.toString()
                            )
                        }
                    )
                } else {
                    AddTransactionBottomSheet
                        .newEditInstance(ArrayList(categoryNames), transaction.id)
                        .show(supportFragmentManager, AddTransactionBottomSheet.TAG)
                }
            }

            row.findViewById<View>(R.id.btnDeleteTransaction).setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Remove transaction")
                    .setMessage("Remove ${transaction.description}?")
                    .setPositiveButton("Remove") { _, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            database.getTransactionDao().delete(transaction)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@TransactionsActivity, "Removed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            container.addView(row)
        }
    }

    override fun onBottomNavFabClicked() {
        AddTransactionBottomSheet
            .newInstance(ArrayList(categoryNames))
            .show(supportFragmentManager, AddTransactionBottomSheet.TAG)
    }
}
