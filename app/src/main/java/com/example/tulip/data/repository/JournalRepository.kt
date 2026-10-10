package com.example.tulip.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.tulip.data.db.TulipDatabase
import com.example.tulip.data.model.JournalEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class JournalRepository(private val context: Context) {
    private val db = TulipDatabase.getInstance(context)
    private val dao = db.journalDao()

    val entriesFlow: Flow<List<JournalEntry>> = dao.getAllEntriesFlow()

    fun searchEntries(query: String): Flow<List<JournalEntry>> = dao.searchEntries(query)

    suspend fun getEntryById(id: String): JournalEntry? = dao.getEntryById(id)

    suspend fun getLatestDraft(): JournalEntry? = dao.getLatestDraft()

    suspend fun saveEntry(entry: JournalEntry) = dao.insertEntry(entry)

    suspend fun updateEntry(entry: JournalEntry) = dao.updateEntry(entry)

    suspend fun deleteEntry(entry: JournalEntry) = dao.deleteEntry(entry)

    suspend fun deleteEntryById(id: String) = dao.deleteEntryById(id)

    suspend fun clearAll() = dao.clearAll()

    /**
     * Copies any Uri (gallery, content, camera output) to private internal storage
     * so that it persists safely and reliably even if gallery origins change or are deleted.
     */
    suspend fun persistImageLocally(sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val imagesDir = File(context.filesDir, "tulip_images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val filename = "tulip_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        val destinationFile = File(imagesDir, filename)

        val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
        if (inputStream != null) {
            inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        destinationFile.absolutePath
    }

    /**
     * Converts a local file path or URI string to a scaled Base64 string for Gemini API.
     * Retains sufficient detail (max dimension 1280px) while maintaining quick upload speed.
     */
    suspend fun loadAndPrepareImageBase64(imagePathOrUri: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val file = File(imagePathOrUri)
            val inputStream: InputStream? = if (file.exists()) {
                file.inputStream()
            } else {
                context.contentResolver.openInputStream(Uri.parse(imagePathOrUri))
            } ?: return@withContext null

            val originalBitmap = inputStream.use { BitmapFactory.decodeStream(it) } ?: return@withContext null

            // Scale if needed to max 1280px
            val maxDim = 1280
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scaledBitmap = if (width > maxDim || height > maxDim) {
                val ratio = width.toFloat() / height.toFloat()
                val targetW: Int
                val targetH: Int
                if (ratio > 1f) {
                    targetW = maxDim
                    targetH = (maxDim / ratio).toInt()
                } else {
                    targetH = maxDim
                    targetW = (maxDim * ratio).toInt()
                }
                Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
            } else {
                originalBitmap
            }

            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
            Pair("image/jpeg", base64)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Export all entries as JSON text for user backup/portability.
     */
    suspend fun exportEntriesJson(): String = withContext(Dispatchers.IO) {
        val list = mutableListOf<JournalEntry>()
        // Simple direct query for export
        val cursor = db.openHelper.readableDatabase.query("SELECT * FROM journal_entries WHERE isDraft = 0 ORDER BY createdAt ASC")
        val jsonArray = JSONArray()
        while (cursor.moveToNext()) {
            val obj = org.json.JSONObject()
            obj.put("id", cursor.getString(cursor.getColumnIndexOrThrow("id")))
            obj.put("originalText", cursor.getString(cursor.getColumnIndexOrThrow("originalText")))
            obj.put("translatedEnglishText", cursor.getString(cursor.getColumnIndexOrThrow("translatedEnglishText")))
            obj.put("locationLabel", cursor.getString(cursor.getColumnIndexOrThrow("locationLabel")))
            obj.put("createdAt", cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")))
            obj.put("tags", cursor.getString(cursor.getColumnIndexOrThrow("tagsJson")))
            obj.put("aiNotes", cursor.getString(cursor.getColumnIndexOrThrow("aiNotesJson")))
            jsonArray.put(obj)
        }
        cursor.close()
        jsonArray.toString(2)
    }
}
