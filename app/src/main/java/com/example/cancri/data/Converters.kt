package com.example.cancri.data

import androidx.room.TypeConverter
import java.time.Instant
import java.util.UUID

class Converters {

    // uuid - string conversions
    @TypeConverter
    fun uuidToString(value: UUID?): String? = value?.toString()
    @TypeConverter
    fun stringToUuid(value: String?): UUID? = value?.let(UUID::fromString)

    // unix timestamp - Instant conversions
    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    // subscription type enum - string conversions
    @TypeConverter
    fun subscriptionTypeToString(value: SubscriptionType?): String? = value?.name?.lowercase()
    @TypeConverter
    fun stringToSubscriptionType(value: String?): SubscriptionType? = when (value?.lowercase()) {
        "monthly" -> SubscriptionType.MONTHLY
        "yearly" -> SubscriptionType.YEARLY
        else -> null
    }
}
