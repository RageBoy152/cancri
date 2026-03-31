package com.example.cancri.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.cancri.data.model.SubscriptionModel
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subscription: SubscriptionModel)

    @Update
    suspend fun update(subscription: SubscriptionModel)

    @Delete
    suspend fun delete(subscription: SubscriptionModel)

    @Query("SELECT * FROM subscriptions ORDER BY description ASC")
    fun observeAll(): Flow<List<SubscriptionModel>>

    @Query("SELECT * FROM subscriptions ORDER BY description ASC")
    suspend fun getAll(): List<SubscriptionModel>

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    suspend fun findById(id: UUID): SubscriptionModel?
}
