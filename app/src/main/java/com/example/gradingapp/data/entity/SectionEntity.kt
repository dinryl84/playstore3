package com.example.gradingapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "sections",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class SectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String, // e.g., "Grade 11 - Einstein", "Grade 12 - Newton"

    val level: String // e.g., "Grade 11", "Grade 12"
)