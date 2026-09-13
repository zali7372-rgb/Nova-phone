package hu.nova.mobile.domain.model

enum class MemoryCategory { PREFERENCE, FACT, OTHER }

data class Memory(
    val id: Long,
    val content: String,
    val category: MemoryCategory,
    val createdAt: Long
)
