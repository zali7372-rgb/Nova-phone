package hu.nova.mobile.database

import androidx.room.TypeConverter
import hu.nova.mobile.database.entity.MemoryCategory
import hu.nova.mobile.database.entity.MessageSender

class Converters {

    @TypeConverter
    fun fromMessageSender(sender: MessageSender): String = sender.name

    @TypeConverter
    fun toMessageSender(value: String): MessageSender = MessageSender.valueOf(value)

    @TypeConverter
    fun fromMemoryCategory(category: MemoryCategory): String = category.name

    @TypeConverter
    fun toMemoryCategory(value: String): MemoryCategory = MemoryCategory.valueOf(value)
}
