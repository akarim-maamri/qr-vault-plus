package com.example.qrcodescanner.data

import kotlinx.coroutines.flow.Flow

interface DataRepository {
    val allResults: Flow<List<ScanResultEntity>>
    val favoriteResults: Flow<List<ScanResultEntity>>
    val allPasswords: Flow<List<PasswordEntity>>
    val allNotes: Flow<List<SecureNoteEntity>>
    
    suspend fun insertResult(result: ScanResultEntity): Long
    suspend fun deleteResult(result: ScanResultEntity)
    suspend fun updateResult(result: ScanResultEntity)
    suspend fun clearAllHistory()

    // Passwords CRUD
    suspend fun insertPassword(password: PasswordEntity)
    suspend fun deletePassword(password: PasswordEntity)
    suspend fun updatePassword(password: PasswordEntity)
    suspend fun clearAllPasswords()

    // Notes CRUD
    suspend fun insertNote(note: SecureNoteEntity)
    suspend fun deleteNote(note: SecureNoteEntity)
    suspend fun updateNote(note: SecureNoteEntity)
    suspend fun clearAllNotes()
}

class DefaultDataRepository(
    private val scanDao: ScanResultDao,
    private val passwordDao: PasswordDao,
    private val secureNoteDao: SecureNoteDao
) : DataRepository {
    override val allResults: Flow<List<ScanResultEntity>> = scanDao.getAllScanResults()
    override val favoriteResults: Flow<List<ScanResultEntity>> = scanDao.getFavoriteScanResults()
    override val allPasswords: Flow<List<PasswordEntity>> = passwordDao.getAllPasswords()
    override val allNotes: Flow<List<SecureNoteEntity>> = secureNoteDao.getAllNotes()

    override suspend fun insertResult(result: ScanResultEntity): Long {
        return scanDao.insertScanResult(result)
    }

    override suspend fun deleteResult(result: ScanResultEntity) {
        scanDao.deleteScanResult(result)
    }

    override suspend fun updateResult(result: ScanResultEntity) {
        scanDao.updateScanResult(result)
    }

    override suspend fun clearAllHistory() {
        scanDao.clearHistory()
    }

    // Passwords operations implementation
    override suspend fun insertPassword(password: PasswordEntity) {
        passwordDao.insertPassword(password)
    }

    override suspend fun deletePassword(password: PasswordEntity) {
        passwordDao.deletePassword(password)
    }

    override suspend fun updatePassword(password: PasswordEntity) {
        passwordDao.updatePassword(password)
    }

    override suspend fun clearAllPasswords() {
        passwordDao.clearAllPasswords()
    }

    // Notes operations implementation
    override suspend fun insertNote(note: SecureNoteEntity) {
        secureNoteDao.insertNote(note)
    }

    override suspend fun deleteNote(note: SecureNoteEntity) {
        secureNoteDao.deleteNote(note)
    }

    override suspend fun updateNote(note: SecureNoteEntity) {
        secureNoteDao.updateNote(note)
    }

    override suspend fun clearAllNotes() {
        secureNoteDao.clearAllNotes()
    }
}
