package com.example.tulip.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val originalText: String = "",
    val translatedEnglishText: String? = null,
    val translationApplied: Boolean = false,
    val imageUrisJson: String = "[]", // JSON array of stored local file URIs or paths
    val locationLabel: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val tagsJson: String = "[]", // JSON array of observation tags
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDraft: Boolean = false,
    val aiNotesJson: String = "[]" // JSON array of saved Ask AI QA entries
)

data class AskAiObservation(
    val id: String = UUID.randomUUID().toString(),
    val imageUri: String,
    val question: String,
    val answer: String,
    val timestamp: Long = System.currentTimeMillis()
)
