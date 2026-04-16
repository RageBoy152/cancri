package com.example.cancri

import android.content.Context
import java.util.Locale

object UserPreferences {
    private const val PREFS_NAME = "cancri_prefs"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_FIRST_NAME = "user_first_name"
    const val KEY_USER_LAST_NAME = "user_last_name"
    const val KEY_CURRENCY_SYMBOL = "currency_symbol"
    const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    const val DEFAULT_CURRENCY_SYMBOL = "\u00A3"

    fun getCurrencySymbol(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CURRENCY_SYMBOL, DEFAULT_CURRENCY_SYMBOL)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_CURRENCY_SYMBOL
    }

    fun formatCurrency(context: Context, amount: Double): String {
        return "${getCurrencySymbol(context)}${"%.2f".format(Locale.UK, amount)}"
    }

    fun getDisplayName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val firstName = prefs.getString(KEY_USER_FIRST_NAME, "") ?: ""
        val lastName = prefs.getString(KEY_USER_LAST_NAME, "") ?: ""
        return when {
            firstName.isNotEmpty() && lastName.isNotEmpty() -> "$firstName $lastName"
            firstName.isNotEmpty() -> firstName
            else -> prefs.getString(KEY_USER_NAME, "there") ?: "there"
        }
    }
}
