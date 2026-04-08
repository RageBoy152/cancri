
package com.example.cancri.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cancri.data.SubscriptionType
import java.util.UUID

@Entity(tableName = "subscriptions")
data class SubscriptionModel(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: UUID,
    @ColumnInfo(name = "amount")
    val amount: Double,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "type")
    val type: SubscriptionType
)
