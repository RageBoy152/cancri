package com.example.cancri.ui

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
import com.example.cancri.UserPreferences
import com.example.cancri.data.AppDatabase
import com.example.cancri.data.SubscriptionType
import com.example.cancri.data.model.SubscriptionModel
import com.example.cancri.data.model.TransactionModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Locale
import java.util.UUID

class AddTransactionBottomSheet : BottomSheetDialogFragment() {

    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase
    private var currentAmount = 0.0
    private var isUpdatingAmountText = false
    private lateinit var currencySymbol: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = AppDatabase.getDatabase(requireContext(), dbScope)
        currencySymbol = UserPreferences.getCurrencySymbol(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_add_transaction, container, false)
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

        val heading = view.findViewById<TextView>(R.id.transactionAddEditHeading)
        val amountInput = view.findViewById<EditText>(R.id.amountDisplay)
        val input = view.findViewById<EditText>(R.id.inputName)
        val spinner = view.findViewById<Spinner>(R.id.spinnerCategory)
        val saveButton = view.findViewById<Button>(R.id.btnSaveTransaction)
        val editTransactionId = arguments?.getString(ARG_EDIT_TRANSACTION_ID)?.let { value ->
            runCatching { UUID.fromString(value) }.getOrNull()
        }

        val categories = arguments?.getStringArrayList(ARG_CATEGORIES).orEmpty()
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("----") + categories + listOf("Other")
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = categoryAdapter

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

        listOf(R.id.btn5 to 5.0, R.id.btn10 to 10.0, R.id.btn20 to 20.0, R.id.btn50 to 50.0)
            .forEach { (id, value) ->
                view.findViewById<Button>(id).setOnClickListener {
                    currentAmount = value
                    val amountText = "${currencySymbol}%.2f".format(Locale.UK, currentAmount)
                    amountInput.setText(amountText)
                    amountInput.setSelection(amountText.length)
                }
            }

        view.findViewById<Button>(R.id.btn5).text = "${currencySymbol}5"
        view.findViewById<Button>(R.id.btn10).text = "${currencySymbol}10"
        view.findViewById<Button>(R.id.btn20).text = "${currencySymbol}20"
        view.findViewById<Button>(R.id.btn50).text = "${currencySymbol}50"
        amountInput.hint = "${currencySymbol}0.00"

        view.findViewById<View>(R.id.dragHandle).setOnClickListener {
            dismiss()
        }

        if (editTransactionId != null) {
            heading.text = "Edit Transaction"
            saveButton.text = "Save Changes"
            loadEditValues(editTransactionId, amountInput, input, spinner)
        } else {
            heading.text = "Add Transaction"
            saveButton.text = "Save Transaction"
        }

        saveButton.setOnClickListener {
            val amountText = amountInput.text?.toString().orEmpty()
            val normalizedAmount = normalizeAmountInput(amountText)
            if (normalizedAmount.isEmpty() || !isValidAmount(normalizedAmount)) {
                Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentAmount = normalizedAmount.toDouble()
            if (currentAmount == 0.0) {
                Toast.makeText(requireContext(), "Please add an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name = input.text?.toString()?.trim() ?: ""
            val selected = spinner.selectedItem?.toString()
            val category = if (selected == "----" || selected == "Other") null else selected

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                if (editTransactionId != null) {
                    updateExistingTransaction(
                        transactionId = editTransactionId,
                        amount = currentAmount,
                        description = name.ifEmpty { category ?: "Transaction" },
                        category = category
                    )
                } else {
                    var linkedSubId: UUID? = null
                    if (category == "Subscriptions") {
                        linkedSubId = UUID.randomUUID()
                        database.getSubscriptionDao().upsert(
                            SubscriptionModel(
                                id = linkedSubId,
                                amount = currentAmount,
                                description = name.ifEmpty { "Subscription" },
                                type = SubscriptionType.MONTHLY,
                                billingDay = null,
                                billingMonth = null
                            )
                        )
                    }

                    database.getTransactionDao().upsert(
                        TransactionModel(
                            id = UUID.randomUUID(),
                            createdAt = Instant.now(),
                            updatedAt = null,
                            amount = currentAmount,
                            description = name.ifEmpty { category ?: "Transaction" },
                            subscriptionId = linkedSubId,
                            category = category
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }

    private suspend fun updateExistingTransaction(
        transactionId: UUID,
        amount: Double,
        description: String,
        category: String?
    ) {
        val existing = database.getTransactionDao().findById(transactionId) ?: return
        database.getTransactionDao().update(
            existing.copy(
                amount = amount,
                description = description,
                category = category,
                updatedAt = Instant.now()
            )
        )
    }

    private fun loadEditValues(
        transactionId: UUID,
        amountInput: EditText,
        input: EditText,
        spinner: Spinner
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val existing = database.getTransactionDao().findById(transactionId) ?: return@launch
            withContext(Dispatchers.Main) {
                val amountText = "${currencySymbol}%.2f".format(Locale.UK, existing.amount)
                amountInput.setText(amountText)
                amountInput.setSelection(amountText.length)
                input.setText(existing.description)

                val selectedCategory = existing.category
                if (selectedCategory != null) {
                    val index = (0 until spinner.count).firstOrNull {
                        spinner.getItemAtPosition(it)?.toString() == selectedCategory
                    } ?: 0
                    spinner.setSelection(index)
                } else {
                    spinner.setSelection(0)
                }
            }
        }
    }

    companion object {
        const val TAG = "AddTransactionBottomSheet"
        private const val ARG_CATEGORIES = "arg_categories"
        private const val ARG_EDIT_TRANSACTION_ID = "arg_edit_transaction_id"

        fun newInstance(categories: ArrayList<String>): AddTransactionBottomSheet {
            return AddTransactionBottomSheet().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_CATEGORIES, categories)
                }
            }
        }

        fun newEditInstance(categories: ArrayList<String>, transactionId: UUID): AddTransactionBottomSheet {
            return AddTransactionBottomSheet().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_CATEGORIES, categories)
                    putString(ARG_EDIT_TRANSACTION_ID, transactionId.toString())
                }
            }
        }

        fun show(fragmentManager: FragmentManager, categories: ArrayList<String>) {
            newInstance(categories).show(fragmentManager, TAG)
        }
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

