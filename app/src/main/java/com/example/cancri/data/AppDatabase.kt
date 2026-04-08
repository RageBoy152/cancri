package com.example.cancri.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.cancri.data.dao.SubscriptionDao
import com.example.cancri.data.dao.TransactionDao
import com.example.cancri.data.model.SubscriptionModel
import com.example.cancri.data.model.TransactionModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

@Database(entities = [SubscriptionModel::class, TransactionModel::class], version = 4)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getSubscriptionDao(): SubscriptionDao
    abstract fun getTransactionDao(): TransactionDao

    companion object {

        @Volatile
        private var instance: AppDatabase? = null

        // v1 -> v2: added category column
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN category TEXT DEFAULT NULL")
            }
        }

        // v2 -> v3: wipe stale seed data so subscriptions and transactions stay in sync
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove the old seeded subscriptions
                db.execSQL("DELETE FROM subscriptions WHERE id IN ('11111111-1111-1111-1111-111111111111','22222222-2222-2222-2222-222222222222')")
                // Remove the old seeded transactions
                db.execSQL("DELETE FROM transactions WHERE id IN ('33333333-3333-3333-3333-333333333333','44444444-4444-4444-4444-444444444444','55555555-5555-5555-5555-555555555555')")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return instance ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cancri_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                instance = newInstance
                newInstance
            }
        }
    }

    // onCreate only runs on a fresh install — no seed data needed now,
    // everything is added by the user via the app
    private class AppDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // No seed data — users add their own subscriptions and transactions
        }
    }
}
