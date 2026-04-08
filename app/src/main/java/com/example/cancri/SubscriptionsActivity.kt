/* Cancri - money management app
   Programming for Mobile - COMP08068
   Team - Matt Miller, Kyle McNamee, Jaimie Neilson, Andrew Gilmour
   Date created - 24/03/26
   Ver 1.0
   Ver 1.1 Created 08/04/26
*/

package com.example.cancri

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cancri.data.AppDatabase
import com.example.cancri.data.SubscriptionType
import com.example.cancri.data.model.SubscriptionModel
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubscriptionsActivity : AppCompatActivity() {

    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_subscriptions)

        database = AppDatabase.getDatabase(this, dbScope)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        lifecycleScope.launch {
            database.getSubscriptionDao().observeAll().collect { subs ->
                withContext(Dispatchers.Main) { populateList(subs) }
            }
        }
    }

    private fun populateList(subs: List<SubscriptionModel>) {
        val container = findViewById<LinearLayout>(R.id.editSubsContainer)
        container.removeAllViews()

        if (subs.isEmpty()) {
            val empty = TextView(this).apply {
                text     = "No subscriptions yet"
                textSize = 14f
                setTextColor(getColor(R.color.text_muted))
                setPadding(0, 24, 0, 0)
            }
            container.addView(empty)
            return
        }

        subs.forEach { sub ->
            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_subscription_edit, container, false)

            row.findViewById<TextView>(R.id.editSubLogo).apply {
                text = sub.description.firstOrNull()?.uppercase() ?: "?"
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    getColor(if (sub.type == SubscriptionType.MONTHLY) R.color.green_primary else R.color.paramount_blue)
                )
            }

            row.findViewById<TextView>(R.id.editSubName).text   = sub.description
            row.findViewById<TextView>(R.id.editSubAmount).text = "£%.2f".format(sub.amount)
            row.findViewById<TextView>(R.id.editSubType).text   =
                if (sub.type == SubscriptionType.MONTHLY) "Monthly" else "Yearly"

            row.findViewById<View>(R.id.btnEditSub).setOnClickListener {
                showEditDialog(sub)
            }

            row.findViewById<View>(R.id.btnDeleteSub).setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Remove subscription")
                    .setMessage("Remove ${sub.description}?")
                    .setPositiveButton("Remove") { _, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            // Delete all transactions linked to this subscription by ID
                            // This updates the spending breakdown bar via Flow
                            database.getTransactionDao().deleteBySubscriptionId(sub.id)

                            // Delete the subscription itself
                            database.getSubscriptionDao().delete(sub)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@SubscriptionsActivity, "Removed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            container.addView(row)
        }
    }

    private fun showEditDialog(sub: SubscriptionModel) {
        val dialogView  = LayoutInflater.from(this).inflate(R.layout.dialog_edit_subscription, null)
        val inputAmount = dialogView.findViewById<TextInputEditText>(R.id.dialogInputAmount)
        val btnMonthly  = dialogView.findViewById<Button>(R.id.dialogBtnMonthly)
        val btnYearly   = dialogView.findViewById<Button>(R.id.dialogBtnYearly)

        var selectedType = sub.type
        inputAmount.setText("%.2f".format(sub.amount))

        fun selectMonthly() {
            selectedType = SubscriptionType.MONTHLY
            btnMonthly.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.green_primary))
            btnMonthly.setTextColor(getColor(R.color.white))
            btnYearly.backgroundTintList  = null
            btnYearly.setTextColor(getColor(R.color.text_secondary))
        }
        fun selectYearly() {
            selectedType = SubscriptionType.YEARLY
            btnYearly.backgroundTintList  = android.content.res.ColorStateList.valueOf(getColor(R.color.green_primary))
            btnYearly.setTextColor(getColor(R.color.white))
            btnMonthly.backgroundTintList = null
            btnMonthly.setTextColor(getColor(R.color.text_secondary))
        }

        btnMonthly.setOnClickListener { selectMonthly() }
        btnYearly.setOnClickListener  { selectYearly() }
        if (sub.type == SubscriptionType.MONTHLY) selectMonthly() else selectYearly()

        AlertDialog.Builder(this)
            .setTitle("Edit ${sub.description}")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newAmount = inputAmount.text?.toString()?.toDoubleOrNull()
                if (newAmount == null || newAmount <= 0) {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    // Update subscription
                    database.getSubscriptionDao().update(
                        sub.copy(amount = newAmount, type = selectedType)
                    )

                    // Update linked transaction amounts by subscription ID
                    // This updates the spending breakdown bar via Flow
                    database.getTransactionDao().updateAmountBySubscriptionId(sub.id, newAmount)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SubscriptionsActivity, "Updated!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}