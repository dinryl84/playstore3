package com.example.gradingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gradingapp.data.entity.StudentEntity
import com.example.gradingapp.data.entity.SectionEntity
import com.example.gradingapp.data.repository.GradingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentUiState(
    val students: List<StudentEntity> = emptyList(),
    val sections: List<SectionEntity> = emptyList(),
    val selectedSection: SectionEntity? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAddingStudent: Boolean = false
)

data class AddEditStudentState(
    val name: String = "",
    val lrn: String = "",
    val gender: String = "Male",
    val selectedSectionId: Int = 0,
    val nameError: String? = null,
    val lrnError: String? = null,
    val sectionError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class StudentViewModel @Inject constructor(
    private val repository: GradingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    private val _addEditState = MutableStateFlow(AddEditStudentState())
    val addEditState: StateFlow<AddEditStudentState> = _addEditState.asStateFlow()

    private val _selectedSectionId = MutableStateFlow<Int?>(null)

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                combine(
                    repository.getAllStudents(),
                    repository.getAllSections(),
                    _selectedSectionId
                ) { students, sections, selectedSectionId ->
                    val filteredStudents = if (selectedSectionId != null) {
                        students.filter { it.sectionId == selectedSectionId }
                    } else students

                    val selectedSection = sections.find { it.id == selectedSectionId }

                    StudentUiState(
                        students = filteredStudents,
                        sections = sections,
                        selectedSection = selectedSection,
                        isLoading = false
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load data: ${e.message}"
                    )
                }
            }
        }
    }

    fun selectSection(sectionId: Int?) {
        _selectedSectionId.value = sectionId
    }

    fun showAddStudentDialog() {
        _uiState.update { it.copy(isAddingStudent = true) }
        resetAddEditState()
    }

    fun hideAddStudentDialog() {
        _uiState.update { it.copy(isAddingStudent = false) }
        resetAddEditState()
    }

    fun updateStudentName(name: String) {
        _addEditState.update {
            it.copy(
                name = name,
                nameError = if (name.isBlank()) "Name cannot be empty" else null
            )
        }
    }

    fun updateStudentLrn(lrn: String) {
        _addEditState.update {
            it.copy(
                lrn = lrn,
                lrnError = when {
                    lrn.isBlank() -> "LRN cannot be empty"
                    lrn.length != 12 -> "LRN must be 12 digits"
                    !lrn.all { char -> char.isDigit() } -> "LRN must contain only numbers"
                    else -> null
                }
            )
        }
    }

    fun updateStudentGender(gender: String) {
        _addEditState.update { it.copy(gender = gender) }
    }

    fun updateSelectedSectionId(sectionId: Int) {
        _addEditState.update {
            it.copy(
                selectedSectionId = sectionId,
                sectionError = if (sectionId == 0) "Please select a section" else null
            )
        }
    }

    fun addStudent() {
        val currentState = _addEditState.value

        // Validate inputs
        val nameError = if (currentState.name.isBlank()) "Name cannot be empty" else null
        val lrnError = when {
            currentState.lrn.isBlank() -> "LRN cannot be empty"
            currentState.lrn.length != 12 -> "LRN must be 12 digits"
            !currentState.lrn.all { it.isDigit() } -> "LRN must contain only numbers"
            else -> null
        }
        val sectionError = if (currentState.selectedSectionId == 0) "Please select a section" else null

        if (nameError != null || lrnError != null || sectionError != null) {
            _addEditState.update {
                it.copy(
                    nameError = nameError,
                    lrnError = lrnError,
                    sectionError = sectionError
                )
            }
            return
        }

        viewModelScope.launch {
            _addEditState.update { it.copy(isLoading = true) }

            val student = StudentEntity(
                name = currentState.name.trim(),
                lrn = currentState.lrn.trim(),
                gender = currentState.gender,
                sectionId = currentState.selectedSectionId
            )

            repository.insertStudent(student)
                .onSuccess {
                    _addEditState.update { AddEditStudentState() }
                    _uiState.update { it.copy(isAddingStudent = false) }
                }
                .onFailure { error ->
                    _addEditState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to add student"
                        )
                    }
                }
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student)
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = "Failed to delete student: ${error.message}")
                    }
                }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
        _addEditState.update { it.copy(errorMessage = null) }
    }

    private fun resetAddEditState() {
        _addEditState.value = AddEditStudentState()
    }
}