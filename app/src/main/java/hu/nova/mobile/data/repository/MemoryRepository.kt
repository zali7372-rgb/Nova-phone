package hu.nova.mobile.data.repository

import hu.nova.mobile.database.dao.MemoryDao
import hu.nova.mobile.database.entity.MemoryEntity
import hu.nova.mobile.domain.model.Memory
import hu.nova.mobile.domain.model.MemoryCategory as DomainMemoryCategory
import hu.nova.mobile.database.entity.MemoryCategory as EntityMemoryCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemoryRepository(private val memoryDao: MemoryDao) {

    fun observeMemories(): Flow<List<Memory>> =
        memoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAllOnce(): List<Memory> = memoryDao.getAllOnce().map { it.toDomain() }

    suspend fun addMemory(content: String, category: DomainMemoryCategory): Long {
        return memoryDao.insert(
            MemoryEntity(
                content = content.trim(),
                category = category.toEntity(),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMemory(memory: Memory) {
        memoryDao.delete(
            MemoryEntity(id = memory.id, content = memory.content, category = memory.category.toEntity(), createdAt = memory.createdAt)
        )
    }

    suspend fun clearAll() = memoryDao.deleteAll()

    private fun MemoryEntity.toDomain() = Memory(
        id = id,
        content = content,
        category = when (category) {
            EntityMemoryCategory.PREFERENCE -> DomainMemoryCategory.PREFERENCE
            EntityMemoryCategory.FACT -> DomainMemoryCategory.FACT
            EntityMemoryCategory.OTHER -> DomainMemoryCategory.OTHER
        },
        createdAt = createdAt
    )

    private fun DomainMemoryCategory.toEntity() = when (this) {
        DomainMemoryCategory.PREFERENCE -> EntityMemoryCategory.PREFERENCE
        DomainMemoryCategory.FACT -> EntityMemoryCategory.FACT
        DomainMemoryCategory.OTHER -> EntityMemoryCategory.OTHER
    }
}
