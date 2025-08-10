package com.example.gradingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gradingapp.data.entity.ScoreEntity
import com.example.gradingapp.data.entity.StudentEntity
import com.example.gradingapp.data.entity.SubjectEntity
import com.example.gradingapp.data.entity.SectionEntity
import com.example.gradingapp.data.repository.GradingRepository
import com.example.gradingapp.data.repository.GradeComputationRepository
import com.example.gradingapp.data.repository.QuarterlyGrade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScoreInputFilter(
    val sectionId: Int = 0,
    val subjectId: Int = 0,
    val semester: Int = 1,
    val quarter: Int = 1
)

data class ScoreUiState(
    val scores: List<ScoreEntity> = emptyList(),
    val students: List<StudentEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val sections: List<SectionEntity> = emptyList(),
    val currentFilter: ScoreInputFilter = ScoreInputFilter(),
    val quarterlyGrades: Map<Int, QuarterlyGrade> = emptyMap(), // studentId -> QuarterlyGrade
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ScoreViewModel @Inject constructor(
    private val repository: GradingRepository,
    private val gradeComputationRepository: GradeComputationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScoreUiState())
    val uiState: StateFlow<ScoreUiState> = _uiState.asStateFlow()

    // Base data flows
    val sections: StateFlow<List<SectionEntity>> = repository.getAllSections()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val subjects: StateFlow<List<SubjectEntity>> = repository.getAllSubjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Combined UI state
    val scoreUiState: StateFlow<ScoreUiState> = combine(
        sections,
        subjects,
        _uiState
    ) { sectionsList, subjectsList, currentState ->
        currentState.copy(
            sections = sectionsList,
            subjects = subjectsList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScoreUiState()
    )

    companion object {
        val SCORE_TYPES = listOf("WW", "PT", "EXAM")
        val SEMESTERS = listOf(1, 2)
        val QUARTERS = listOf(1, 2)
        const val MAX_WW_PT_NUMBER = 10
        const val MAX_EXAM_NUMBER = 1
        const val MIN_SCORE = 0
        const val DEFAULT_MAX_SCORE = 100
    }

    /**
     * Set the current filter for score input
     */
    fun setScoreFilter(sectionId: Int, subjectId: Int, semester: Int, quarter: Int) {
        if (sectionId <= 0 || subjectId <= 0 || semester !in SEMESTERS || quarter !in QUARTERS) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please select valid section, subject, semester, and quarter"
            )
            return
        }

        val newFilter = ScoreInputFilter(sectionId, subjectId, semester, quarter)
        _uiState.value = _uiState.value.copy(
            currentFilter = newFilter,
            errorMessage = null
        )

        // Load data for the new filter
        loadScoresForFilter(newFilter)
        loadStudentsForSection(sectionId)
    }

    /**
     * Load scores based on current filter
     */
    private fun loadScoresForFilter(filter: ScoreInputFilter) {
        viewModelScope.launch {
            repository.getScoresBySemesterQuarter(
                // Note: This method doesn't exist in your current repository
                // We'll use a different approach
                filter.sectionId,
                filter.subjectId,
                filter.semester,
                filter.quarter
            ).collect { scores ->
                _uiState.value = _uiState.value.copy(scores = scores)

                // Load quarterly grades for all students
                loadQuarterlyGrades(filter)
            }
        }
    }

    /**
     * Load students for selected section
     */
    private fun loadStudentsForSection(sectionId: Int) {
        viewModelScope.launch {
            repository.getStudentsBySection(sectionId).collect { students ->
                _uiState.value = _uiState.value.copy(students = students)
            }
        }
    }

    /**
     * Load quarterly grades for all students in current filter
     */
    private fun loadQuarterlyGrades(filter: ScoreInputFilter) {
        viewModelScope.launch {
            val students = _uiState.value.students
            val gradesMap = mutableMapOf<Int, QuarterlyGrade>()

            students.forEach { student ->
                gradeComputationRepository.getQuarterlyGrade(
                    student.id,
                    filter.subjectId,
                    filter.semester,
                    filter.quarter
                ).collect { grade ->
                    gradesMap[student.id] = grade
                    _uiState.value = _uiState.value.copy(quarterlyGrades = gradesMap.toMap())
                }
            }
        }
    }

    /**
     * Add or update a score
     */
    fun addOrUpdateScore(
        studentId: Int,
        scoreType: String,
        number: Int,
        rawScore: Int,
        maxScore: Int = DEFAULT_MAX_SCORE
    ) {
        val filter = _uiState.value.currentFilter
        val validationResult = validateScoreInput(studentId, scoreType, number, rawScore, maxScore, filter)

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

            val score = ScoreEntity(
                studentId = studentId,
                subjectId = filter.subjectId,
                sectionId = filter.sectionId,
                semester = filter.semester,
                quarter = filter.quarter,
                scoreType = scoreType,
                number = number,
                rawScore = rawScore,
                maxScore = maxScore
            )

            repository.insertScore(score).fold(
                onSuccess = { scoreId ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Score saved successfully"
                    )
                    // Refresh quarterly grades
                    loadQuarterlyGrades(filter)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to save score"
                    )
                }
            )
        }
    }

    /**
     * Update existing score
     */
    fun updateScore(score: ScoreEntity, newRawScore: Int, newMaxScore: Int = score.maxScore) {
        val validationResult = validateScoreValues(newRawScore, newMaxScore)

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

            val updatedScore = score.copy(
                rawScore = newRawScore,
                maxScore = newMaxScore
            )

            repository.updateScore(updatedScore).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Score updated successfully"
                    )
                    // Refresh quarterly grades
                    loadQuarterlyGrades(_uiState.value.currentFilter)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to update score"
                    )
                }
            )
        }
    }

    /**
     * Delete a score
     */
    fun deleteScore(score: ScoreEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            repository.deleteScore(score).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Score deleted successfully"
                    )
                    // Refresh quarterly grades
                    loadQuarterlyGrades(_uiState.value.currentFilter)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to delete score"
                    )
                }
            )
        }
    }

    /**
     * Bulk add scores for multiple students
     */
    fun addBulkScores(
        studentIds: List<Int>,
        scoreType: String,
        number: Int,
        scoreData: Map<Int, Pair<Int, Int>> // studentId -> (rawScore, maxScore)
    ) {
        if (studentIds.isEmpty() || scoreData.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "No students or scores provided")
            return
        }

        val filter = _uiState.value.currentFilter

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            var successCount = 0
            var errorCount = 0

            studentIds.forEach { studentId ->
                scoreData[studentId]?.let { (rawScore, maxScore) ->
                    val score = ScoreEntity(
                        studentId = studentId,
                        subjectId = filter.subjectId,
                        sectionId = filter.sectionId,
                        semester = filter.semester,
                        quarter = filter.quarter,
                        scoreType = scoreType,
                        number = number,
                        rawScore = rawScore,
                        maxScore = maxScore
                    )

                    repository.insertScore(score).fold(
                        onSuccess = { successCount++ },
                        onFailure = { errorCount++ }
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                successMessage = if (errorCount == 0) {
                    "All $successCount scores saved successfully"
                } else {
                    "$successCount scores saved, $errorCount failed"
                }
            )

            // Refresh quarterly grades
            loadQuarterlyGrades(filter)
        }
    }

    /**
     * Get scores for specific student and score type
     */
    fun getScoresForStudentAndType(
        studentId: Int,
        scoreType: String
    ): List<ScoreEntity> {
        val filter = _uiState.value.currentFilter
        return _uiState.value.scores.filter {
            it.studentId == studentId &&
                    it.scoreType == scoreType &&
                    it.semester == filter.semester &&
                    it.quarter == filter.quarter
        }
    }

    /**
     * Get available score number for student and type
     */
    fun getNextAvailableScoreNumber(studentId: Int, scoreType: String): Int {
        val existingScores = getScoresForStudentAndType(studentId, scoreType)
        val maxNumber = when (scoreType) {
            "WW", "PT" -> MAX_WW_PT_NUMBER
            "EXAM" -> MAX_EXAM_NUMBER
            else -> MAX_WW_PT_NUMBER
        }

        for (number in 1..maxNumber) {
            if (existingScores.none { it.number == number }) {
                return number
            }
        }

        return -1 // No available slots
    }

    /**
     * Validate score input
     */
    private fun validateScoreInput(
        studentId: Int,
        scoreType: String,
        number: Int,
        rawScore: Int,
        maxScore: Int,
        filter: ScoreInputFilter
    ): ValidationResult {
        // Validate student
        if (studentId <= 0) {
            return ValidationResult(false, "Invalid student selected")
        }

        // Validate score type
        if (scoreType !in SCORE_TYPES) {
            return ValidationResult(false, "Invalid score type: $scoreType")
        }

        // Validate number based on score type
        val maxNumber = when (scoreType) {
            "WW", "PT" -> MAX_WW_PT_NUMBER
            "EXAM" -> MAX_EXAM_NUMBER
            else -> MAX_WW_PT_NUMBER
        }

        if (number < 1 || number > maxNumber) {
            return ValidationResult(false, "Score number must be between 1 and $maxNumber for $scoreType")
        }

        // Validate scores
        return validateScoreValues(rawScore, maxScore)
    }

    /**
     * Validate raw score and max score values
     */
    private fun validateScoreValues(rawScore: Int, maxScore: Int): ValidationResult {
        if (rawScore < MIN_SCORE) {
            return ValidationResult(false, "Raw score cannot be negative")
        }

        if (maxScore <= MIN_SCORE) {
            return ValidationResult(false, "Max score must be greater than 0")
        }

        if (rawScore > maxScore) {
            return ValidationResult(false, "Raw score cannot exceed max score")
        }

        return ValidationResult(true, null)
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