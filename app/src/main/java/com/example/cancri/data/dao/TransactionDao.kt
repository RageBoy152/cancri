package com.example.cancri.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.cancri.data.model.TransactionModel
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionModel>)

    @Update
    suspend fun update(transaction: TransactionModel)

    @Delete
    suspend fun delete(transaction: TransactionModel)

    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    fun observeAll(): Flow<List<TransactionModel>>

    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    suspend fun getAll(): List<TransactionModel>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun findById(id: UUID): TransactionModel?

    @Query("SELECT * FROM transactions WHERE subscription = :subscriptionId ORDER BY created_at DESC")
    fun observeBySubscription(subscriptionId: UUID): Flow<List<TransactionModel>>

    // ── Delete all transactions for a given subscription ID ───────────────
    @Query("DELETE FROM transactions WHERE subscription = :subscriptionId")
    suspend fun deleteBySubscriptionId(subscriptionId: UUID)

    // ── Update amount for all transactions linked to a subscription ───────
    @Query("UPDATE transactions SET amount = :newAmount WHERE subscription = :subscriptionId")
    suspend fun updateAmountBySubscriptionId(subscriptionId: UUID, newAmount: Double)
}