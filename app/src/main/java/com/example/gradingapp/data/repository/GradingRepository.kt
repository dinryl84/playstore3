package com.example.gradingapp.data.repository

import com.example.gradingapp.data.database.GradingDatabase
import com.example.gradingapp.data.entity.*
import com.example.gradingapp.utils.TransmutationTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradingRepository @Inject constructor(
    private val database: GradingDatabase
) {
    private val studentDao = database.studentDao()
    private val sectionDao = database.sectionDao()
    private val subjectDao = database.subjectDao()
    private val scoreDao = database.scoreDao()
    private val subjectSectionDao = database.subjectSectionCrossRefDao()

    // Student operations
    fun getAllStudents(): Flow<List<StudentEntity>> = studentDao.getAllStudents()

    suspend fun getStudentById(studentId: Int): StudentEntity? = studentDao.getStudentById(studentId)

    fun getStudentsBySection(sectionId: Int): Flow<List<StudentEntity>> =
        studentDao.getStudentsBySection(sectionId)

    suspend fun insertStudent(student: StudentEntity): Result<Long> {
        return try {
            if (studentDao.isLrnExists(student.lrn)) {
                Result.failure(Exception("LRN already exists"))
            } else {
                val id = studentDao.insertStudent(student)
                Result.success(id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStudent(student: StudentEntity): Result<Unit> {
        return try {
            val existingStudent = studentDao.getStudentByLrn(student.lrn)
            if (existingStudent != null && existingStudent.id != student.id) {
                Result.failure(Exception("LRN already exists for another student"))
            } else {
                studentDao.updateStudent(student)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStudent(student: StudentEntity): Result<Unit> {
        return try {
            studentDao.deleteStudent(student)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Section operations
    fun getAllSections(): Flow<List<SectionEntity>> = sectionDao.getAllSections()

    suspend fun getSectionById(sectionId: Int): SectionEntity? = sectionDao.getSectionById(sectionId)

    suspend fun insertSection(section: SectionEntity): Result<Long> {
        return try {
            if (sectionDao.isSectionNameExists(section.name)) {
                Result.failure(Exception("Section name already exists"))
            } else {
                val id = sectionDao.insertSection(section)
                Result.success(id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSection(section: SectionEntity): Result<Unit> {
        return try {
            val existingSection = sectionDao.getSectionByName(section.name)
            if (existingSection != null && existingSection.id != section.id) {
                Result.failure(Exception("Section name already exists"))
            } else {
                sectionDao.updateSection(section)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSection(section: SectionEntity): Result<Unit> {
        return try {
            sectionDao.deleteSection(section)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Subject operations
    fun getAllSubjects(): Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()

    suspend fun getSubjectById(subjectId: Int): SubjectEntity? = subjectDao.getSubjectById(subjectId)

    suspend fun insertSubject(subject: SubjectEntity): Result<Long> {
        return try {
            if (subjectDao.isSubjectNameExists(subject.name)) {
                Result.failure(Exception("Subject name already exists"))
            } else {
                val id = subjectDao.insertSubject(subject)
                Result.success(id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSubject(subject: SubjectEntity): Result<Unit> {
        return try {
            val existingSubject = subjectDao.getSubjectByName(subject.name)
            if (existingSubject != null && existingSubject.id != subject.id) {
                Result.failure(Exception("Subject name already exists"))
            } else {
                subjectDao.updateSubject(subject)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(subject: SubjectEntity): Result<Unit> {
        return try {
            subjectDao.deleteSubject(subject)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Subject-Section assignment operations
    fun getSubjectsBySection(sectionId: Int): Flow<List<SubjectEntity>> =
        subjectDao.getSubjectsBySection(sectionId)

    fun getSectionsBySubject(subjectId: Int): Flow<List<SectionEntity>> =
        sectionDao.getSectionsBySubject(subjectId)

    suspend fun assignSubjectToSection(subjectId: Int, sectionId: Int): Result<Unit> {
        return try {
            subjectSectionDao.assignSubjectToSection(subjectId, sectionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unassignSubjectFromSection(subjectId: Int, sectionId: Int): Result<Unit> {
        return try {
            subjectSectionDao.unassignSubjectFromSection(subjectId, sectionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isSubjectAssignedToSection(subjectId: Int, sectionId: Int): Boolean =
        subjectSectionDao.isSubjectAssignedToSection(subjectId, sectionId)

    // Score operations
    fun getScoresByStudentAndSubject(studentId: Int, subjectId: Int): Flow<List<ScoreEntity>> =
        scoreDao.getScoresByStudentAndSubject(studentId, subjectId)

    fun getScoresBySemesterQuarter(
        studentId: Int,
        subjectId: Int,
        semester: Int,
        quarter: Int
    ): Flow<List<ScoreEntity>> =
        scoreDao.getScoresByStudentSubjectSemesterQuarter(studentId, subjectId, semester, quarter)

    suspend fun insertScore(score: ScoreEntity): Result<Long> {
        return try {
            val id = scoreDao.insertScore(score)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateScore(score: ScoreEntity): Result<Unit> {
        return try {
            scoreDao.updateScore(score)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteScore(score: ScoreEntity): Result<Unit> {
        return try {
            scoreDao.deleteScore(score)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}