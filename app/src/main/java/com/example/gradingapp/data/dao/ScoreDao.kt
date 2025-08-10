package com.example.gradingapp.data.dao

import androidx.room.*
import com.example.gradingapp.data.entity.ScoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {

    @Query("SELECT * FROM scores")
    fun getAllScores(): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores WHERE id = :scoreId")
    suspend fun getScoreById(scoreId: Int): ScoreEntity?

    @Query("""
        SELECT * FROM scores 
        WHERE studentId = :studentId AND subjectId = :subjectId 
        ORDER BY semester ASC, quarter ASC, scoreType ASC, number ASC
    """)
    fun getScoresByStudentAndSubject(studentId: Int, subjectId: Int): Flow<List<ScoreEntity>>

    @Query("""
        SELECT * FROM scores 
        WHERE studentId = :studentId AND subjectId = :subjectId AND semester = :semester AND quarter = :quarter
        ORDER BY scoreType ASC, number ASC
    """)
    fun getScoresByStudentSubjectSemesterQuarter(
        studentId: Int,
        subjectId: Int,
        semester: Int,
        quarter: Int
    ): Flow<List<ScoreEntity>>

    @Query("""
        SELECT * FROM scores 
        WHERE sectionId = :sectionId AND subjectId = :subjectId AND semester = :semester AND quarter = :quarter
        ORDER BY studentId ASC, scoreType ASC, number ASC
    """)
    fun getScoresBySectionSubjectSemesterQuarter(
        sectionId: Int,
        subjectId: Int,
        semester: Int,
        quarter: Int
    ): Flow<List<ScoreEntity>>

    @Query("""
        SELECT * FROM scores 
        WHERE studentId = :studentId AND subjectId = :subjectId AND semester = :semester 
              AND quarter = :quarter AND scoreType = :scoreType
        ORDER BY number ASC
    """)
    fun getScoresByStudentSubjectSemesterQuarterType(
        studentId: Int,
        subjectId: Int,
        semester: Int,
        quarter: Int,
        scoreType: String
    ): Flow<List<ScoreEntity>>

    @Query("""
        SELECT COUNT(*) FROM scores 
        WHERE studentId = :studentId AND subjectId = :subjectId AND semester = :semester 
              AND quarter = :quarter AND scoreType = :scoreType
    """)
    suspend fun getScoreCountByType(
        studentId: Int,
        subjectId: Int,
        semester: Int,
        quarter: Int,
        scoreType: String
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: ScoreEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScores(scores: List<ScoreEntity>): List<Long>

    @Update
    suspend fun updateScore(score: ScoreEntity)

    @Delete
    suspend fun deleteScore(score: ScoreEntity)

    @Query("DELETE FROM scores WHERE id = :scoreId")
    suspend fun deleteScoreById(scoreId: Int)

    @Query("""
        DELETE FROM scores 
        WHERE studentId = :studentId AND subjectId = :subjectId AND semester = :semester 
              AND quarter = :quarter AND scoreType = :scoreType AND number = :number
    """)
    suspend fun deleteSpecificScore(
        studentId: Int,
        subjectId: Int,
        semester: Int,
        quarter: Int,
        scoreType: String,
        number: Int
    )

    @Query("""
        DELETE FROM scores 
        WHERE studentId = :studentId AND subjectId = :subjectId AND semester = :semester AND quarter = :quarter
    """)
    suspend fun deleteScoresBySemesterQuarter(
        studentId: Int,
        subjectId: Int,
        semester: Int,
        quarter: Int
    )

    @Query("DELETE FROM scores WHERE studentId = :studentId")
    suspend fun deleteScoresByStudent(studentId: Int)

    @Query("DELETE FROM scores WHERE subjectId = :subjectId")
    suspend fun deleteScoresBySubject(subjectId: Int)

    @Query("DELETE FROM scores WHERE sectionId = :sectionId")
    suspend fun deleteScoresBySection(sectionId: Int)

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM scores 
            WHERE studentId = :studentId AND subjectId = :subjectId AND semester = :semester 
                  AND quarter = :quarter AND scoreType = :scoreType AND number = :number
        )
    """)
    suspend fun isScoreExists(
        studentId: Int,
        subjectId: Int,
        semester: Int,
        quarter: Int,
        scoreType: String,
        number: Int
    ): Boolean
}