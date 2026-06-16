package com.wade.school.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null

    private fun getStudentDir(studentId: String): File {
        val dir = File(context.filesDir, "recordings/$studentId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun startRecording(studentId: String, studentName: String): File? {
        val dateFormat = SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val fileName = "${timestamp}-${studentName}.mp3"
        currentFile = File(getStudentDir(studentId), fileName)

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(currentFile!!.absolutePath)
            
            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }
        return currentFile
    }

    fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }

    fun listRecordings(studentId: String): List<File> {
        return getStudentDir(studentId).listFiles { _, name -> 
            name.endsWith(".mp3") 
        }?.toList() ?: emptyList()
    }

    fun deleteRecording(file: File): Boolean {
        return file.delete()
    }

    fun renameRecording(file: File, newName: String): File {
        val newFile = File(file.parentFile, "$newName.mp3")
        return if (file.renameTo(newFile)) newFile else file
    }
}
