package com.example.gradingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gradingapp.data.entity.StudentEntity
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

data class StudentUiState(
    val students: List<StudentEntity> = emptyList(),
    val sections: List<SectionEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class StudentViewModel @Inject constructor(
    private val repository: GradingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    // StateFlow for all students
    val students: StateFlow<List<StudentEntity>> = repository.getAllStudents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // StateFlow for all sections (needed for student-section assignment)
    val sections: StateFlow<List<SectionEntity>> = repository.getAllSections()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Combined UI state
    val studentUiState: StateFlow<StudentUiState> = combine(
        students,
        sections,
        _uiState
    ) { studentsList, sectionsList, currentState ->
        currentState.copy(
            students = studentsList,
            sections = sectionsList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StudentUiState()
    )

    companion object {
        val VALID_GENDERS = listOf("Male", "Female", "Other")
        const val LRN_MIN_LENGTH = 10
        const val LRN_MAX_LENGTH = 15
        const val NAME_MIN_LENGTH = 2
    }

    /**
     * Add a new student
     */
    fun addStudent(name: String, lrn: String, gender: String, sectionId: Int) {
        val validationResult = validateStudentInput(name, lrn, gender, sectionId)
        if (!validationResult.isValid) {
            _uiState.value = _uiState.value.copy(errorMessage = validationResult.errorMessage)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val student = StudentEntity(
                name = name.trim(),
                lrn = lrn.trim(),
                gender = gender,
                sectionId = sectionId
            )

            repository.insertStudent(student).fold(
                onSuccess = { studentId ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Student '${name.trim()}' added successfully"
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to add student"
                    )
                }
            )
        }
    }

    /**
     * Update an existing student
     */
    fun updateStudent(
        student: StudentEntity,
        newName: String,
        newLrn: String,
        newGender: String,
        newSectionId: Int
    ) {
        val validationResult = validateStudentInput(newName, newLrn, newGender, newSectionId, student.id)
        if (!validationResult.isValid) {
            _uiState.value = _uiState.value.copy(errorMessage = validationResult.errorMessage)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val updatedStudent = student.copy(
                name = newName.trim(),
                lrn = newLrn.trim(),
                gender = newGender,
                sectionId = newSectionId
            )

            repository.updateStudent(updatedStudent).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Student updated successfully"
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to update student"
                    )
                }
            )
        }
    }

    /**
     * Delete a student
     */
    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            repository.deleteStudent(student).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Student '${student.name}' deleted successfully"
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to delete student"
                    )
                }
            )
        }
    }

    /**
     * Get students by section
     */
    fun getStudentsBySection(sectionId: Int): StateFlow<List<StudentEntity>> {
        return repository.getStudentsBySection(sectionId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    /**
     * Transfer student to another section
     */
    fun transferStudentToSection(student: StudentEntity, newSectionId: Int) {
        if (student.sectionId == newSectionId) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Student is already in this section"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val updatedStudent = student.copy(sectionId = newSectionId)

            repository.updateStudent(updatedStudent).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Student transferred to new section successfully"
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to transfer student"
                    )
                }
            )
        }
    }

    /**
     * Get student by ID
     */
    suspend fun getStudentById(studentId: Int): StudentEntity? {
        return repository.getStudentById(studentId)
    }

    /**
     * Validate student input
     */
    private fun validateStudentInput(
        name: String,
        lrn: String,
        gender: String,
        sectionId: Int,
        excludeStudentId: Int? = null
    ): ValidationResult {
        // Validate name
        if (name.isBlank()) {
            return ValidationResult(false, "Student name cannot be empty")
        }

        val trimmedName = name.trim()
        if (trimmedName.length < NAME_MIN_LENGTH) {
            return ValidationResult(false, "Student name must be at least $NAME_MIN_LENGTH characters long")
        }

        // Validate LRN
        if (lrn.isBlank()) {
            return ValidationResult(false, "LRN cannot be empty")
        }

        val trimmedLrn = lrn.trim()
        if (trimmedLrn.length < LRN_MIN_LENGTH || trimmedLrn.length > LRN_MAX_LENGTH) {
            return ValidationResult(false, "LRN must be between $LRN_MIN_LENGTH and $LRN_MAX_LENGTH characters")
        }

        // Validate LRN contains only alphanumeric characters
        if (!trimmedLrn.matches(Regex("^[a-zA-Z0-9]+$"))) {
            return ValidationResult(false, "LRN can only contain letters and numbers")
        }

        // Validate gender
        if (gender !in VALID_GENDERS) {
            return ValidationResult(false, "Gender must be one of: ${VALID_GENDERS.joinToString(", ")}")
        }

        // Validate section exists (basic check)
        if (sectionId <= 0) {
            return ValidationResult(false, "Please select a valid section")
        }

        return ValidationResult(true, null)
    }

    /**
     * Check if LRN is available (for real-time validation)
     */
    fun checkLrnAvailability(lrn: String, excludeStudentId: Int? = null) {
        if (lrn.isBlank()) return

        viewModelScope.launch {
            try {
                // This would need to be implemented in repository if needed for real-time validation
                // For now, validation happens during save
            } catch (e: Exception) {
                // Handle error silently for real-time validation
            }
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

/**
 * Data class for validation results
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
)