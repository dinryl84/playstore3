package com.example.gradingapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "subject_section_cross_ref",
    primaryKeys = ["subjectId", "sectionId"],
    foreignKeys = [
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
        Index(value = ["subjectId"]),
        Index(value = ["sectionId"])
    ]
)
data class SubjectSectionCrossRef(
    val subjectId: Int, // Foreign key to SubjectEntity
    val sectionId: Int  // Foreign key to SectionEntity
)