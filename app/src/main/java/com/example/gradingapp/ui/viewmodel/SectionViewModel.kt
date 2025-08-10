package com.example.gradingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gradingapp.data.entity.SectionEntity
import com.example.gradingapp.data.repository.GradingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectionUiState(
    val sections: List<SectionEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SectionViewModel @Inject constructor(
    private val repository: GradingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SectionUiState())
    val uiState: StateFlow<SectionUiState> = _uiState.asStateFlow()

    // StateFlow for all sections
    val sections: StateFlow<List<SectionEntity>> = repository.getAllSections()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // StateFlow that combines sections with UI state
    val sectionUiState: StateFlow<SectionUiState> = combine(
        sections,
        _uiState
    ) { sectionsList, currentState ->
        currentState.copy(sections = sectionsList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SectionUiState()
    )

    /**
     * Add a new section
     */
    fun addSection(name: String, level: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Section name cannot be empty"
            )
            return
        }

        if (level.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Section level cannot be empty"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val section = SectionEntity(
                name = name.trim(),
                level = level.trim()
            )

            repository.insertSection(section).fold(
                onSuccess = { sectionId ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Section '$name' added successfully"
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to add section"
                    )
                }
            )
        }
    }

    /**
     * Update an existing section
     */
    fun updateSection(section: SectionEntity, newName: String, newLevel: String) {
        if (newName.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Section name cannot be empty"
            )
            return
        }

        if (newLevel.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Section level cannot be empty"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val updatedSection = section.copy(
                name = newName.trim(),
                level = newLevel.trim()
            )

            repository.updateSection(updatedSection).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Section updated successfully"
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to update section"
                    )
                }
            )
        }
    }

    /**
     * Delete a section
     */
    fun deleteSection(section: SectionEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            repository.deleteSection(section).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Section '${section.name}' deleted successfully"
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to delete section"
                    )
                }
            )
        }
    }

    /**
     * Get section by ID
     */
    suspend fun getSectionById(sectionId: Int): SectionEntity? {
        return repository.getSectionById(sectionId)
    }

    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    /**
     * Clear all messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}