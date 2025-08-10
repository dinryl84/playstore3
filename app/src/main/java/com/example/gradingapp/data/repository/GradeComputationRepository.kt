package com.example.gradingapp.data.repository

import com.example.gradingapp.data.database.GradingDatabase
import com.example.gradingapp.data.entity.ScoreEntity
import com.example.gradingapp.utils.TransmutationTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class ComponentScore(
    val scoreType: String,
    val totalRawScore: Int,
    val totalMaxScore: Int,
    val percentage: Double,
    val weightedScore: Double
)

data class QuarterlyGrade(
    val studentId: Int,
    val subjectId: Int,
    val semester: Int,
    val quarter: Int,
    val writtenWork: ComponentScore,
    val performanceTask: ComponentScore,
    val exam: ComponentScore,
    val initialGrade: Double,
    val transmutedGrade: Int,
    val isComplete: Boolean
)

data class SemesterGrade(
    val studentId: Int,
    val subjectId: Int,
    val semester: Int,
    val quarter1Grade: Int?,
    val quarter2Grade: Int?,
    val semesterGrade: Double?,
    val isComplete: Boolean
)

data class FinalGrade(
    val studentId: Int,
    val subjectId: Int,
    val semester1Grade: Double?,
    val semester2Grade: Double?,
    val finalGrade: Double?,
    val isComplete: Boolean,
    val isPassing: Boolean
)

@Singleton
class GradeComputationRepository @Inject constructor(
    private val database: GradingDatabase
) {
    private val scoreDao = database.scoreDao()

    // Component weights as per DepEd standards
    companion object {
        const val WRITTEN_WORK_WEIGHT = 0.25
        const val PERFORMANCE_TASK_WEIGHT = 0.50
        const val EXAM_WEIGHT = 0.25
    }

    /**
     * Computes quarterly grade for a specific student, subject, semester, and quarter
     */
    fun getQuarterlyGrade(
        studentId: Int,
        subjectId: Int,
        semester: Int,
        quarter: Int
    ): Flow<QuarterlyGrade> {
        return scoreDao.getScoresByStudentSubjectSemesterQuarter(studentId, subjectId, semester, quarter)
            .map { scores ->
                computeQuarterlyGrade(scores, studentId, subjectId, semester, quarter)
            }
    }

    /**
     * Computes semester grade for a specific student and subject
     */
    fun getSemesterGrade(
        studentId: Int,
        subjectId: Int,
        semester: Int
    ): Flow<SemesterGrade> {
        return scoreDao.getScoresByStudentAndSubject(studentId, subjectId)
            .map { allScores ->
                val semesterScores = allScores.filter { it.semester == semester }
                computeSemesterGrade(semesterScores, studentId, subjectId, semester)
            }
    }

    /**
     * Computes final grade for a specific student and subject
     */
    fun getFinalGrade(
        studentId: Int,
        subjectId: Int
    ): Flow<FinalGrade> {
        return scoreDao.getScoresByStudentAndSubject(studentId, subjectId)
            .map { allScores ->
                computeFinalGrade(allScores, studentId, subjectId)
            }
    }

    private fun computeQuarterlyGrade(
        scores: List<ScoreEntity>,
        studentId: Int,
        subjectId: Int,
        semester: Int,
        quarter: Int
    ): QuarterlyGrade {
        val wwScores = scores.filter { it.scoreType == "WW" }
        val ptScores = scores.filter { it.scoreType == "PT" }
        val examScores = scores.filter { it.scoreType == "EXAM" }

        val wwComponent = computeComponentScore("WW", wwScores, WRITTEN_WORK_WEIGHT)
        val ptComponent = computeComponentScore("PT", ptScores, PERFORMANCE_TASK_WEIGHT)
        val examComponent = computeComponentScore("EXAM", examScores, EXAM_WEIGHT)

        val initialGrade = wwComponent.weightedScore + ptComponent.weightedScore + examComponent.weightedScore
        val transmutedGrade = TransmutationTable.getTransmutedGrade(initialGrade)

        val isComplete = wwScores.isNotEmpty() && ptScores.isNotEmpty() && examScores.isNotEmpty()

        return QuarterlyGrade(
            studentId = studentId,
            subjectId = subjectId,
            semester = semester,
            quarter = quarter,
            writtenWork = wwComponent,
            performanceTask = ptComponent,
            exam = examComponent,
            initialGrade = initialGrade,
            transmutedGrade = transmutedGrade,
            isComplete = isComplete
        )
    }

    private fun computeComponentScore(
        scoreType: String,
        scores: List<ScoreEntity>,
        weight: Double
    ): ComponentScore {
        if (scores.isEmpty()) {
            return ComponentScore(scoreType, 0, 0, 0.0, 0.0)
        }

        val totalRawScore = scores.sumOf { it.rawScore }
        val totalMaxScore = scores.sumOf { it.maxScore }

        val percentage = if (totalMaxScore > 0) {
            (totalRawScore.toDouble() / totalMaxScore.toDouble()) * 100.0
        } else {
            0.0
        }

        val weightedScore = percentage * weight

        return ComponentScore(
            scoreType = scoreType,
            totalRawScore = totalRawScore,
            totalMaxScore = totalMaxScore,
            percentage = percentage,
            weightedScore = weightedScore
        )
    }

    private fun computeSemesterGrade(
        scores: List<ScoreEntity>,
        studentId: Int,
        subjectId: Int,
        semester: Int
    ): SemesterGrade {
        val q1Scores = scores.filter { it.quarter == 1 }
        val q2Scores = scores.filter { it.quarter == 2 }

        val q1Grade = if (q1Scores.isNotEmpty()) {
            val quarterly = computeQuarterlyGrade(q1Scores, studentId, subjectId, semester, 1)
            if (quarterly.isComplete) quarterly.transmutedGrade else null
        } else null

        val q2Grade = if (q2Scores.isNotEmpty()) {
            val quarterly = computeQuarterlyGrade(q2Scores, studentId, subjectId, semester, 2)
            if (quarterly.isComplete) quarterly.transmutedGrade else null
        } else null

        val semesterGrade = if (q1Grade != null && q2Grade != null) {
            (q1Grade + q2Grade) / 2.0
        } else null

        val isComplete = q1Grade != null && q2Grade != null

        return SemesterGrade(
            studentId = studentId,
            subjectId = subjectId,
            semester = semester,
            quarter1Grade = q1Grade,
            quarter2Grade = q2Grade,
            semesterGrade = semesterGrade,
            isComplete = isComplete
        )
    }

    private fun computeFinalGrade(
        scores: List<ScoreEntity>,
        studentId: Int,
        subjectId: Int
    ): FinalGrade {
        val sem1Scores = scores.filter { it.semester == 1 }
        val sem2Scores = scores.filter { it.semester == 2 }

        val sem1Grade = if (sem1Scores.isNotEmpty()) {
            val semesterGrade = computeSemesterGrade(sem1Scores, studentId, subjectId, 1)
            if (semesterGrade.isComplete) semesterGrade.semesterGrade else null
        } else null

        val sem2Grade = if (sem2Scores.isNotEmpty()) {
            val semesterGrade = computeSemesterGrade(sem2Scores, studentId, subjectId, 2)
            if (semesterGrade.isComplete) semesterGrade.semesterGrade else null
        } else null

        val finalGrade = if (sem1Grade != null && sem2Grade != null) {
            (sem1Grade + sem2Grade) / 2.0
        } else null

        val isComplete = sem1Grade != null && sem2Grade != null
        val isPassing = finalGrade?.let { it >= 75.0 } ?: false

        return FinalGrade(
            studentId = studentId,
            subjectId = subjectId,
            semester1Grade = sem1Grade,
            semester2Grade = sem2Grade,
            finalGrade = finalGrade,
            isComplete = isComplete,
            isPassing = isPassing
        )
    }
}