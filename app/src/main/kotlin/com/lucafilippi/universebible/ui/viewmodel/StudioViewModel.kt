package com.lucafilippi.universebible.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucafilippi.universebible.data.repository.Slate
import com.lucafilippi.universebible.data.repository.StudioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface StudioState {
    data object Loading : StudioState
    data object ReadFailed : StudioState
    data class Ready(val slate: Slate) : StudioState
}

class StudioViewModel(val repository: StudioRepository) : ViewModel() {
    private val current = MutableStateFlow<StudioState>(StudioState.Loading)
    val state = current.asStateFlow()
    private var observation: Job? = null

    init { reload() }

    fun reload() {
        observation?.cancel()
        current.value = StudioState.Loading
        observation = viewModelScope.launch {
            repository.slate.catch { current.value = StudioState.ReadFailed }
                .collect { current.value = StudioState.Ready(it) }
        }
    }
}
