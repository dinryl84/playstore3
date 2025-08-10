package com.example.gradingapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "scores",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["subjectId"]),
        Index(value = ["sectionId"]),
        Index(value = ["studentId", "subjectId", "semester", "quarter", "scoreType", "number"], unique = true)
    ]
)
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val studentId: Int, // Foreign key to StudentEntity

    val subjectId: Int, // Foreign key to SubjectEntity

    val sectionId: Int, // Foreign key to SectionEntity

    val semester: Int, // 1 or 2

    val quarter: Int, // 1 or 2 (Q1 or Q2)

    val scoreType: String, // "WW" (Written Work), "PT" (Performance Task), "EXAM" (Quarterly Assessment)

    val number: Int, // 1-10 for WW and PT, always 1 for EXAM

    val rawScore: Int, // The actual score the student got

    val maxScore: Int // The highest possible score for this assessment
)