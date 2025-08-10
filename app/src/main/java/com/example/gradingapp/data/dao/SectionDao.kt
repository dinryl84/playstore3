package com.example.gradingapp.data.dao

import androidx.room.*
import com.example.gradingapp.data.entity.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {

    @Query("SELECT * FROM sections ORDER BY level ASC, name ASC")
    fun getAllSections(): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE id = :sectionId")
    suspend fun getSectionById(sectionId: Int): SectionEntity?

    @Query("SELECT * FROM sections WHERE name = :name")
    suspend fun getSectionByName(name: String): SectionEntity?

    @Query("SELECT * FROM sections WHERE level = :level ORDER BY name ASC")
    fun getSectionsByLevel(level: String): Flow<List<SectionEntity>>

    @Query("SELECT COUNT(*) FROM sections")
    suspend fun getSectionCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSection(section: SectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSections(sections: List<SectionEntity>): List<Long>

    @Update
    suspend fun updateSection(section: SectionEntity)

    @Delete
    suspend fun deleteSection(section: SectionEntity)

    @Query("DELETE FROM sections WHERE id = :sectionId")
    suspend fun deleteSectionById(sectionId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM sections WHERE name = :name)")
    suspend fun isSectionNameExists(name: String): Boolean

    @Query("""
        SELECT s.* FROM sections s
        INNER JOIN subject_section_cross_ref ssc ON s.id = ssc.sectionId
        WHERE ssc.subjectId = :subjectId
        ORDER BY s.level ASC, s.name ASC
    """)
    fun getSectionsBySubject(subjectId: Int): Flow<List<SectionEntity>>
}