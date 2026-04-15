package com.example.cancri.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule the daily alarm as soon as the phone turns on
            NotificationScheduler.scheduleDaily(context)
        }
    }
}
