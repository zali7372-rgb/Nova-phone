package hu.nova.mobile.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MemoryCategory { PREFERENCE, FACT, OTHER }

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val category: MemoryCategory,
    val createdAt: Long
)
