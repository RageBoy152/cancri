package com.example.cancri.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object NotificationSettings {

    const val PREFS_NAME = "cancri_prefs"
    const val KEY_SUBSCRIPTION_REMINDERS_ENABLED = "notif_subscription_reminders_enabled"
    const val KEY_STREAK_REMINDERS_ENABLED = "notif_streak_reminders_enabled"
    const val KEY_MOTIVATION_REMINDERS_ENABLED = "notif_motivation_reminders_enabled"

    fun isSubscriptionRemindersEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SUBSCRIPTION_REMINDERS_ENABLED, true)
    }

    fun isStreakRemindersEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_STREAK_REMINDERS_ENABLED, true)
    }

    fun isMotivationRemindersEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MOTIVATION_REMINDERS_ENABLED, true)
    }

    fun setSubscriptionRemindersEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SUBSCRIPTION_REMINDERS_ENABLED, enabled)
            .apply()
    }

    fun setStreakRemindersEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_STREAK_REMINDERS_ENABLED, enabled)
            .apply()
    }

    fun setMotivationRemindersEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MOTIVATION_REMINDERS_ENABLED, enabled)
            .apply()
    }

    fun disableAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SUBSCRIPTION_REMINDERS_ENABLED, false)
            .putBoolean(KEY_STREAK_REMINDERS_ENABLED, false)
            .putBoolean(KEY_MOTIVATION_REMINDERS_ENABLED, false)
            .apply()
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
