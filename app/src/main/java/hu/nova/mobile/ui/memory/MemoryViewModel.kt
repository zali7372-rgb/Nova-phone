package hu.nova.mobile.ui.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.nova.mobile.data.repository.MemoryRepository
import hu.nova.mobile.domain.model.Memory
import hu.nova.mobile.domain.model.MemoryCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(private val memoryRepository: MemoryRepository) : ViewModel() {

    val memories: StateFlow<List<Memory>> = memoryRepository.observeMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMemory(content: String, category: MemoryCategory) {
        if (content.isBlank()) return
        viewModelScope.launch { memoryRepository.addMemory(content, category) }
    }

    fun deleteMemory(memory: Memory) {
        viewModelScope.launch { memoryRepository.deleteMemory(memory) }
    }

    fun clearAll() {
        viewModelScope.launch { memoryRepository.clearAll() }
    }
}
