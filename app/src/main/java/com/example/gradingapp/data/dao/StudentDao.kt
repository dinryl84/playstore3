package com.example.gradingapp.data.dao

import androidx.room.*
import com.example.gradingapp.data.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Query("SELECT * FROM students")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :studentId")
    suspend fun getStudentById(studentId: Int): StudentEntity?

    @Query("SELECT * FROM students WHERE sectionId = :sectionId ORDER BY name ASC")
    fun getStudentsBySection(sectionId: Int): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE lrn = :lrn")
    suspend fun getStudentByLrn(lrn: String): StudentEntity?

    @Query("SELECT COUNT(*) FROM students WHERE sectionId = :sectionId")
    suspend fun getStudentCountBySection(sectionId: Int): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStudent(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStudents(students: List<StudentEntity>): List<Long>

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudentById(studentId: Int)

    @Query("DELETE FROM students WHERE sectionId = :sectionId")
    suspend fun deleteStudentsBySection(sectionId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM students WHERE lrn = :lrn)")
    suspend fun isLrnExists(lrn: String): Boolean

    @Query("""
        SELECT s.* FROM students s
        INNER JOIN subject_section_cross_ref ssc ON s.sectionId = ssc.sectionId
        WHERE ssc.subjectId = :subjectId
        ORDER BY s.name ASC
    """)
    fun getStudentsBySubject(subjectId: Int): Flow<List<StudentEntity>>
}