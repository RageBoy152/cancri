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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.example.cancri.data.AppDatabase
import com.example.cancri.data.SubscriptionType
import com.example.cancri.data.UserPreferences
import com.example.cancri.data.model.SubscriptionModel
import com.example.cancri.data.model.TransactionModel
import com.example.cancri.notifications.NotificationSettings
import com.example.cancri.ui.AddTransactionBottomSheet
import com.example.cancri.ui.NavbarFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity(), NavbarFragment.Listener {

    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase
    private var latestTransactions: List<TransactionModel> = emptyList()
    private var latestSubscriptions: List<SubscriptionModel> = emptyList()
    private lateinit var userPreferences: UserPreferences

    private var budgets: Map<String, Double> = emptyMap()
    private val goalBudgetCategories = listOf("Bills", "Debts", "Savings Goals")
    private val notificationPermissionRequestCode = 101

    private val categoryRowIds    = listOf(R.id.catBills, R.id.catSubscriptions, R.id.catDebts, R.id.catSavings)
    private val categoryNames     = listOf("Bills", "Subscriptions", "Debts", "Savings Goals")
    private val categoryIconMap = mapOf(
        "Savings Goals" to R.drawable.ic_lucide_piggy_bank,
        "Debts" to R.drawable.ic_lucide_wallet_cards,
        "Subscriptions" to R.drawable.ic_lucide_list,
        "Bills" to R.drawable.ic_lucide_lamp_ceiling
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        budgets = loadBudgetsFromPrefs()
        updateWeeklyStreak()

        database = AppDatabase.getDatabase(this, dbScope)
        userPreferences = UserPreferences(this)

        setupHero()
        setupScreenActions()
        animatePanelEntry()
        observeTransactions()
        observeSubscriptions()
        checkNotificationPermission()
    }

    private fun updateWeeklyStreak() {
        val prefs = getSharedPreferences("cancri_prefs", MODE_PRIVATE)
        val today = LocalDate.now()

        val originStr = prefs.getString("streak_origin_date", "") ?: ""
        val lastStr   = prefs.getString("last_active_date", "") ?: ""

        var origin = if (originStr.isEmpty()) today else LocalDate.parse(originStr)
        val last   = if (lastStr.isEmpty()) today else LocalDate.parse(lastStr)

        // Reset if they haven't been here in over 7 days
        if (ChronoUnit.DAYS.between(last, today) > 7) {
            origin = today
        }

        // Calculate how many 7-day blocks (weeks) they have completed
        val weeksPassed = (ChronoUnit.DAYS.between(origin, today) / 7).toInt() + 1

        // Save today as the new "last active"
        prefs.edit()
            .putString("streak_origin_date", origin.toString())
            .putString("last_active_date", today.toString())
            .putInt("weeks_count", weeksPassed)
            .apply()

        renderStreakUI(weeksPassed)
    }

    private fun renderStreakUI(weeks: Int) {
        findViewById<TextView>(R.id.streakLabel).text = "$weeks Week Streak"

        val claws = listOf(
            findViewById<ImageView>(R.id.streakClaw1),
            findViewById<ImageView>(R.id.streakClaw2),
            findViewById<ImageView>(R.id.streakClaw3),
            findViewById<ImageView>(R.id.streakClaw4),
            findViewById<ImageView>(R.id.streakClaw5)
        )

        // Show 1-5 filled claws. If streak is 6, it wraps back to 1 (6 % 5)
        val displayCount = if (weeks % 5 == 0 && weeks > 0) 5 else weeks % 5

        claws.forEachIndexed { i, view ->
            view?.setImageResource(if (i < displayCount) R.drawable.cancri_streak_filled else R.drawable.cancri_streak_outline)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    notificationPermissionRequestCode
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == notificationPermissionRequestCode) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                NotificationSettings.disableAll(this)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        budgets = loadBudgetsFromPrefs()
        findViewById<TextView>(R.id.heroName).text = userPreferences.getDisplayName()
        updateSpendingBreakdown(latestTransactions, latestSubscriptions)
        updateGoalStatus(latestTransactions)
    }

    //  Hero
    private fun setupHero() {
        findViewById<TextView>(R.id.heroGreeting).text = getTimeGreeting()
        findViewById<TextView>(R.id.heroName).text = userPreferences.getDisplayName()
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
                    latestTransactions = transactions
                    updateSpendingBreakdown(transactions, latestSubscriptions)
                    updateGoalStatus(transactions)
                    updateTransactionsList(transactions)
                    updateSubscriptionsList(latestSubscriptions)
                }
            }
        }
    }

    //  Observe subscriptions
    private fun observeSubscriptions() {
        lifecycleScope.launch {
            database.getSubscriptionDao().observeAll().collect { subs ->
                withContext(Dispatchers.Main) {
                    latestSubscriptions = subs
                    updateSpendingBreakdown(latestTransactions, subs)
                    updateSubscriptionsList(subs)
                }
            }
        }
    }

    //  Spending Breakdown
    private fun updateSpendingBreakdown(
        transactions: List<TransactionModel>,
        subscriptions: List<SubscriptionModel>
    ) {
        val spent = mutableMapOf<String, Double>()
        categoryNames.forEach { spent[it] = 0.0 }
        transactions.forEach { tx ->
            val cat = tx.category ?: return@forEach
            if (spent.containsKey(cat)) spent[cat] = spent[cat]!! + tx.amount
        }

        val today = java.time.LocalDate.now()
        val txBySubscriptionId = transactions
            .filter { it.subscriptionId != null }
            .groupBy { it.subscriptionId!! }

        val latestTxBySubscriptionId = txBySubscriptionId.mapValues { (_, txs) -> txs.maxByOrNull { it.createdAt } }
        val dueSubscriptionsThisMonth = subscriptions.filter { sub ->
            when (sub.type) {
                SubscriptionType.MONTHLY -> true
                SubscriptionType.YEARLY -> (sub.billingMonth ?: today.monthValue) == today.monthValue
            }
        }
        val dueSubscriptionsAmountThisMonth = dueSubscriptionsThisMonth.sumOf { it.amount }
        val paidSubscriptionsThisMonth = dueSubscriptionsThisMonth.count { sub ->
            val latest = latestTxBySubscriptionId[sub.id] ?: return@count false
            val lastPaidDate = latest.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
            !lastPaidDate.isBefore(cycleStartDate(sub, today))
        }
        val subscriptionsLeftThisMonth = (dueSubscriptionsThisMonth.size - paidSubscriptionsThisMonth).coerceAtLeast(0)
        val subscriptionsPaidRatio = if (dueSubscriptionsThisMonth.isEmpty()) {
            1.0
        } else {
            (paidSubscriptionsThisMonth.toDouble() / dueSubscriptionsThisMonth.size.toDouble()).coerceIn(0.0, 1.0)
        }

        categoryNames.forEachIndexed { i, catName ->
            val row       = findViewById<View>(categoryRowIds[i])
            val catSpent  = spent[catName] ?: 0.0
            val catBudget = if (catName == "Subscriptions") {
                dueSubscriptionsAmountThisMonth
            } else {
                budgets[catName] ?: 0.0
            }
            val progress = if (catName == "Subscriptions") {
                subscriptionsPaidRatio
            } else if (catBudget <= 0.0) {
                0.0
            } else {
                (catSpent / catBudget).coerceIn(0.0, 1.0)
            }

            row.findViewById<ImageView>(R.id.catIcon).setImageResource(
                categoryIconMap[catName] ?: R.drawable.ic_lucide_list
            )
            row.findViewById<TextView>(R.id.catName).text = catName.uppercase()
            row.findViewById<TextView>(R.id.catAmounts).text =
                "${userPreferences.formatCurrency(catSpent)} / ${userPreferences.formatCurrency(catBudget)}"

            val barFill  = row.findViewById<View>(R.id.catBarFill)
            val statusTv = row.findViewById<TextView>(R.id.catStatus)

            val paidRatio = if (catBudget <= 0.0) 0.0 else (catSpent / catBudget)
            val atGoal = kotlin.math.abs(paidRatio - 1) < 0.0001

            val (statusText, statusColor) = when (catName) {
                "Bills" -> when {
                    paidRatio == 0.0  -> "build momentum" to R.color.text_tertiary
                    paidRatio < 0.2  -> "keep up the momentum" to R.color.bar_red
                    paidRatio < 0.5  -> "you're making good progress" to R.color.bar_red
                    paidRatio < 1  -> "nearly there" to R.color.bar_amber
                    else             -> "paid off!" to R.color.green_primary
                }
                "Subscriptions" -> {
                    val color = when {
                        subscriptionsPaidRatio >= 1.0 -> R.color.green_primary
                        subscriptionsPaidRatio >= 0.5 -> R.color.bar_amber
                        else -> R.color.bar_red
                    }
                    if (subscriptionsLeftThisMonth == 1) {
                        "1 subscription left this month" to color
                    } else {
                        "$subscriptionsLeftThisMonth subscriptions left this month" to color
                    }
                }
                "Debts" -> when {
                    paidRatio == 0.0  -> "head on your way" to R.color.text_tertiary
                    paidRatio < 0.5  -> "you're on your way" to R.color.bar_red
                    paidRatio < 1.0  -> "nearly there" to R.color.bar_amber
                    atGoal           -> "target reached!" to R.color.green_primary
                    else             -> "goal exceeded!" to R.color.green_primary
                }
                "Savings Goals" -> when {
                    paidRatio == 0.0 -> "build toward your goal" to R.color.text_tertiary
                    paidRatio <= 0.5 -> "building toward your goal" to R.color.bar_red
                    paidRatio < 1.0  -> "nearly there" to R.color.bar_amber
                    atGoal           -> "savings goal reached!" to R.color.green_primary
                    else             -> "your saving extra this month!" to R.color.green_primary
                }
                else -> "on target" to R.color.green_primary
            }

            barFill.setBackgroundColor(getColor(statusColor))
            statusTv.text = statusText
            statusTv.setTextColor(getColor(statusColor))

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

        }
    }

    //  Goal status
    private fun updateGoalStatus(transactions: List<TransactionModel>) {
        val budgetTotal = goalBudgetCategories.sumOf { budgets[it] ?: 0.0 }
        val spentTotal = transactions
            .filter { tx -> goalBudgetCategories.contains(tx.category) }
            .sumOf { it.amount }
        val saved        = budgetTotal - spentTotal
        val goalStatusTv = findViewById<TextView>(R.id.heroGoalStatus)
        val savedNoteTv  = findViewById<TextView>(R.id.heroSavedNote)
        val allOnTarget  = goalBudgetCategories.all { cat ->
            transactions.filter { it.category == cat }.sumOf { it.amount } <= (budgets[cat] ?: 1.0)
        }

        goalStatusTv.text = if (allOnTarget) "On Track" else "Needs Attention"
        goalStatusTv.setTextColor(getColor(if (allOnTarget) R.color.green_accent else R.color.bar_amber))

        if (saved >= 0) {
            savedNoteTv.text = "SAVED ${userPreferences.formatCurrency(saved)} MORE THIS MONTH"
            savedNoteTv.setTextColor(getColor(R.color.text_tertiary))
        } else {
            savedNoteTv.text = "OVER BUDGET BY ${userPreferences.formatCurrency(-saved)}"
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

        val today = LocalDate.now()
        val latestTxBySubscriptionId = latestTransactions
            .filter { it.subscriptionId != null }
            .groupBy { it.subscriptionId!! }
            .mapValues { (_, txs) -> txs.maxByOrNull { it.createdAt } }

        val sortedSubs = subs.sortedBy { nextBillingDate(it, today) }

        sortedSubs.forEachIndexed { index, sub ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_subscription, container, false)
            val logo = row.findViewById<TextView>(R.id.subLogo)
            val statusIcon = row.findViewById<ImageView>(R.id.subStatusCircle)
            val nextDate = nextBillingDate(sub, today)
            val cycleStart = cycleStartDate(sub, today)
            val latestTx = latestTxBySubscriptionId[sub.id]
            val latestTxDate = latestTx
                ?.createdAt
                ?.atZone(ZoneId.systemDefault())
                ?.toLocalDate()
            val isComplete = latestTxDate != null && !latestTxDate.isBefore(cycleStart)

            logo.text = sub.description.firstOrNull()?.uppercase() ?: "?"
            logo.backgroundTintList = android.content.res.ColorStateList.valueOf(
                getColor(if (sub.type == SubscriptionType.MONTHLY) R.color.green_primary else R.color.paramount_blue)
            )

            row.findViewById<TextView>(R.id.subName).text = sub.description
            row.findViewById<TextView>(R.id.subDate).text = "Next billing: ${formatDate(nextDate)}"
            row.findViewById<TextView>(R.id.subAmount).text = userPreferences.formatCurrency(sub.amount)
            row.findViewById<TextView>(R.id.subFreq).text = if (sub.type == SubscriptionType.MONTHLY) "/mo" else "/yr"
            row.findViewById<View>(R.id.subDivider).visibility = if (index == sortedSubs.lastIndex) View.GONE else View.VISIBLE

            statusIcon.setImageResource(
                if (isComplete) R.drawable.ic_lucide_circle_check_big else R.drawable.ic_lucide_circle
            )
            statusIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                getColor(if (isComplete) R.color.green_primary else R.color.text_tertiary)
            )

            row.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (isComplete) {
                        // delete this months subscription transaction if we're unchecking
                        latestTx.let { database.getTransactionDao().delete(it) }
                    } else {
                        // add new transaction when checked
                        database.getTransactionDao().upsert(
                            TransactionModel(
                                id = java.util.UUID.randomUUID(),
                                createdAt = Instant.now(),
                                updatedAt = null,
                                amount = sub.amount,
                                description = sub.description,
                                subscriptionId = sub.id,
                                category = "Subscriptions"
                            )
                        )
                    }
                }
            }

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

        transactions.take(20).forEachIndexed { index, transaction ->
            val row  = LayoutInflater.from(this).inflate(R.layout.item_transaction, container, false)
            val logo = row.findViewById<TextView>(R.id.transactionLogo)

            logo.text = transaction.description.firstOrNull()?.uppercase() ?: "?"
            logo.backgroundTintList = android.content.res.ColorStateList.valueOf(
                getColor(R.color.green_primary) // R.color.paramount_blue
            )

            row.findViewById<TextView>(R.id.transactionTitle).text   = transaction.description
            row.findViewById<TextView>(R.id.transactionDescription).text   = transaction.category ?: "Uncategorized"
            row.findViewById<TextView>(R.id.transactionAmount).text = userPreferences.formatCurrency(transaction.amount)
            row.findViewById<View>(R.id.transactionDivider).visibility = if (index == transactions.lastIndex) View.GONE else View.VISIBLE

            container.addView(row)
        }
    }

    //  Bottom nav
    private fun setupScreenActions() {
        // Bottom nav click handling lives in NavbarFragment.
        // Keep MainActivity wiring limited to views in activity_main.
        findViewById<TextView>(R.id.btnEditGoals).setOnClickListener {
            startActivity(android.content.Intent(this, GoalsActivity::class.java))
        }
        findViewById<TextView>(R.id.btnEditSubs).setOnClickListener {
            startActivity(android.content.Intent(this, SubscriptionsActivity::class.java))
        }
        findViewById<TextView>(R.id.btnEditTransactions).setOnClickListener {
            startActivity(android.content.Intent(this, TransactionsActivity::class.java))
        }
    }

    override fun onBottomNavFabClicked() {
        AddTransactionBottomSheet.newInstance(ArrayList(categoryNames))
            .show(supportFragmentManager, AddTransactionBottomSheet.TAG)
    }

    private fun nextBillingDate(subscription: SubscriptionModel, today: LocalDate): LocalDate {
        val day = (subscription.billingDay ?: 1).coerceIn(1, 31)

        return when (subscription.type) {
            SubscriptionType.MONTHLY -> {
                val thisMonth = YearMonth.from(today)
                val thisMonthDate = thisMonth.atDay(day.coerceAtMost(thisMonth.lengthOfMonth()))
                if (thisMonthDate.isBefore(today)) {
                    val nextMonth = thisMonth.plusMonths(1)
                    nextMonth.atDay(day.coerceAtMost(nextMonth.lengthOfMonth()))
                } else {
                    thisMonthDate
                }
            }

            SubscriptionType.YEARLY -> {
                val month = (subscription.billingMonth ?: today.monthValue).coerceIn(1, 12)
                val thisYearMonth = YearMonth.of(today.year, month)
                val thisYearDate = thisYearMonth.atDay(day.coerceAtMost(thisYearMonth.lengthOfMonth()))
                if (thisYearDate.isBefore(today)) {
                    val nextYearMonth = thisYearMonth.plusYears(1)
                    nextYearMonth.atDay(day.coerceAtMost(nextYearMonth.lengthOfMonth()))
                } else {
                    thisYearDate
                }
            }
        }
    }

    private fun cycleStartDate(subscription: SubscriptionModel, today: LocalDate): LocalDate {
        val day = (subscription.billingDay ?: 1).coerceIn(1, 31)

        return when (subscription.type) {
            SubscriptionType.MONTHLY -> {
                val thisMonth = YearMonth.from(today)
                val thisMonthDate = thisMonth.atDay(day.coerceAtMost(thisMonth.lengthOfMonth()))
                if (thisMonthDate.isAfter(today)) {
                    val previousMonth = thisMonth.minusMonths(1)
                    previousMonth.atDay(day.coerceAtMost(previousMonth.lengthOfMonth()))
                } else {
                    thisMonthDate
                }
            }

            SubscriptionType.YEARLY -> {
                val month = (subscription.billingMonth ?: today.monthValue).coerceIn(1, 12)
                val thisYearMonth = YearMonth.of(today.year, month)
                val thisYearDate = thisYearMonth.atDay(day.coerceAtMost(thisYearMonth.lengthOfMonth()))
                if (thisYearDate.isAfter(today)) {
                    val lastYearMonth = thisYearMonth.minusYears(1)
                    lastYearMonth.atDay(day.coerceAtMost(lastYearMonth.lengthOfMonth()))
                } else {
                    thisYearDate
                }
            }
        }
    }

    private fun formatDate(date: LocalDate): String {
        val monthName = date.month.name.lowercase(Locale.UK)
            .replaceFirstChar { it.uppercaseChar() }
        return "$monthName ${date.dayOfMonth}${toOrdinalSuffix(date.dayOfMonth)}"
    }

    private fun toOrdinalSuffix(day: Int): String {
        val mod100 = day % 100
        if (mod100 in 11..13) return "th"
        return when (day % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }

    private fun loadBudgetsFromPrefs(): Map<String, Double> {
        val prefs = getSharedPreferences("cancri_prefs", MODE_PRIVATE)
        return mapOf(
            "Bills" to prefs.getFloat("goal_bills", 900f).toDouble(),
            "Debts" to prefs.getFloat("goal_debts", 200f).toDouble(),
            "Savings Goals" to prefs.getFloat("goal_savings", 300f).toDouble()
        )
    }
}
