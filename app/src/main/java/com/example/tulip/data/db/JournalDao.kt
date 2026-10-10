package com.example.tulip.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tulip.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries WHERE isDraft = 0 ORDER BY createdAt DESC")
    fun getAllEntriesFlow(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryById(id: String): JournalEntry?

    @Query("SELECT * FROM journal_entries WHERE isDraft = 1 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestDraft(): JournalEntry?

    @Query("""
        SELECT * FROM journal_entries 
        WHERE isDraft = 0 
          AND (originalText LIKE '%' || :query || '%' 
               OR (translatedEnglishText IS NOT NULL AND translatedEnglishText LIKE '%' || :query || '%')
               OR (locationLabel IS NOT NULL AND locationLabel LIKE '%' || :query || '%')
               OR tagsJson LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchEntries(query: String): Flow<List<JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry)

    @Update
    suspend fun updateEntry(entry: JournalEntry)

    @Delete
    suspend fun deleteEntry(entry: JournalEntry)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteEntryById(id: String)

    @Query("DELETE FROM journal_entries")
    suspend fun clearAll()
}
