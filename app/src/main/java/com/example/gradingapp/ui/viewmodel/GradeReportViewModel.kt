package com.example.gradingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gradingapp.data.entity.*
import com.example.gradingapp.data.repository.GradingRepository
import com.example.gradingapp.data.repository.GradeComputationRepository
import com.example.gradingapp.data.repository.QuarterlyGrade
import com.example.gradingapp.data.repository.SemesterGrade
import com.example.gradingapp.data.repository.FinalGrade
import com.example.gradingapp.utils.TransmutationTable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentGradeReport(
    val student: StudentEntity,
    val subject: SubjectEntity,
    val semester1Quarter1: QuarterlyGrade?,
    val semester1Quarter2: QuarterlyGrade?,
    val semester2Quarter1: QuarterlyGrade?,
    val semester2Quarter2: QuarterlyGrade?,
    val semester1Grade: SemesterGrade?,
    val semester2Grade: SemesterGrade?,
    val finalGrade: FinalGrade?
)

data class ClassGradesSummary(
    val section: SectionEntity,
    val subject: SubjectEntity,
    val semester: Int,
    val quarter: Int,
    val studentGrades: List<Pair<StudentEntity, QuarterlyGrade>>,
    val classAverage: Double,
    val passingCount: Int,
    val totalCount: Int
)

data class GradeReportUiState(
    val sections: List<SectionEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val selectedSection: SectionEntity? = null,
    val selectedSubject: SubjectEntity? = null,
    val selectedStudent: StudentEntity? = null,
    val reportType: ReportType = ReportType.STUDENT_REPORT,
    val studentGradeReport: StudentGradeReport? = null,
    val classGradesSummary: ClassGradesSummary? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

enum class ReportType {
    STUDENT_REPORT,
    CLASS_SUMMARY
}

@HiltViewModel
class GradeReportViewModel @Inject constructor(
    private val repository: GradingRepository,
    private val gradeRepository: GradeComputationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GradeReportUiState())
    val uiState: StateFlow<GradeReportUiState> = _uiState.asStateFlow()

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
            _uiState.update {
                it.copy(
                    selectedSection = section,
                    selectedSubject = null,
                    selectedStudent = null,
                    studentGradeReport = null,
                    classGradesSummary = null
                )
            }

            if (section != null) {
                // Load subjects for this section
                repository.getSubjectsBySection(section.id).collect { subjects ->
                    _uiState.update { it.copy(subjects = subjects) }
                }
            }
        }
    }

    fun selectSubject(subject: SubjectEntity?) {
        _uiState.update {
            it.copy(
                selectedSubject = subject,
                selectedStudent = null,
                studentGradeReport = null,
                classGradesSummary = null
            )
        }
    }

    fun selectStudent(student: StudentEntity?) {
        _uiState.update {
            it.copy(
                selectedStudent = student,
                studentGradeReport = null
            )
        }

        if (student != null && _uiState.value.selectedSubject != null) {
            generateStudentReport()
        }
    }

    fun setReportType(reportType: ReportType) {
        _uiState.update {
            it.copy(
                reportType = reportType,
                studentGradeReport = null,
                classGradesSummary = null
            )
        }
    }

    fun generateStudentReport() {
        val student = _uiState.value.selectedStudent
        val subject = _uiState.value.selectedSubject

        if (student == null || subject == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                combine(
                    gradeRepository.getQuarterlyGrade(student.id, subject.id, 1, 1),
                    gradeRepository.getQuarterlyGrade(student.id, subject.id, 1, 2),
                    gradeRepository.getQuarterlyGrade(student.id, subject.id, 2, 1),
                    gradeRepository.getQuarterlyGrade(student.id, subject.id, 2, 2),
                    gradeRepository.getSemesterGrade(student.id, subject.id, 1),
                    gradeRepository.getSemesterGrade(student.id, subject.id, 2),
                    gradeRepository.getFinalGrade(student.id, subject.id)
                ) { s1q1, s1q2, s2q1, s2q2, sem1, sem2, final ->

                    val report = StudentGradeReport(
                        student = student,
                        subject = subject,
                        semester1Quarter1 = if (s1q1.isComplete) s1q1 else null,
                        semester1Quarter2 = if (s1q2.isComplete) s1q2 else null,
                        semester2Quarter1 = if (s2q1.isComplete) s2q1 else null,
                        semester2Quarter2 = if (s2q2.isComplete) s2q2 else null,
                        semester1Grade = if (sem1.isComplete) sem1 else null,
                        semester2Grade = if (sem2.isComplete) sem2 else null,
                        finalGrade = if (final.isComplete) final else null
                    )

                    _uiState.update {
                        it.copy(
                            studentGradeReport = report,
                            isLoading = false
                        )
                    }
                }.collect()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to generate student report: ${e.message}"
                    )
                }
            }
        }
    }

    fun generateClassSummary(semester: Int, quarter: Int) {
        val section = _uiState.value.selectedSection
        val subject = _uiState.value.selectedSubject

        if (section == null || subject == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                repository.getStudentsBySection(section.id).collect { students ->
                    if (students.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                classGradesSummary = ClassGradesSummary(
                                    section = section,
                                    subject = subject,
                                    semester = semester,
                                    quarter = quarter,
                                    studentGrades = emptyList(),
                                    classAverage = 0.0,
                                    passingCount = 0,
                                    totalCount = 0
                                ),
                                isLoading = false
                            )
                        }
                        return@collect
                    }

                    val gradeFlows = students.map { student ->
                        gradeRepository.getQuarterlyGrade(student.id, subject.id, semester, quarter)
                            .map { grade -> student to grade }
                    }

                    combine(gradeFlows) { studentGradePairs ->
                        val completeGrades = studentGradePairs.filter { (_, grade) -> grade.isComplete }
                        val transmutedGrades = completeGrades.map { (_, grade) -> grade.transmutedGrade }

                        val classAverage = if (transmutedGrades.isNotEmpty()) {
                            transmutedGrades.average()
                        } else 0.0

                        val passingCount = transmutedGrades.count { it >= 75 }

                        val summary = ClassGradesSummary(
                            section = section,
                            subject = subject,
                            semester = semester,
                            quarter = quarter,
                            studentGrades = studentGradePairs.toList(),
                            classAverage = classAverage,
                            passingCount = passingCount,
                            totalCount = students.size
                        )

                        _uiState.update {
                            it.copy(
                                classGradesSummary = summary,
                                isLoading = false
                            )
                        }
                    }.collect()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to generate class summary: ${e.message}"
                    )
                }
            }
        }
    }

    fun getStudentsForSection() {
        val section = _uiState.value.selectedSection ?: return

        viewModelScope.launch {
            repository.getStudentsBySection(section.id).collect { students ->
                // You can expose this through another StateFlow if needed for student selection
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}