package hu.nova.mobile.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Small key/value table used for settings that need to be queried relationally
 * (e.g. joined against usage stats in the future). Everyday scalar preferences
 * (theme, language, provider selection) live in DataStore via PreferencesManager
 * instead, since they don't need SQL querying - this entity exists for
 * completeness/extensibility and is currently used for AI-provider usage counters.
 */
@Entity(tableName = "settings_kv")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val value: String
)
