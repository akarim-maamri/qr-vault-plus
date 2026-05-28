package com.example.qrcodescanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SecureNoteDao {
    @Query("SELECT * FROM secure_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<SecureNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: SecureNoteEntity)

    @Update
    suspend fun updateNote(note: SecureNoteEntity)

    @Delete
    suspend fun deleteNote(note: SecureNoteEntity)

    @Query("DELETE FROM secure_notes")
    suspend fun clearAllNotes()
}
