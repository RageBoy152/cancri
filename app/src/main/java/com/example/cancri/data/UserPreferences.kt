package com.example.cancri.data

import android.content.Context
import java.util.Locale

class UserPreferences(context: Context) {
    private val appContext = context.applicationContext

    private fun prefs() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // return selected currency or if null, default
    fun getCurrencySymbol(): String {
        return prefs()
            .getString(KEY_CURRENCY_SYMBOL, DEFAULT_CURRENCY_SYMBOL)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_CURRENCY_SYMBOL
    }

    // format price text with selected currency
    fun formatCurrency(amount: Double): String {
        return "${getCurrencySymbol()}${"%.2f".format(Locale.UK, amount)}"
    }

    // returns "Full Name" or just "Firstname"
    fun getDisplayName(): String {
        val sharedPrefs = prefs()
        val firstName = sharedPrefs.getString(KEY_USER_FIRST_NAME, "") ?: ""
        val lastName = sharedPrefs.getString(KEY_USER_LAST_NAME, "") ?: ""
        return when {
            firstName.isNotEmpty() && lastName.isNotEmpty() -> "$firstName $lastName"
            firstName.isNotEmpty() -> firstName
            else -> sharedPrefs.getString(KEY_USER_NAME, "there") ?: "there"
        }
    }

    // keys and other constants
    companion object {
        private const val PREFS_NAME = "cancri_prefs"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_FIRST_NAME = "user_first_name"
        const val KEY_USER_LAST_NAME = "user_last_name"
        const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        const val DEFAULT_CURRENCY_SYMBOL = "\u00A3"
    }
}
