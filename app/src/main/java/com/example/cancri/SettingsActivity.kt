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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.example.cancri.data.UserPreferences

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var nameInput: EditText
    private lateinit var currencySpinner: Spinner
    private lateinit var userPreferences: UserPreferences
    private var isBindingCurrency = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_settings)

        userPreferences = UserPreferences(this)
        prefs = getSharedPreferences("cancri_prefs", MODE_PRIVATE)
        nameInput = findViewById(R.id.inputUserName)
        currencySpinner = findViewById(R.id.spinnerCurrency)

        setupCurrencySpinner()
        setupNameInput()
        animatePanelEntry()
    }

    override fun onResume() {
        super.onResume()
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
