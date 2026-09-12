package hu.nova.mobile.database.dao

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import hu.nova.mobile.database.entity.SettingsEntity

@Dao
interface SettingsDao {

    @Upsert
    suspend fun upsert(entry: SettingsEntity)

    @Query("SELECT * FROM settings_kv WHERE `key` = :key")
    suspend fun get(key: String): SettingsEntity?
}
