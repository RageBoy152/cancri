/* Cancri - money management app
Programming for Mobile - COMP08068
Team - Matt Miller, Kyle McNamee, Jaimie Neilson, Andrew Gilmour
Date created - 24/03/26
Ver 1.0
 */


package com.example.cancri

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.cancri.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val database = AppDatabase.getDatabase(this, dbScope)
        lifecycleScope.launch(Dispatchers.IO) {
            launch {
                database.getSubscriptionDao().observeAll().collect { subscriptions ->
                    Log.d("MainActivity", "Subscriptions: $subscriptions")
                }
            }
            launch {
                database.getTransactionDao().observeAll().collect { transactions ->
                    Log.d("MainActivity", "Transactions: $transactions")
                }
            }
        }
    }
}
