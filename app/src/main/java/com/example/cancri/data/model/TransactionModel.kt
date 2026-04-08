package com.example.cancri.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = SubscriptionModel::class,
            parentColumns = ["id"],
            childColumns = ["subscription"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subscription"])]
)
data class TransactionModel(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: UUID,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant?,
    @ColumnInfo(name = "amount")
    val amount: Double,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "subscription")
    val subscriptionId: UUID?,

    // ── NEW: which spending category this transaction belongs to ──────────
    // Matches the category names used in MainActivity:
    // "Bills", "Subscriptions", "Debts", "Savings Goals", or null for uncategorised
    @ColumnInfo(name = "category")
    val category: String? = null
)