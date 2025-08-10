package com.example.gradingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gradingapp.data.entity.*
import com.example.gradingapp.data.repository.GradingRepository
import com.example.gradingapp.data.repository.GradeComputationRepository
import com.example.gradingapp.data.repository.QuarterlyGrade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScoreInputFilter(
    val selectedSection: SectionEntity? = null,
    val selectedSubject: SubjectEntity? = null,
    val selectedSemester: Int = 1,
    val selectedQuarter: Int = 1
)

data class StudentScoreData(
    val student: StudentEntity,
    val writtenWorks: List<ScoreEntity> = emptyList(),
    val performanceTasks: List<ScoreEntity> = emptyList(),
    val exam: ScoreEntity? = null,
    val quarterlyGrade: QuarterlyGrade? = null
)

data class ScoreInputUiState(
    val sections: List<SectionEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val students: List<StudentEntity> = emptyList(),
    val filter: ScoreInputFilter = ScoreInputFilter(),
    val studentScores: List<StudentScoreData> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isFiltersValid: Boolean = false
)

data class ScoreEntry(
    val studentId: Int,
    val scoreType: String, // "WW", "PT", "EXAM"
    val number: Int,
    val rawScore: String = "",
    val maxScore: String = "",
    val rawScoreError: String? = null,
    val maxScoreError: String? = null
)

@HiltViewModel
class ScoreInputViewModel @Inject constructor(
    private val repository: GradingRepository,
    private val gradeRepository: GradeComputationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScoreInputUiState())
    val uiState: StateFlow<ScoreInputUiState> = _uiState.asStateFlow()

    private val _scoreEntries = MutableStateFlow<List<ScoreEntry>>(emptyList())
    val scoreEntries: StateFlow<List<ScoreEntry>> = _scoreEntries.asStateFlow()

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

    fun updateSelectedSection(section: SectionEntity?) {
        viewModelScope.launch {
            val newFilter = _uiState.value.filter.copy(selectedSection = section, selectedSubject = null)
            _uiState.update { it.copy(filter = newFilter, isFiltersValid = false) }

            if (section != null) {
                // Load subjects for this section
                repository.getSubjectsBySection(section.id).collect { subjects ->
                    _uiState.update { it.copy(subjects = subjects) }
                }
            }
        }
    }

    fun updateSelectedSubject(subject: SubjectEntity?) {
        val newFilter = _uiState.value.filter.copy(selectedSubject = subject)
        val isValid = newFilter.selectedSection != null && newFilter.selectedSubject != null
        _uiState.update {
            it.copy(
                filter = newFilter,
                isFiltersValid = isValid
            )
        }

        if (isValid) {
            loadStudentScores()
        }
    }

    fun updateSelectedSemester(semester: Int) {
        val newFilter = _uiState.value.filter.copy(selectedSemester = semester)
        _uiState.update { it.copy(filter = newFilter) }

        if (_uiState.value.isFiltersValid) {
            loadStudentScores()
        }
    }

    fun updateSelectedQuarter(quarter: Int) {
        val newFilter = _uiState.value.filter.copy(selectedQuarter = quarter)
        _uiState.update { it.copy(filter = newFilter) }

        if (_uiState.value.isFiltersValid) {
            loadStudentScores()
        }
    }

    private fun loadStudentScores() {
        val filter = _uiState.value.filter
        val section = filter.selectedSection
        val subject = filter.selectedSubject

        if (section == null || subject == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                repository.getStudentsBySection(section.id).collect { students ->
                    val studentScoresData = students.map { student ->
                        combine(
                            repository.getScoresBySemesterQuarter(
                                student.id, subject.id, filter.selectedSemester, filter.selectedQuarter
                            ),
                            gradeRepository.getQuarterlyGrade(
                                student.id, subject.id, filter.selectedSemester, filter.selectedQuarter
                            )
                        ) { scores, grade ->
                            val wwScores = scores.filter { it.scoreType == "WW" }
                            val ptScores = scores.filter { it.scoreType == "PT" }
                            val examScore = scores.find { it.scoreType == "EXAM" }

                            StudentScoreData(
                                student = student,
                                writtenWorks = wwScores,
                                performanceTasks = ptScores,
                                exam = examScore,
                                quarterlyGrade = grade
                            )
                        }
                    }

                    combine(studentScoresData) { scores ->
                        _uiState.update {
                            it.copy(
                                students = students,
                                studentScores = scores.toList(),
                                isLoading = false
                            )
                        }
                    }.collect()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load student scores: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateScoreEntry(
        studentId: Int,
        scoreType: String,
        number: Int,
        rawScore: String,
        maxScore: String
    ) {
        _scoreEntries.update { entries ->
            val existingEntryIndex = entries.indexOfFirst {
                it.studentId == studentId && it.scoreType == scoreType && it.number == number
            }

            val entry = ScoreEntry(
                studentId = studentId,
                scoreType = scoreType,
                number = number,
                rawScore = rawScore,
                maxScore = maxScore,
                rawScoreError = validateScore(rawScore, maxScore).first,
                maxScoreError = validateScore(rawScore, maxScore).second
            )

            if (existingEntryIndex >= 0) {
                entries.toMutableList().apply { set(existingEntryIndex, entry) }
            } else {
                entries + entry
            }
        }
    }

    private fun validateScore(rawScore: String, maxScore: String): Pair<String?, String?> {
        val rawScoreInt = rawScore.toIntOrNull()
        val maxScoreInt = maxScore.toIntOrNull()

        val rawError = when {
            rawScore.isBlank() -> null
            rawScoreInt == null -> "Invalid number"
            rawScoreInt < 0 -> "Score cannot be negative"
            else -> null
        }

        val maxError = when {
            maxScore.isBlank() -> null
            maxScoreInt == null -> "Invalid number"
            maxScoreInt <= 0 -> "Max score must be greater than 0"
            else -> null
        }

        val rangeError = if (rawScoreInt != null && maxScoreInt != null && rawScoreInt > maxScoreInt) {
            "Raw score cannot exceed max score"
        } else null

        return Pair(rawError ?: rangeError, maxError)
    }

    fun saveScore(studentId: Int, scoreType: String, number: Int) {
        val entry = _scoreEntries.value.find {
            it.studentId == studentId && it.scoreType == scoreType && it.number == number
        } ?: return

        val rawScore = entry.rawScore.toIntOrNull() ?: return
        val maxScore = entry.maxScore.toIntOrNull() ?: return

        if (entry.rawScoreError != null || entry.maxScoreError != null) return

        val filter = _uiState.value.filter
        val sectionId = filter.selectedSection?.id ?: return
        val subjectId = filter.selectedSubject?.id ?: return

        viewModelScope.launch {
            val score = ScoreEntity(
                studentId = studentId,
                subjectId = subjectId,
                sectionId = sectionId,
                semester = filter.selectedSemester,
                quarter = filter.selectedQuarter,
                scoreType = scoreType,
                number = number,
                rawScore = rawScore,
                maxScore = maxScore
            )

            repository.insertScore(score)
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = "Failed to save score: ${error.message}")
                    }
                }
        }
    }

    fun saveAllScores() {
        viewModelScope.launch {
            val validEntries = _scoreEntries.value.filter { entry ->
                entry.rawScore.isNotBlank() &&
                        entry.maxScore.isNotBlank() &&
                        entry.rawScoreError == null &&
                        entry.maxScoreError == null
            }

            validEntries.forEach { entry ->
                saveScore(entry.studentId, entry.scoreType, entry.number)
            }

            // Clear saved entries
            _scoreEntries.update { entries ->
                entries.filter { entry ->
                    !validEntries.contains(entry)
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}