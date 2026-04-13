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

                    if (daysUntil == 3L) {
                        NotificationHelper.showNotification(
                            context,
                            "Subscription Due Soon",
                            "${sub.description} is due in 3 days"
                        )
                    }
                }
            }

            // 20% chance of motivation
            if ((1..100).random() <= 20) {
                NotificationHelper.showNotification(
                    context,
                    "Keep Going",
                    "You're doing great — stay consistent"
                )
            }
        }
    }
}
