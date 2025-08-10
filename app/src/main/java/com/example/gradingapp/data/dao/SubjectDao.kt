package com.example.gradingapp.data.dao

import androidx.room.*
import com.example.gradingapp.data.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    suspend fun getSubjectById(subjectId: Int): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE name = :name")
    suspend fun getSubjectByName(name: String): SubjectEntity?

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun getSubjectCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubjects(subjects: List<SubjectEntity>): List<Long>

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :subjectId")
    suspend fun deleteSubjectById(subjectId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM subjects WHERE name = :name)")
    suspend fun isSubjectNameExists(name: String): Boolean

    @Query("""
        SELECT sub.* FROM subjects sub
        INNER JOIN subject_section_cross_ref ssc ON sub.id = ssc.subjectId
        WHERE ssc.sectionId = :sectionId
        ORDER BY sub.name ASC
    """)
    fun getSubjectsBySection(sectionId: Int): Flow<List<SubjectEntity>>
}