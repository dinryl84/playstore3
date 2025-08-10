package com.example.gradingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gradingapp.data.entity.SubjectEntity
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

data class SubjectUiState(
    val subjects: List<SubjectEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val repository: GradingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectUiState())
    val uiState: StateFlow<SubjectUiState> = _uiState.asStateFlow()

    // StateFlow for all subjects
    val subjects: StateFlow<List<SubjectEntity>> = repository.getAllSubjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // StateFlow that combines subjects with UI state
    val subjectUiState: StateFlow<SubjectUiState> = combine(
        subjects,
        _uiState
    ) { subjectsList, currentState ->
        currentState.copy(subjects = subjectsList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubjectUiState()
    )

    /**
     * Add a new subject
     */
    fun addSubject(name: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Subject name cannot be empty"
            )
            return
        }

        // Additional validation for common subject name formats
        val trimmedName = name.trim()
        if (trimmedName.length < 2) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Subject name must be at least 2 characters long"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val subject = SubjectEntity(name = trimmedName)

            repository.insertSubject(subject).fold(
                onSuccess = { subjectId ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Subject '$trimmedName' added successfully"
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to add subject"
                    )
                }
            )
        }
    }

    /**
     * Update an existing subject
     */
    fun updateSubject(subject: SubjectEntity, newName: String) {
        if (newName.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Subject name cannot be empty"
            )
            return
        }

        val trimmedName = newName.trim()
        if (trimmedName.length < 2) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Subject name must be at least 2 characters long"
            )
            return
        }

        // No need to update if name hasn't changed
        if (subject.name == trimmedName) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No changes made to subject name"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val updatedSubject = subject.copy(name = trimmedName)

            repository.updateSubject(updatedSubject).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Subject updated successfully"
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to update subject"
                    )
                }
            )
        }
    }

    /**
     * Delete a subject
     */
    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            repository.deleteSubject(subject).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Subject '${subject.name}' deleted successfully"
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to delete subject"
                    )
                }
            )
        }
    }

    /**
     * Get subject by ID
     */
    suspend fun getSubjectById(subjectId: Int): SubjectEntity? {
        return repository.getSubjectById(subjectId)
    }

    /**
     * Get subjects assigned to a specific section
     */
    fun getSubjectsBySection(sectionId: Int): StateFlow<List<SubjectEntity>> {
        return repository.getSubjectsBySection(sectionId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    /**
     * Assign a subject to a section
     */
    fun assignSubjectToSection(subjectId: Int, sectionId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            // Check if already assigned
            val isAlreadyAssigned = repository.isSubjectAssignedToSection(subjectId, sectionId)
            if (isAlreadyAssigned) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Subject is already assigned to this section"
                )
                return@launch
            }

            repository.assignSubjectToSection(subjectId, sectionId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Subject assigned to section successfully"
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to assign subject to section"
                    )
                }
            )
        }
    }

    /**
     * Remove a subject from a section
     */
    fun unassignSubjectFromSection(subjectId: Int, sectionId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            repository.unassignSubjectFromSection(subjectId, sectionId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Subject removed from section successfully"
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to remove subject from section"
                    )
                }
            )
        }
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