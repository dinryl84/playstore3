package com.example.gradingapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "subjects",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String // e.g., "Mathematics", "Science", "English", "Filipino"
)