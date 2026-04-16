package com.example.cancri.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.cancri.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val db = AppDatabase.getDatabase(context, scope)

        scope.launch {
            val subscriptions = db.getSubscriptionDao().getAll()
            val transactions = db.getTransactionDao().getAll()

            // Truncate "now" to the start of the day for cleaner day-counting
            val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()

            subscriptions.forEach { sub ->
                val lastPayment = transactions
                    .filter { it.subscriptionId == sub.id }
                    .maxByOrNull { it.createdAt }

                if (lastPayment != null) {
                    val lastPaymentDate = lastPayment.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
                    
                    val nextPaymentDate = when (sub.type.name) {
                        "MONTHLY" -> lastPaymentDate.plusMonths(1)
                        else -> lastPaymentDate.plusYears(1)
                    }

                    val daysUntil = ChronoUnit.DAYS.between(today, nextPaymentDate)

                    if (daysUntil == 3L && NotificationSettings.isSubscriptionRemindersEnabled(context)) {
                        // Randomise the message in the reminder
                        val titles = listOf("Heads up!", "Payment Alert", "Subscription Reminder", "Budget Check")
                        val footers = listOf("Ready?", "Keep an eye on it!", "Don't forget!", "Time to prepare!")
                        
                        NotificationHelper.showNotification(
                            context,
                            titles.random(),
                            "${sub.description} is due in 3 days. ${footers.random()}"
                        )
                    }
                }
            }

            // Weekly Streak Reminder
            val prefs = context.getSharedPreferences("cancri_prefs", Context.MODE_PRIVATE)
            val lastActiveStr = prefs.getString("last_active_date", "") ?: ""

            if (lastActiveStr.isNotEmpty()) {
                val lastActiveDate = java.time.LocalDate.parse(lastActiveStr)
                val daysSinceActive = ChronoUnit.DAYS.between(lastActiveDate, today)

                if (daysSinceActive == 6L && NotificationSettings.isStreakRemindersEnabled(context)) {
                    NotificationHelper.showNotification(
                        context,
                        "Streak at risk!",
                        "It's been 6 days! Open Cancri today to keep your weekly streak alive."
                    )
                }
            }

            // Personalised motivational messages (20% chance)
            if (NotificationSettings.isMotivationRemindersEnabled(context) && (1..100).random() <= 20) {
                val motivations = listOf(
                    "One Day at a Time" to "Tracking today is a great start.",
                    "Every Bit Counts" to "Small steps can make a big difference over time.",
                    "Stay Steady" to "Consistency helps build great habits.",
                    "Progress" to "You're making progress with every update.",
                    "Habit Builder" to "Building good habits takes time, keep at it."
                )
                
                val (randomTitle, randomMessage) = motivations.random()
                NotificationHelper.showNotification(context, randomTitle, randomMessage)
            }
        }
    }
}
