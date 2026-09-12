package hu.nova.mobile.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import hu.nova.mobile.database.dao.ConversationDao
import hu.nova.mobile.database.dao.MemoryDao
import hu.nova.mobile.database.dao.MessageDao
import hu.nova.mobile.database.dao.SettingsDao
import hu.nova.mobile.database.entity.ConversationEntity
import hu.nova.mobile.database.entity.MemoryEntity
import hu.nova.mobile.database.entity.MessageEntity
import hu.nova.mobile.database.entity.SettingsEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NovaDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile private var instance: NovaDatabase? = null

        fun getInstance(context: Context): NovaDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NovaDatabase::class.java,
                    "nova.db"
                ).build().also { instance = it }
            }
    }
}
