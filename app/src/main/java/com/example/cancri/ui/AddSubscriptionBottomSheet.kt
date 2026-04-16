package com.example.cancri.ui

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.cancri.R
import com.example.cancri.data.AppDatabase
import com.example.cancri.data.SubscriptionType
import com.example.cancri.data.UserPreferences
import com.example.cancri.data.model.SubscriptionModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Month
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddSubscriptionBottomSheet : BottomSheetDialogFragment() {

    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase
    private var currentAmount = 0.0
    private var isUpdatingAmountText = false
    private lateinit var currencySymbol: String
    private var selectedType: SubscriptionType = SubscriptionType.MONTHLY
    private var selectedYearlyMonth: Int? = null
    private var selectedYearlyDay: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = AppDatabase.getDatabase(requireContext(), dbScope)
        currencySymbol = UserPreferences(requireContext()).getCurrencySymbol()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_add_subscription, container, false)
    }

    override fun onStart() {
        super.onStart()
        val bottomSheetDialog = dialog as? BottomSheetDialog ?: return
        bottomSheetDialog.behavior.apply {
            isDraggable = true
            isHideable = true
            skipCollapsed = true
            isGestureInsetBottomIgnored = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val amountInput = view.findViewById<EditText>(R.id.inputAmount)
        val titleInput = view.findViewById<EditText>(R.id.inputTitle)
        val btnMonthly = view.findViewById<Button>(R.id.btnMonthly)
        val btnYearly = view.findViewById<Button>(R.id.btnYearly)
        val heading = view.findViewById<TextView>(R.id.subscriptionAddEditHeading)
        val billingDateLabel = view.findViewById<TextView>(R.id.billingDateLabel)
        val monthlyDaySpinner = view.findViewById<Spinner>(R.id.spinnerMonthlyDay)
        val yearlyDateInput = view.findViewById<EditText>(R.id.inputYearlyDate)

        monthlyDaySpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            (1..31).map { it.toString() }
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        amountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingAmountText) return

                val normalizedAmount = normalizeAmountInput(s?.toString().orEmpty())
                val rendered = if (normalizedAmount.isEmpty()) "" else "$currencySymbol$normalizedAmount"
                val current = s?.toString().orEmpty()

                if (rendered != current) {
                    isUpdatingAmountText = true
                    amountInput.setText(rendered)
                    amountInput.setSelection(rendered.length)
                    isUpdatingAmountText = false
                }

                currentAmount = normalizedAmount.toDoubleOrNull() ?: 0.0
            }
        })

        val calendar = Calendar.getInstance()
        monthlyDaySpinner.setSelection(calendar.get(Calendar.DAY_OF_MONTH) - 1)

        fun applyCycleUi() {
            val selectedColor = ColorStateList.valueOf(requireContext().getColor(R.color.green_primary))

            if (selectedType == SubscriptionType.MONTHLY) {
                btnMonthly.backgroundTintList = selectedColor
                btnMonthly.setTextColor(requireContext().getColor(R.color.white))
                btnYearly.backgroundTintList = null
                btnYearly.setTextColor(requireContext().getColor(R.color.green_primary))

                billingDateLabel.text = "Day of month"
                monthlyDaySpinner.visibility = View.VISIBLE
                yearlyDateInput.visibility = View.GONE
            } else {
                btnYearly.backgroundTintList = selectedColor
                btnYearly.setTextColor(requireContext().getColor(R.color.white))
                btnMonthly.backgroundTintList = null
                btnMonthly.setTextColor(requireContext().getColor(R.color.green_primary))

                billingDateLabel.text = "Billing date"
                monthlyDaySpinner.visibility = View.GONE
                yearlyDateInput.visibility = View.VISIBLE

                if (selectedYearlyMonth == null || selectedYearlyDay == null) {
                    selectedYearlyMonth = calendar.get(Calendar.MONTH) + 1
                    selectedYearlyDay = calendar.get(Calendar.DAY_OF_MONTH)
                }
                yearlyDateInput.setText(formatMonthDay(selectedYearlyMonth!!, selectedYearlyDay!!))
            }
        }

        btnMonthly.setOnClickListener {
            selectedType = SubscriptionType.MONTHLY
            applyCycleUi()
        }

        btnYearly.setOnClickListener {
            selectedType = SubscriptionType.YEARLY
            applyCycleUi()
        }

        yearlyDateInput.setOnClickListener {
            val currentYear = calendar.get(Calendar.YEAR)
            val monthZeroIndex = (selectedYearlyMonth ?: (calendar.get(Calendar.MONTH) + 1)) - 1
            val day = selectedYearlyDay ?: calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                requireContext(),
                { _, _, selectedMonthZeroBased, selectedDayOfMonth ->
                    selectedYearlyMonth = selectedMonthZeroBased + 1
                    selectedYearlyDay = selectedDayOfMonth
                    yearlyDateInput.setText(formatMonthDay(selectedYearlyMonth!!, selectedYearlyDay!!))
                },
                currentYear,
                monthZeroIndex,
                day
            ).show()
        }

        view.findViewById<View>(R.id.dragHandle).setOnClickListener { dismiss() }

        val editSubscriptionId = arguments?.getString(ARG_EDIT_SUBSCRIPTION_ID)?.let { UUID.fromString(it) }
        amountInput.hint = "${currencySymbol}0.00"
        if (editSubscriptionId != null) {
            loadEditValues(editSubscriptionId, heading, amountInput, titleInput, monthlyDaySpinner, yearlyDateInput, ::applyCycleUi)
        } else {
            heading.text = "Add Subscription"
            selectedType = SubscriptionType.MONTHLY
            applyCycleUi()
        }

        view.findViewById<Button>(R.id.btnSaveTransaction).setOnClickListener {
            val normalizedAmount = normalizeAmountInput(amountInput.text?.toString().orEmpty())
            if (normalizedAmount.isEmpty() || !isValidAmount(normalizedAmount)) {
                Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentAmount = normalizedAmount.toDouble()
            if (currentAmount == 0.0) {
                Toast.makeText(requireContext(), "Please add an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val title = titleInput.text?.toString()?.trim().orEmpty().ifEmpty { "Subscription" }
            val billingDay = if (selectedType == SubscriptionType.MONTHLY) monthlyDaySpinner.selectedItemPosition + 1 else selectedYearlyDay
            val billingMonth = if (selectedType == SubscriptionType.YEARLY) selectedYearlyMonth else null

            if (selectedType == SubscriptionType.YEARLY && (billingMonth == null || billingDay == null)) {
                Toast.makeText(requireContext(), "Please select a billing date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                if (editSubscriptionId != null) {
                    updateExistingSubscription(editSubscriptionId, currentAmount, title, selectedType, billingDay, billingMonth)
                } else {
                    createSubscription(currentAmount, title, selectedType, billingDay, billingMonth)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }

    private suspend fun createSubscription(
        amount: Double,
        title: String,
        type: SubscriptionType,
        billingDay: Int?,
        billingMonth: Int?
    ) {
        val subscriptionId = UUID.randomUUID()

        database.getSubscriptionDao().upsert(
            SubscriptionModel(
                id = subscriptionId,
                amount = amount,
                description = title,
                type = type,
                billingDay = billingDay,
                billingMonth = billingMonth
            )
        )
    }

    private suspend fun updateExistingSubscription(
        subscriptionId: UUID,
        amount: Double,
        title: String,
        type: SubscriptionType,
        billingDay: Int?,
        billingMonth: Int?
    ) {
        val existing = database.getSubscriptionDao().findById(subscriptionId) ?: return

        database.getSubscriptionDao().update(
            existing.copy(
                amount = amount,
                description = title,
                type = type,
                billingDay = billingDay,
                billingMonth = billingMonth
            )
        )
    }

    private fun loadEditValues(
        subscriptionId: UUID,
        heading: TextView,
        amountInput: EditText,
        titleInput: EditText,
        monthlyDaySpinner: Spinner,
        yearlyDateInput: EditText,
        applyCycleUi: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val existing = database.getSubscriptionDao().findById(subscriptionId) ?: return@launch
            withContext(Dispatchers.Main) {
                heading.text = "Edit '${existing.description}'"
                val amountText = "${currencySymbol}%.2f".format(Locale.UK, existing.amount)
                amountInput.setText(amountText)
                amountInput.setSelection(amountText.length)
                titleInput.setText(existing.description)

                selectedType = existing.type
                selectedYearlyMonth = existing.billingMonth
                selectedYearlyDay = existing.billingDay

                if (existing.type == SubscriptionType.MONTHLY) {
                    val day = (existing.billingDay ?: 1).coerceIn(1, 31)
                    monthlyDaySpinner.setSelection(day - 1)
                } else {
                    if (existing.billingMonth != null && existing.billingDay != null) {
                        yearlyDateInput.setText(formatMonthDay(existing.billingMonth, existing.billingDay))
                    }
                }

                applyCycleUi()
            }
        }
    }

    companion object {
        const val TAG = "AddSubscriptionBottomSheet"
        private const val ARG_CATEGORIES = "arg_categories"
        private const val ARG_EDIT_SUBSCRIPTION_ID = "arg_edit_subscription_id"

        fun newInstance(categories: ArrayList<String>): AddSubscriptionBottomSheet {
            return AddSubscriptionBottomSheet().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_CATEGORIES, categories)
                }
            }
        }

        fun newEditInstance(categories: ArrayList<String>, subscriptionId: UUID): AddSubscriptionBottomSheet {
            return AddSubscriptionBottomSheet().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_CATEGORIES, categories)
                    putString(ARG_EDIT_SUBSCRIPTION_ID, subscriptionId.toString())
                }
            }
        }

        fun show(fragmentManager: FragmentManager, categories: ArrayList<String>) {
            newInstance(categories).show(fragmentManager, TAG)
        }
    }

    private fun formatMonthDay(month: Int, day: Int): String {
        val monthLabel = Month.of(month.coerceIn(1, 12)).name.lowercase(Locale.UK)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.UK) else it.toString() }
        return "$monthLabel $day"
    }

    private fun normalizeAmountInput(input: String): String {
        val withoutSymbol = input.replace(currencySymbol, "").replace(",", "").trim()
        if (withoutSymbol.isEmpty()) return ""

        val builder = StringBuilder()
        var seenDot = false
        var decimals = 0

        withoutSymbol.forEach { char ->
            when {
                char.isDigit() && (!seenDot || decimals < 2) -> {
                    builder.append(char)
                    if (seenDot) decimals++
                }

                char == '.' && !seenDot -> {
                    if (builder.isEmpty()) builder.append('0')
                    builder.append('.')
                    seenDot = true
                }
            }
        }

        return builder.toString()
    }

    private fun isValidAmount(amount: String): Boolean {
        return amount.matches(Regex("^\\d+(\\.\\d{1,2})?$"))
    }
}

