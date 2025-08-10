package com.example.gradingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gradingapp.data.entity.SectionEntity
import com.example.gradingapp.data.entity.SubjectEntity
import com.example.gradingapp.data.repository.GradingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectionSubjectUiState(
    val sections: List<SectionEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val selectedSection: SectionEntity? = null,
    val subjectsForSelectedSection: List<SubjectEntity> = emptyList(),
    val availableSubjectsForSection: List<SubjectEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showAddSectionDialog: Boolean = false,
    val showAddSubjectDialog: Boolean = false,
    val showAssignSubjectDialog: Boolean = false
)

data class AddSectionState(
    val name: String = "",
    val level: String = "Grade 11",
    val nameError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class AddSubjectState(
    val name: String = "",
    val nameError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SectionSubjectViewModel @Inject constructor(
    private val repository: GradingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SectionSubjectUiState())
    val uiState: StateFlow<SectionSubjectUiState> = _uiState.asStateFlow()

    private val _addSectionState = MutableStateFlow(AddSectionState())
    val addSectionState: StateFlow<AddSectionState> = _addSectionState.asStateFlow()

    private val _addSubjectState = MutableStateFlow(AddSubjectState())
    val addSubjectState: StateFlow<AddSubjectState> = _addSubjectState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                combine(
                    repository.getAllSections(),
                    repository.getAllSubjects()
                ) { sections, subjects ->
                    _uiState.update {
                        it.copy(
                            sections = sections,
                            subjects = subjects,
                            isLoading = false
                        )
                    }
                }.collect()
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

    fun selectSection(section: SectionEntity?) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedSection = section) }

            if (section != null) {
                // Load subjects for the selected section
                repository.getSubjectsBySection(section.id).collect { subjectsForSection ->
                    val availableSubjects = _uiState.value.subjects.filter { subject ->
                        subjectsForSection.none { it.id == subject.id }
                    }

                    _uiState.update {
                        it.copy(
                            subjectsForSelectedSection = subjectsForSection,
                            availableSubjectsForSection = availableSubjects
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        subjectsForSelectedSection = emptyList(),
                        availableSubjectsForSection = emptyList()
                    )
                }
            }
        }
    }

    // Section operations
    fun showAddSectionDialog() {
        _uiState.update { it.copy(showAddSectionDialog = true) }
        resetAddSectionState()
    }

    fun hideAddSectionDialog() {
        _uiState.update { it.copy(showAddSectionDialog = false) }
        resetAddSectionState()
    }

    fun updateSectionName(name: String) {
        _addSectionState.update {
            it.copy(
                name = name,
                nameError = if (name.isBlank()) "Section name cannot be empty" else null
            )
        }
    }

    fun updateSectionLevel(level: String) {
        _addSectionState.update { it.copy(level = level) }
    }

    fun addSection() {
        val currentState = _addSectionState.value

        if (currentState.name.isBlank()) {
            _addSectionState.update { it.copy(nameError = "Section name cannot be empty") }
            return
        }

        viewModelScope.launch {
            _addSectionState.update { it.copy(isLoading = true) }

            val section = SectionEntity(
                name = currentState.name.trim(),
                level = currentState.level
            )

            repository.insertSection(section)
                .onSuccess {
                    _addSectionState.value = AddSectionState()
                    _uiState.update { it.copy(showAddSectionDialog = false) }
                }
                .onFailure { error ->
                    _addSectionState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to add section"
                        )
                    }
                }
        }
    }

    // Subject operations
    fun showAddSubjectDialog() {
        _uiState.update { it.copy(showAddSubjectDialog = true) }
        resetAddSubjectState()
    }

    fun hideAddSubjectDialog() {
        _uiState.update { it.copy(showAddSubjectDialog = false) }
        resetAddSubjectState()
    }

    fun updateSubjectName(name: String) {
        _addSubjectState.update {
            it.copy(
                name = name,
                nameError = if (name.isBlank()) "Subject name cannot be empty" else null
            )
        }
    }

    fun addSubject() {
        val currentState = _addSubjectState.value

        if (currentState.name.isBlank()) {
            _addSubjectState.update { it.copy(nameError = "Subject name cannot be empty") }
            return
        }

        viewModelScope.launch {
            _addSubjectState.update { it.copy(isLoading = true) }

            val subject = SubjectEntity(name = currentState.name.trim())

            repository.insertSubject(subject)
                .onSuccess {
                    _addSubjectState.value = AddSubjectState()
                    _uiState.update { it.copy(showAddSubjectDialog = false) }
                }
                .onFailure { error ->
                    _addSubjectState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to add subject"
                        )
                    }
                }
        }
    }

    // Subject assignment operations
    fun showAssignSubjectDialog() {
        _uiState.update { it.copy(showAssignSubjectDialog = true) }
    }

    fun hideAssignSubjectDialog() {
        _uiState.update { it.copy(showAssignSubjectDialog = false) }
    }

    fun assignSubjectToSection(subjectId: Int, sectionId: Int) {
        viewModelScope.launch {
            repository.assignSubjectToSection(subjectId, sectionId)
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = "Failed to assign subject: ${error.message}")
                    }
                }
        }
    }

    fun unassignSubjectFromSection(subjectId: Int, sectionId: Int) {
        viewModelScope.launch {
            repository.unassignSubjectFromSection(subjectId, sectionId)
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = "Failed to unassign subject: ${error.message}")
                    }
                }
        }
    }

    fun deleteSection(section: SectionEntity) {
        viewModelScope.launch {
            repository.deleteSection(section)
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = "Failed to delete section: ${error.message}")
                    }
                }
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = "Failed to delete subject: ${error.message}")
                    }
                }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
        _addSectionState.update { it.copy(errorMessage = null) }
        _addSubjectState.update { it.copy(errorMessage = null) }
    }

    private fun resetAddSectionState() {
        _addSectionState.value = AddSectionState()
    }

    private fun resetAddSubjectState() {
        _addSubjectState.value = AddSubjectState()
    }
}