package com.lucafilippi.universebible.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucafilippi.universebible.data.repository.UniverseRepository
import com.lucafilippi.universebible.domain.model.Universe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface UniversesState {
    data object Loading : UniversesState
    data object LoadFailed : UniversesState
    data class Ready(val universes: List<Universe>) : UniversesState
}

enum class UniverseMutation { Idle, Saving, Saved, Deleting, Deleted, Failed }

class UniversesViewModel(private val repository: UniverseRepository) : ViewModel() {
    private val _state = MutableStateFlow<UniversesState>(UniversesState.Loading)
    val state = _state.asStateFlow()

    private val _mutation = MutableStateFlow(UniverseMutation.Idle)
    val mutation = _mutation.asStateFlow()

    private var observation: Job? = null

    init {
        reload()
    }

    fun reload() {
        observation?.cancel()
        _state.value = UniversesState.Loading
        observation = viewModelScope.launch {
            repository.observeUniverses()
                .catch { _state.value = UniversesState.LoadFailed }
                .collect { _state.value = UniversesState.Ready(it) }
        }
    }

    fun save(id: String?, name: String, description: String, studioName: String) {
        mutate(UniverseMutation.Saving, UniverseMutation.Saved) {
            if (id == null) {
                repository.create(name, description, studioName)
            } else {
                repository.edit(id, name, description, studioName)
            }
        }
    }

    fun delete(id: String) {
        mutate(UniverseMutation.Deleting, UniverseMutation.Deleted) { repository.delete(id) }
    }

    fun dismissMutation() {
        if (_mutation.value != UniverseMutation.Saving && _mutation.value != UniverseMutation.Deleting) {
            _mutation.value = UniverseMutation.Idle
        }
    }

    private fun mutate(
        inProgress: UniverseMutation,
        completed: UniverseMutation,
        operation: suspend () -> Unit,
    ) {
        if (_mutation.value != UniverseMutation.Idle && _mutation.value != UniverseMutation.Failed) return
        _mutation.value = inProgress
        viewModelScope.launch {
            try {
                operation()
                _mutation.value = completed
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                android.util.Log.e("UniversesViewModel", "Universe database operation failed", exception)
                _mutation.value = UniverseMutation.Failed
            }
        }
    }
}
