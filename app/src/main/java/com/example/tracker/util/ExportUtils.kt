package com.example.tracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.tracker.data.local.AppDatabase
import com.example.tracker.data.local.entities.StudySessionEntity
import com.example.tracker.utils.FirebaseAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {

    suspend fun exportToCsv(context: Context) {
        val userId = FirebaseAuthManager.getCurrentUserId() ?: return
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val sessions = db.studySessionDao().getAllSessionsSync(userId)
            
            val csvHeader = "ID,TaskID,StartTime,EndTime,DurationSeconds,Date\n"
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            val csvData = StringBuilder()
            csvData.append(csvHeader)
            
            sessions.forEach { session ->
                val date = dateFormat.format(Date(session.startTime))
                csvData.append("${session.id},${session.taskId ?: "None"},${session.startTime},${session.endTime},${session.durationSeconds},$date\n")
            }
            
            val file = File(context.cacheDir, "study_history_export.csv")
            FileOutputStream(file).use { 
                it.write(csvData.toString().toByteArray())
            }
            
            shareFile(context, file, "text/csv")
        }
    }

    suspend fun exportToJson(context: Context) {
        val userId = FirebaseAuthManager.getCurrentUserId() ?: return
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val sessions = db.studySessionDao().getAllSessionsSync(userId)
            
            val jsonArray = JSONArray()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            sessions.forEach { session ->
                val jsonObject = JSONObject().apply {
                    put("id", session.id)
                    put("taskId", session.taskId)
                    put("startTime", session.startTime)
                    put("endTime", session.endTime)
                    put("durationSeconds", session.durationSeconds)
                    put("readableDate", dateFormat.format(Date(session.startTime)))
                }
                jsonArray.put(jsonObject)
            }
            
            val file = File(context.cacheDir, "study_history_export.json")
            FileOutputStream(file).use { 
                it.write(jsonArray.toString(4).toByteArray())
            }
            
            shareFile(context, file, "application/json")
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(intent, "Export Study History")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
