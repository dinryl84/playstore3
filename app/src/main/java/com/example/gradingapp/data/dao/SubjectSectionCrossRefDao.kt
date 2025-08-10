package com.example.gradingapp.data.dao

import androidx.room.*
import com.example.gradingapp.data.entity.SubjectSectionCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectSectionCrossRefDao {

    @Query("SELECT * FROM subject_section_cross_ref")
    fun getAllSubjectSectionCrossRefs(): Flow<List<SubjectSectionCrossRef>>

    @Query("SELECT * FROM subject_section_cross_ref WHERE subjectId = :subjectId")
    fun getCrossRefsBySubject(subjectId: Int): Flow<List<SubjectSectionCrossRef>>

    @Query("SELECT * FROM subject_section_cross_ref WHERE sectionId = :sectionId")
    fun getCrossRefsBySection(sectionId: Int): Flow<List<SubjectSectionCrossRef>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM subject_section_cross_ref 
            WHERE subjectId = :subjectId AND sectionId = :sectionId
        )
    """)
    suspend fun isSubjectAssignedToSection(subjectId: Int, sectionId: Int): Boolean

    @Query("SELECT COUNT(*) FROM subject_section_cross_ref WHERE subjectId = :subjectId")
    suspend fun getSectionCountForSubject(subjectId: Int): Int

    @Query("SELECT COUNT(*) FROM subject_section_cross_ref WHERE sectionId = :sectionId")
    suspend fun getSubjectCountForSection(sectionId: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubjectSectionCrossRef(crossRef: SubjectSectionCrossRef): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubjectSectionCrossRefs(crossRefs: List<SubjectSectionCrossRef>): List<Long>

    @Delete
    suspend fun deleteSubjectSectionCrossRef(crossRef: SubjectSectionCrossRef)

    @Query("""
        DELETE FROM subject_section_cross_ref 
        WHERE subjectId = :subjectId AND sectionId = :sectionId
    """)
    suspend fun deleteSubjectSectionAssignment(subjectId: Int, sectionId: Int)

    @Query("DELETE FROM subject_section_cross_ref WHERE subjectId = :subjectId")
    suspend fun deleteAllAssignmentsForSubject(subjectId: Int)

    @Query("DELETE FROM subject_section_cross_ref WHERE sectionId = :sectionId")
    suspend fun deleteAllAssignmentsForSection(sectionId: Int)

    @Transaction
    suspend fun assignSubjectToSection(subjectId: Int, sectionId: Int) {
        val crossRef = SubjectSectionCrossRef(subjectId = subjectId, sectionId = sectionId)
        insertSubjectSectionCrossRef(crossRef)
    }

    @Transaction
    suspend fun unassignSubjectFromSection(subjectId: Int, sectionId: Int) {
        deleteSubjectSectionAssignment(subjectId, sectionId)
    }

    @Transaction
    suspend fun assignSubjectToMultipleSections(subjectId: Int, sectionIds: List<Int>) {
        val crossRefs = sectionIds.map { sectionId ->
            SubjectSectionCrossRef(subjectId = subjectId, sectionId = sectionId)
        }
        insertSubjectSectionCrossRefs(crossRefs)
    }
}