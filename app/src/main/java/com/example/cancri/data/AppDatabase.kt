package com.example.cancri.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.cancri.data.dao.SubscriptionDao
import com.example.cancri.data.dao.TransactionDao
import com.example.cancri.data.model.SubscriptionModel
import com.example.cancri.data.model.TransactionModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

@Database(entities = [SubscriptionModel::class, TransactionModel::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getSubscriptionDao(): SubscriptionDao
    abstract fun getTransactionDao(): TransactionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return instance ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "cancri_database").addCallback(
                    AppDatabaseCallback(scope)).build()
                instance = newInstance
                newInstance
            }
        }
    }

    private class AppDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch {
                // seed database with sample data
                val subscriptionDao = instance?.getSubscriptionDao() ?: return@launch
                val transactionDao = instance?.getTransactionDao() ?: return@launch

                val monthlySubId = UUID.fromString("11111111-1111-1111-1111-111111111111")
                val yearlySubId = UUID.fromString("22222222-2222-2222-2222-222222222222")

                subscriptionDao.upsert(
                    SubscriptionModel(
                        id = monthlySubId,
                        amount = 9.99,
                        description = "Spotify Premium",
                        type = SubscriptionType.MONTHLY
                    )
                )
                subscriptionDao.upsert(
                    SubscriptionModel(
                        id = yearlySubId,
                        amount = 79.99,
                        description = "Adobe Creative Cloud",
                        type = SubscriptionType.YEARLY
                    )
                )

                val now = Instant.now()
                transactionDao.upsert(
                    TransactionModel(
                        id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        createdAt = now,
                        updatedAt = null,
                        amount = 9.99,
                        description = "March renewal",
                        subscriptionId = monthlySubId
                    )
                )
                transactionDao.upsert(
                    TransactionModel(
                        id = UUID.fromString("44444444-4444-4444-4444-444444444444"),
                        createdAt = now.minusSeconds(86_400),
                        updatedAt = now,
                        amount = 79.99,
                        description = "Annual payment",
                        subscriptionId = yearlySubId
                    )
                )
                transactionDao.upsert(
                    TransactionModel(
                        id = UUID.fromString("55555555-5555-5555-5555-555555555555"),
                        createdAt = now.minusSeconds(172_800),
                        updatedAt = null,
                        amount = 54.25,
                        description = "Aldi Shop",
                        subscriptionId = null
                    )
                )
            }
        }
    }
}
