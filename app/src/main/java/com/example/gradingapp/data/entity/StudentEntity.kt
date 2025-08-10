package com.example.gradingapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["lrn"], unique = true),
        Index(value = ["sectionId"])
    ]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val lrn: String, // Learner's Reference Number - must be unique

    val gender: String, // "Male", "Female", or "Other"

    val sectionId: Int // Foreign key to SectionEntity
)