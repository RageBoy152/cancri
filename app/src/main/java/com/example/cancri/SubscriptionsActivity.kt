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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cancri.data.AppDatabase
import com.example.cancri.data.SubscriptionType
import com.example.cancri.data.UserPreferences
import com.example.cancri.data.model.SubscriptionModel
import com.example.cancri.ui.AddSubscriptionBottomSheet
import com.example.cancri.ui.NavbarFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Month
import java.util.Locale
import java.util.UUID

class SubscriptionsActivity : AppCompatActivity(), NavbarFragment.Listener {

    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase
    private lateinit var userPreferences: UserPreferences
    private val categoryNames = listOf("Bills", "Subscriptions", "Debts", "Savings Goals")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_subscriptions)

        database = AppDatabase.getDatabase(this, dbScope)
        userPreferences = UserPreferences(this)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        lifecycleScope.launch {
            database.getSubscriptionDao().observeAll().collect { subs ->
                withContext(Dispatchers.Main) { populateList(subs) }
            }
        }

        if (savedInstanceState == null) {
            val editSubscriptionId = intent.getStringExtra(EXTRA_EDIT_SUBSCRIPTION_ID)?.let { value ->
                runCatching { UUID.fromString(value) }.getOrNull()
            }
            if (editSubscriptionId != null) {
                AddSubscriptionBottomSheet
                    .newEditInstance(ArrayList(categoryNames), editSubscriptionId)
                    .show(supportFragmentManager, AddSubscriptionBottomSheet.TAG)
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
                setTextColor(getColor(R.color.text_tertiary))
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

            row.findViewById<TextView>(R.id.editSubName).text = sub.description
            row.findViewById<TextView>(R.id.editSubAmount).text = userPreferences.formatCurrency(sub.amount)
            row.findViewById<TextView>(R.id.editSubType).text = formatRenewalText(sub)

            row.findViewById<View>(R.id.btnEditSub).setOnClickListener {
                AddSubscriptionBottomSheet
                    .newEditInstance(ArrayList(categoryNames), sub.id)
                    .show(supportFragmentManager, AddSubscriptionBottomSheet.TAG)
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

    override fun onBottomNavFabClicked() {
        AddSubscriptionBottomSheet
            .newInstance(ArrayList(categoryNames))
            .show(supportFragmentManager, AddSubscriptionBottomSheet.TAG)
    }

    private fun formatRenewalText(subscription: SubscriptionModel): String {
        val day = (subscription.billingDay ?: 1).coerceIn(1, 31)
        return if (subscription.type == SubscriptionType.MONTHLY) {
            "/mo (Renews on $day${toOrdinalSuffix(day)})"
        } else {
            val month = (subscription.billingMonth ?: 1).coerceIn(1, 12)
            val monthLabel = Month.of(month).name.lowercase(Locale.UK)
                .replaceFirstChar { it.uppercaseChar() }
                .take(3)
            "/yr (Renews on $monthLabel $day${toOrdinalSuffix(day)})"
        }
    }

    private fun toOrdinalSuffix(day: Int): String {
        val mod100 = day % 100
        if (mod100 in 11..13) return "th"
        return when (day % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }

    companion object {
        const val EXTRA_EDIT_SUBSCRIPTION_ID = "extra_edit_subscription_id"
    }
}

