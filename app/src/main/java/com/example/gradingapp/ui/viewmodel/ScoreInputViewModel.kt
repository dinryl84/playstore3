// File path: app/src/main/java/com/example/gradingapp/ui/viewmodel/ScoreInputViewModel.kt

package com.example.gradingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gradingapp.data.entity.*
import com.example.gradingapp.data.repository.GradingRepository
import com.example.gradingapp.utils.TransmutationTable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentScoreData(
    val student: StudentEntity,
    val writtenWorks: MutableMap<Int, ScoreEntry> = mutableMapOf(), // key: number (1-10)
    val performanceTasks: MutableMap<Int, ScoreEntry> = mutableMapOf(), // key: number (1-10)
    val quarterlyAssessment: ScoreEntry? = null,
    val computedGrade: ComputedGrade? = null
)

data class ScoreEntry(
    val rawScore: Int,
    val maxScore: Int,
    val isValid: Boolean = true
)

data class ComputedGrade(
    val wwAverage: Double,
    val ptAverage: Double,
    val examScore: Double,
    val wwWeighted: Double,
    val ptWeighted: Double,
    val examWeighted: Double,
    val initialGrade: Double,
    val transmutedGrade: Int
)

data class ScoreInputUiState(
    val sections: List<SectionEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val availableSubjectsForSection: List<SubjectEntity> = emptyList(),
    val studentsScoreData: List<StudentScoreData> = emptyList(),
    val selectedSectionId: Int? = null,
    val selectedSubjectId: Int? = null,
    val selectedSemester: Int = 1,
    val selectedQuarter: Int = 1,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ScoreInputViewModel @Inject constructor(
    private val repository: GradingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScoreInputUiState())
    val uiState: StateFlow<ScoreInputUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Using Flow.first() to get current values from existing repository methods
                val sections = repository.getAllSections().first()
                val subjects = repository.getAllSubjects().first()
                _uiState.update {
                    it.copy(
                        sections = sections,
                        subjects = subjects,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load initial data: ${e.message}"
                    )
                }
            }
        }
    }

    fun onSectionSelected(sectionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedSectionId = sectionId) }
            loadSubjectsForSection(sectionId)
        }
    }

    private fun loadSubjectsForSection(sectionId: Int) {
        viewModelScope.launch {
            try {
                // Using existing repository method
                val availableSubjects = repository.getSubjectsBySection(sectionId).first()
                _uiState.update {
                    it.copy(
                        availableSubjectsForSection = availableSubjects,
                        selectedSubjectId = null,
                        studentsScoreData = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to load subjects: ${e.message}")
                }
            }
        }
    }

    fun onSubjectSelected(subjectId: Int) {
        _uiState.update { it.copy(selectedSubjectId = subjectId) }
        loadStudentsAndScores()
    }

    fun onSemesterSelected(semester: Int) {
        _uiState.update { it.copy(selectedSemester = semester) }
        if (_uiState.value.selectedSectionId != null && _uiState.value.selectedSubjectId != null) {
            loadStudentsAndScores()
        }
    }

    fun onQuarterSelected(quarter: Int) {
        _uiState.update { it.copy(selectedQuarter = quarter) }
        if (_uiState.value.selectedSectionId != null && _uiState.value.selectedSubjectId != null) {
            loadStudentsAndScores()
        }
    }

    private fun loadStudentsAndScores() {
        val state = _uiState.value
        val sectionId = state.selectedSectionId ?: return
        val subjectId = state.selectedSubjectId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Using existing repository methods
                val students = repository.getStudentsBySection(sectionId).first()

                val studentsScoreData = students.map { student ->
                    // Get scores for this student and subject for the selected semester/quarter
                    val scores = repository.getScoresBySemesterQuarter(
                        student.id, subjectId, state.selectedSemester, state.selectedQuarter
                    ).first()

                    val wwScores = scores.filter { it.scoreType == "WW" }
                        .associate { it.number to ScoreEntry(it.rawScore, it.maxScore) }
                        .toMutableMap()
                    val ptScores = scores.filter { it.scoreType == "PT" }
                        .associate { it.number to ScoreEntry(it.rawScore, it.maxScore) }
                        .toMutableMap()
                    val examScore = scores.find { it.scoreType == "EXAM" }
                        ?.let { ScoreEntry(it.rawScore, it.maxScore) }

                    val scoreData = StudentScoreData(
                        student = student,
                        writtenWorks = wwScores,
                        performanceTasks = ptScores,
                        quarterlyAssessment = examScore
                    )

                    scoreData.copy(computedGrade = computeGrade(scoreData))
                }

                _uiState.update {
                    it.copy(
                        studentsScoreData = studentsScoreData,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load students and scores: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateWrittenWorkScore(studentId: Int, number: Int, rawScore: Int, maxScore: Int) {
        val updatedData = _uiState.value.studentsScoreData.map { studentData ->
            if (studentData.student.id == studentId) {
                val isValid = rawScore >= 0 && rawScore <= maxScore && maxScore > 0
                val scoreEntry = ScoreEntry(rawScore, maxScore, isValid)
                studentData.writtenWorks[number] = scoreEntry
                studentData.copy(computedGrade = computeGrade(studentData))
            } else {
                studentData
            }
        }
        _uiState.update { it.copy(studentsScoreData = updatedData) }

        // Save to database
        saveScore(studentId, "WW", number, rawScore, maxScore)
    }

    fun updatePerformanceTaskScore(studentId: Int, number: Int, rawScore: Int, maxScore: Int) {
        val updatedData = _uiState.value.studentsScoreData.map { studentData ->
            if (studentData.student.id == studentId) {
                val isValid = rawScore >= 0 && rawScore <= maxScore && maxScore > 0
                val scoreEntry = ScoreEntry(rawScore, maxScore, isValid)
                studentData.performanceTasks[number] = scoreEntry
                studentData.copy(computedGrade = computeGrade(studentData))
            } else {
                studentData
            }
        }
        _uiState.update { it.copy(studentsScoreData = updatedData) }

        // Save to database
        saveScore(studentId, "PT", number, rawScore, maxScore)
    }

    fun updateQuarterlyAssessmentScore(studentId: Int, rawScore: Int, maxScore: Int) {
        val updatedData = _uiState.value.studentsScoreData.map { studentData ->
            if (studentData.student.id == studentId) {
                val isValid = rawScore >= 0 && rawScore <= maxScore && maxScore > 0
                val scoreEntry = ScoreEntry(rawScore, maxScore, isValid)
                val updatedStudentData = studentData.copy(quarterlyAssessment = scoreEntry)
                updatedStudentData.copy(computedGrade = computeGrade(updatedStudentData))
            } else {
                studentData
            }
        }
        _uiState.update { it.copy(studentsScoreData = updatedData) }

        // Save to database
        saveScore(studentId, "EXAM", 1, rawScore, maxScore)
    }

    private fun saveScore(studentId: Int, scoreType: String, number: Int, rawScore: Int, maxScore: Int) {
        val state = _uiState.value
        val sectionId = state.selectedSectionId ?: return
        val subjectId = state.selectedSubjectId ?: return

        viewModelScope.launch {
            try {
                val scoreEntity = ScoreEntity(
                    id = 0, // Room will auto-generate
                    studentId = studentId,
                    subjectId = subjectId,
                    sectionId = sectionId,
                    semester = state.selectedSemester,
                    quarter = state.selectedQuarter,
                    scoreType = scoreType,
                    number = number,
                    rawScore = rawScore,
                    maxScore = maxScore
                )

                // Using existing repository method - first try to insert, if it fails, update
                val insertResult = repository.insertScore(scoreEntity)
                if (insertResult.isFailure) {
                    // If insert fails (score might already exist), try to update
                    repository.updateScore(scoreEntity)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to save score: ${e.message}")
                }
            }
        }
    }

    private fun computeGrade(studentData: StudentScoreData): ComputedGrade? {
        // Need at least one score in each category to compute
        if (studentData.writtenWorks.isEmpty() ||
            studentData.performanceTasks.isEmpty() ||
            studentData.quarterlyAssessment == null) {
            return null
        }

        // Calculate averages
        val wwAverage = studentData.writtenWorks.values
            .filter { it.isValid }
            .map { it.rawScore.toDouble() / it.maxScore * 100 }
            .takeIf { it.isNotEmpty() }
            ?.average() ?: 0.0

        val ptAverage = studentData.performanceTasks.values
            .filter { it.isValid }
            .map { it.rawScore.toDouble() / it.maxScore * 100 }
            .takeIf { it.isNotEmpty() }
            ?.average() ?: 0.0

        val examScore = studentData.quarterlyAssessment?.let {
            if (it.isValid) it.rawScore.toDouble() / it.maxScore * 100 else 0.0
        } ?: 0.0

        // Calculate weighted scores
        val wwWeighted = wwAverage * 0.25
        val ptWeighted = ptAverage * 0.50
        val examWeighted = examScore * 0.25

        // Calculate initial grade
        val initialGrade = wwWeighted + ptWeighted + examWeighted

        // Using correct method from TransmutationTable
        val transmutedGrade = TransmutationTable.getTransmutedGrade(initialGrade)

        return ComputedGrade(
            wwAverage = wwAverage,
            ptAverage = ptAverage,
            examScore = examScore,
            wwWeighted = wwWeighted,
            ptWeighted = ptWeighted,
            examWeighted = examWeighted,
            initialGrade = initialGrade,
            transmutedGrade = transmutedGrade
        )
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}