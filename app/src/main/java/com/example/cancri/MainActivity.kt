/* Cancri - money management app
   Programming for Mobile - COMP08068
   Team - Matt Miller, Kyle McNamee, Jaimie Neilson, Andrew Gilmour
   Date created - 24/03/26
   Ver 1.0
   Ver 1.1 Created 08/04/26
*/

package com.example.cancri

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.example.cancri.data.AppDatabase
import com.example.cancri.data.SubscriptionType
import com.example.cancri.data.model.SubscriptionModel
import com.example.cancri.data.model.TransactionModel
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Calendar
import java.util.UUID

class MainActivity : AppCompatActivity(), NavbarFragment.Listener {

    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase

    private val budgets = mapOf(
        "Bills"         to 900.0,
        "Subscriptions" to 54.99,
        "Debts"         to 200.0,
        "Savings Goals" to 300.0
    )

    private val categoryRowIds    = listOf(R.id.catBills, R.id.catSubscriptions, R.id.catDebts, R.id.catSavings)
    private val categoryNames     = listOf("Bills", "Subscriptions", "Debts", "Savings Goals")
    private val categoryBarColors = listOf(R.color.bar_green, R.color.bar_green, R.color.bar_amber, R.color.bar_red)
    private val categoryIconMap = mapOf(
        "Savings Goals" to R.drawable.ic_lucide_piggy_bank,
        "Debts" to R.drawable.ic_lucide_wallet_cards,
        "Subscriptions" to R.drawable.ic_lucide_list,
        "Bills" to R.drawable.ic_lucide_lamp_ceiling
    )

    private var currentAmount = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        database = AppDatabase.getDatabase(this, dbScope)

        setupHero()
        setupScreenActions()
        setupAddTransactionDrawer()
        animatePanelEntry()
        observeTransactions()
        observeSubscriptions()
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }
    override fun onResume() {
        super.onResume()
        val prefs     = getSharedPreferences("cancri_prefs", MODE_PRIVATE)
        val firstName = prefs.getString("user_first_name", "") ?: ""
        val lastName  = prefs.getString("user_last_name", "") ?: ""
        val userName  = when {
            firstName.isNotEmpty() && lastName.isNotEmpty() -> "$firstName $lastName"
            firstName.isNotEmpty() -> firstName
            else -> prefs.getString("user_name", "there") ?: "there"
        }
        findViewById<TextView>(R.id.heroName).text = userName
    }

    //  Hero
    private fun setupHero() {
        val prefs     = getSharedPreferences("cancri_prefs", MODE_PRIVATE)
        val firstName = prefs.getString("user_first_name", "") ?: ""
        val lastName  = prefs.getString("user_last_name", "") ?: ""
        val userName  = when {
            firstName.isNotEmpty() && lastName.isNotEmpty() -> "$firstName $lastName"
            firstName.isNotEmpty() -> firstName
            else -> prefs.getString("user_name", "there") ?: "there"
        }
        findViewById<TextView>(R.id.heroGreeting).text = getTimeGreeting()
        findViewById<TextView>(R.id.heroName).text     = userName
    }

    private fun getTimeGreeting() = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11  -> "Good morning,"
        in 12..17 -> "Good afternoon,"
        else      -> "Good evening,"
    }

    //  Panel slide-up
    private fun animatePanelEntry() {
        val panel = findViewById<NestedScrollView>(R.id.whitePanel)
        panel.post {
            ObjectAnimator.ofFloat(panel, View.TRANSLATION_Y, panel.height.toFloat(), 0f).apply {
                duration     = 650
                interpolator = android.view.animation.DecelerateInterpolator(2f)
                start()
            }
        }
    }

    //  Observe transactions
    private fun observeTransactions() {
        lifecycleScope.launch {
            database.getTransactionDao().observeAll().collect { transactions ->
                withContext(Dispatchers.Main) {
                    updateSpendingBreakdown(transactions)
                    updateGoalStatus(transactions)
                    updateTransactionsList(transactions)
                }
            }
        }
    }

    //  Observe subscriptions
    private fun observeSubscriptions() {
        lifecycleScope.launch {
            database.getSubscriptionDao().observeAll().collect { subs ->
                withContext(Dispatchers.Main) {
                    updateSubscriptionsList(subs)
                }
            }
        }
    }

    //  Spending Breakdown
    private fun updateSpendingBreakdown(transactions: List<TransactionModel>) {
        val spent = mutableMapOf<String, Double>()
        categoryNames.forEach { spent[it] = 0.0 }
        transactions.forEach { tx ->
            val cat = tx.category ?: return@forEach
            if (spent.containsKey(cat)) spent[cat] = spent[cat]!! + tx.amount
        }

        categoryNames.forEachIndexed { i, catName ->
            val row       = findViewById<View>(categoryRowIds[i])
            val catSpent  = spent[catName] ?: 0.0
            val catBudget = budgets[catName] ?: 1.0
            val progress  = (catSpent / catBudget).coerceIn(0.0, 1.0)

            row.findViewById<ImageView>(R.id.catIcon).setImageResource(
                categoryIconMap[catName] ?: R.drawable.ic_lucide_list
            )
            row.findViewById<TextView>(R.id.catName).text    = catName.uppercase()
            row.findViewById<TextView>(R.id.catAmounts).text = "£%.2f / £%.2f".format(catSpent, catBudget)

            val barFill  = row.findViewById<View>(R.id.catBarFill)
            val statusTv = row.findViewById<TextView>(R.id.catStatus)
            barFill.setBackgroundColor(getColor(if (catSpent > catBudget) R.color.bar_red else categoryBarColors[i]))

            barFill.post {
                val parentWidth = (barFill.parent as View).width
                ValueAnimator.ofInt(barFill.layoutParams.width, (parentWidth * progress).toInt()).apply {
                    duration     = 600
                    interpolator = android.view.animation.DecelerateInterpolator()
                    addUpdateListener {
                        val lp = barFill.layoutParams
                        lp.width = it.animatedValue as Int
                        barFill.layoutParams = lp
                    }
                    start()
                }
            }

            when {
                catSpent > catBudget        -> { statusTv.text = "OVER BUDGET";  statusTv.setTextColor(getColor(R.color.bar_red)) }
                catSpent / catBudget > 0.85 -> { statusTv.text = "NEARLY THERE"; statusTv.setTextColor(getColor(R.color.bar_amber)) }
                else                        -> { statusTv.text = "ON TARGET";    statusTv.setTextColor(getColor(R.color.green_primary)) }
            }
        }
    }

    //  Goal status
    private fun updateGoalStatus(transactions: List<TransactionModel>) {
        val saved        = budgets.values.sum() - transactions.sumOf { it.amount }
        val goalStatusTv = findViewById<TextView>(R.id.heroGoalStatus)
        val savedNoteTv  = findViewById<TextView>(R.id.heroSavedNote)
        val allOnTarget  = categoryNames.all { cat ->
            transactions.filter { it.category == cat }.sumOf { it.amount } <= (budgets[cat] ?: 1.0)
        }

        goalStatusTv.text = if (allOnTarget) "On Track" else "Needs Attention"
        goalStatusTv.setTextColor(getColor(if (allOnTarget) R.color.green_accent else R.color.bar_amber))

        if (saved >= 0) {
            savedNoteTv.text = "SAVED %.2f MORE THIS MONTH".format(saved)
            savedNoteTv.setTextColor(getColor(R.color.text_tertiary))
        } else {
            savedNoteTv.text = "OVER BUDGET BY %.2f".format(-saved)
            savedNoteTv.setTextColor(getColor(R.color.bar_red))
        }
    }

    //  Subscriptions list (live from DB)
    private fun updateSubscriptionsList(subs: List<SubscriptionModel>) {
        val container = findViewById<LinearLayout>(R.id.subscriptionsContainer)
        container.removeAllViews()

        if (subs.isEmpty()) {
            val empty = TextView(this).apply {
                text     = "No subscriptions yet"
                textSize = 13f
                setTextColor(getColor(R.color.text_tertiary))
                setPadding(0, 8, 0, 8)
            }
            container.addView(empty)
            return
        }

        subs.forEachIndexed { index, sub ->
            val row  = LayoutInflater.from(this).inflate(R.layout.item_subscription, container, false)
            val logo = row.findViewById<TextView>(R.id.subLogo)

            logo.text = sub.description.firstOrNull()?.uppercase() ?: "?"
            logo.backgroundTintList = android.content.res.ColorStateList.valueOf(
                getColor(if (sub.type == SubscriptionType.MONTHLY) R.color.green_primary else R.color.paramount_blue)
            )

            row.findViewById<TextView>(R.id.subName).text   = sub.description
            row.findViewById<TextView>(R.id.subDate).text   = if (sub.type == SubscriptionType.MONTHLY) "Billed monthly" else "Billed yearly"
            row.findViewById<TextView>(R.id.subAmount).text = "%.2f".format(sub.amount)
            row.findViewById<TextView>(R.id.subFreq).text   = if (sub.type == SubscriptionType.MONTHLY) "/mo" else "/yr"
            row.findViewById<View>(R.id.subDivider).visibility = if (index == subs.lastIndex) View.GONE else View.VISIBLE

            container.addView(row)
        }
    }

    //  Transactions list (live from DB)
    private fun updateTransactionsList(transactions: List<TransactionModel>) {
        val container = findViewById<LinearLayout>(R.id.transactionsContainer)
        container.removeAllViews()

        if (transactions.isEmpty()) {
            val empty = TextView(this).apply {
                text     = "No transactions yet"
                textSize = 13f
                setTextColor(getColor(R.color.text_tertiary))
                setPadding(0, 8, 0, 8)
            }
            container.addView(empty)
            return
        }

        transactions.forEachIndexed { index, transaction ->
            val row  = LayoutInflater.from(this).inflate(R.layout.item_transaction, container, false)
            val logo = row.findViewById<TextView>(R.id.transactionLogo)

            logo.text = transaction.description.firstOrNull()?.uppercase() ?: "?"
            logo.backgroundTintList = android.content.res.ColorStateList.valueOf(
                getColor(R.color.green_primary) // R.color.paramount_blue
            )

            row.findViewById<TextView>(R.id.transactionTitle).text   = transaction.description
            row.findViewById<TextView>(R.id.transactionDescription).text   = transaction.category ?: "Uncategorized"
            row.findViewById<TextView>(R.id.transactionAmount).text = "£%.2f".format(transaction.amount)
            row.findViewById<View>(R.id.transactionDivider).visibility = if (index == transactions.lastIndex) View.GONE else View.VISIBLE

            container.addView(row)
        }
    }

    //  Bottom nav
    private fun setupScreenActions() {
        // Bottom nav click handling lives in NavbarFragment.
        // Keep MainActivity wiring limited to views in activity_main.
        findViewById<TextView>(R.id.btnEditSubs).setOnClickListener {
            startActivity(android.content.Intent(this, SubscriptionsActivity::class.java))
        }
    }

    override fun onBottomNavFabClicked() {
        openDrawer()
    }

    //  Add Transaction drawer
    private fun setupAddTransactionDrawer() {
        val display = findViewById<TextView>(R.id.amountDisplay)
        val input   = findViewById<TextInputEditText>(R.id.inputName)
        val spinner = findViewById<Spinner>(R.id.spinnerCategory)

        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("----") + categoryNames + listOf("Other")
        )

        listOf(R.id.btn5 to 5.0, R.id.btn10 to 10.0, R.id.btn20 to 20.0, R.id.btn50 to 50.0)
            .forEach { (id, value) ->
                findViewById<Button>(id).setOnClickListener {
                    currentAmount += value
                    display.text = "%.2f".format(currentAmount)
                }
            }

        findViewById<Button>(R.id.btnSaveTransaction).setOnClickListener {
            if (currentAmount == 0.0) {
                Toast.makeText(this, "Please add an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name     = input.text?.toString()?.trim() ?: ""
            val selected = spinner.selectedItem?.toString()
            val category = if (selected == "----" || selected == "Other") null else selected

            lifecycleScope.launch(Dispatchers.IO) {

                // If Subscriptions selected, save to subscriptions table
                // and link the transaction to it via subscriptionId
                var linkedSubId: UUID? = null
                if (category == "Subscriptions") {
                    linkedSubId = UUID.randomUUID()
                    database.getSubscriptionDao().upsert(
                        SubscriptionModel(
                            id          = linkedSubId,
                            amount      = currentAmount,
                            description = name.ifEmpty { "Subscription" },
                            type        = SubscriptionType.MONTHLY
                        )
                    )
                }

                // Always save a transaction, linked to subscription if applicable
                database.getTransactionDao().upsert(
                    TransactionModel(
                        id             = UUID.randomUUID(),
                        createdAt      = Instant.now(),
                        updatedAt      = null,
                        amount         = currentAmount,
                        description    = name.ifEmpty { category ?: "Transaction" },
                        subscriptionId = linkedSubId,
                        category       = category
                    )
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Saved!", Toast.LENGTH_SHORT).show()
                    currentAmount = 0.0
                    display.text  = "0.00"
                    input.text?.clear()
                    spinner.setSelection(0)
                    closeDrawer()
                }
            }
        }

        findViewById<View>(R.id.drawerScrim).setOnClickListener { closeDrawer() }
    }

    private fun openDrawer() {
        val scrim  = findViewById<View>(R.id.drawerScrim)
        val drawer = findViewById<LinearLayout>(R.id.addTransactionDrawer)
        scrim.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(scrim, View.ALPHA, 0f, 1f).apply { duration = 250; start() }
        drawer.post {
            ObjectAnimator.ofFloat(drawer, View.TRANSLATION_Y, drawer.height.toFloat(), 0f).apply {
                duration     = 350
                interpolator = android.view.animation.DecelerateInterpolator(2f)
                start()
            }
        }
    }

    private fun closeDrawer() {
        val scrim  = findViewById<View>(R.id.drawerScrim)
        val drawer = findViewById<LinearLayout>(R.id.addTransactionDrawer)
        ObjectAnimator.ofFloat(scrim, View.ALPHA, 1f, 0f).apply {
            duration = 250
            start()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) { scrim.visibility = View.GONE }
            })
        }
        ObjectAnimator.ofFloat(drawer, View.TRANSLATION_Y, 0f, drawer.height.toFloat()).apply {
            duration     = 300
            interpolator = android.view.animation.AccelerateInterpolator()
            start()
        }
    }
}
