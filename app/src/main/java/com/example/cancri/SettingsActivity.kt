/* Cancri - money management app
   Programming for Mobile - COMP08068
   Team - Matt Miller, Kyle McNamee, Jaimie Neilson, Andrew Gilmour
   Settings Activity
*/

package com.example.cancri

import android.animation.ObjectAnimator
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.NestedScrollView
import com.example.cancri.data.UserPreferences
import com.example.cancri.notifications.NotificationSettings

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var nameInput: EditText
    private lateinit var currencySpinner: Spinner
    private lateinit var subscriptionSwitch: SwitchCompat
    private lateinit var streakSwitch: SwitchCompat
    private lateinit var motivationSwitch: SwitchCompat
    private lateinit var userPreferences: UserPreferences
    private var isBindingCurrency = false
    private var isBindingNotificationSwitches = false
    private var pendingNotificationToggleKey: String? = null
    private var permissionMessageShown = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val toggleKey = pendingNotificationToggleKey
        pendingNotificationToggleKey = null

        if (granted) {
            permissionMessageShown = false
            when (toggleKey) {
                NotificationSettings.KEY_SUBSCRIPTION_REMINDERS_ENABLED ->
                    NotificationSettings.setSubscriptionRemindersEnabled(this, true)
                NotificationSettings.KEY_STREAK_REMINDERS_ENABLED ->
                    NotificationSettings.setStreakRemindersEnabled(this, true)
                NotificationSettings.KEY_MOTIVATION_REMINDERS_ENABLED ->
                    NotificationSettings.setMotivationRemindersEnabled(this, true)
            }
        } else {
            NotificationSettings.disableAll(this)
            if (!permissionMessageShown) {
                Toast.makeText(
                    this,
                    "Please allow notifications for Cancri in your device settings.",
                    Toast.LENGTH_SHORT
                ).show()
                permissionMessageShown = true
            }
        }

        bindNotificationSwitches()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_settings)

        userPreferences = UserPreferences(this)
        prefs = getSharedPreferences("cancri_prefs", MODE_PRIVATE)
        nameInput = findViewById(R.id.inputUserName)
        currencySpinner = findViewById(R.id.spinnerCurrency)
        subscriptionSwitch = findViewById(R.id.switchSubscriptionReminders)
        streakSwitch = findViewById(R.id.switchStreakReminders)
        motivationSwitch = findViewById(R.id.switchMotivationReminders)

        setupCurrencySpinner()
        setupNameInput()
        setupNotificationSwitches()
        animatePanelEntry()
    }

    override fun onResume() {
        super.onResume()
        syncNotificationPermissionState()
        bindValues()
    }

    private fun bindValues() {
        val displayName = userPreferences.getDisplayName()
        nameInput.setText(if (displayName == "there") "" else displayName)
        nameInput.setSelection(nameInput.text?.length ?: 0)

        isBindingCurrency = true
        val selectedSymbol = userPreferences.getCurrencySymbol()
        val options = currencyOptions()
        val index = options.indexOfFirst { it.first == selectedSymbol }.coerceAtLeast(0)
        currencySpinner.setSelection(index)
        isBindingCurrency = false

        bindNotificationSwitches()
    }

    private fun setupNameInput() {
        nameInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveName()
            }
        }
    }

    private fun saveName() {
        val name = nameInput.text?.toString()?.trim().orEmpty()
        prefs.edit()
            .putString(UserPreferences.KEY_USER_NAME, name)
            .putString(UserPreferences.KEY_USER_FIRST_NAME, name)
            .putString(UserPreferences.KEY_USER_LAST_NAME, "")
            .apply()
    }

    private fun setupCurrencySpinner() {
        val options = currencyOptions()
        val labels = options.map { it.second }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySpinner.adapter = adapter

        currencySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isBindingCurrency) return
                val symbol = options[position].first
                prefs.edit().putString(UserPreferences.KEY_CURRENCY_SYMBOL, symbol).apply()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
    }

    private fun setupNotificationSwitches() {
        subscriptionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingNotificationSwitches) return@setOnCheckedChangeListener
            handleNotificationToggle(
                NotificationSettings.KEY_SUBSCRIPTION_REMINDERS_ENABLED,
                isChecked
            )
        }

        streakSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingNotificationSwitches) return@setOnCheckedChangeListener
            handleNotificationToggle(
                NotificationSettings.KEY_STREAK_REMINDERS_ENABLED,
                isChecked
            )
        }

        motivationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingNotificationSwitches) return@setOnCheckedChangeListener
            handleNotificationToggle(
                NotificationSettings.KEY_MOTIVATION_REMINDERS_ENABLED,
                isChecked
            )
        }
    }

    private fun bindNotificationSwitches() {
        isBindingNotificationSwitches = true
        subscriptionSwitch.isChecked = NotificationSettings.isSubscriptionRemindersEnabled(this)
        streakSwitch.isChecked = NotificationSettings.isStreakRemindersEnabled(this)
        motivationSwitch.isChecked = NotificationSettings.isMotivationRemindersEnabled(this)
        isBindingNotificationSwitches = false
    }

    private fun handleNotificationToggle(key: String, enabled: Boolean) {
        if (!enabled) {
            setNotificationToggle(key, false)
            return
        }

        if (NotificationSettings.hasNotificationPermission(this)) {
            permissionMessageShown = false
            setNotificationToggle(key, true)
            return
        }

        pendingNotificationToggleKey = key
        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun syncNotificationPermissionState() {
        if (!NotificationSettings.hasNotificationPermission(this)) {
            NotificationSettings.disableAll(this)
        }
    }

    private fun setNotificationToggle(key: String, enabled: Boolean) {
        when (key) {
            NotificationSettings.KEY_SUBSCRIPTION_REMINDERS_ENABLED ->
                NotificationSettings.setSubscriptionRemindersEnabled(this, enabled)
            NotificationSettings.KEY_STREAK_REMINDERS_ENABLED ->
                NotificationSettings.setStreakRemindersEnabled(this, enabled)
            NotificationSettings.KEY_MOTIVATION_REMINDERS_ENABLED ->
                NotificationSettings.setMotivationRemindersEnabled(this, enabled)
        }
    }

    private fun currencyOptions(): List<Pair<String, String>> {
        return listOf(
            "\u00A3" to "\u00A3 British Pound",
            "$" to "$ US Dollar",
            "\u20AC" to "\u20AC Euro",
            "\u00A5" to "\u00A5 Yen"
        )
    }


    private fun animatePanelEntry() {
        val panel = findViewById<NestedScrollView>(R.id.settingsPanel)
        panel.post {
            ObjectAnimator.ofFloat(panel, View.TRANSLATION_Y, panel.height.toFloat(), 0f).apply {
                duration = 650
                interpolator = android.view.animation.DecelerateInterpolator(2f)
                start()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveName()
    }
}
