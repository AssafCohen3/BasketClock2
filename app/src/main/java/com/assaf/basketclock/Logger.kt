package com.assaf.basketclock

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(private val context: Context) : Timber.DebugTree() {

    private val todayLogFileNameTemplate = "logs_%s.txt"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault())

    private fun getTodayLogFile(): File {
        return File(context.getExternalFilesDir(null), todayLogFileNameTemplate.format(fileDateFormat.format(Date())))
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        try {
            val logFile = getTodayLogFile()
            val fileWriter = FileWriter(logFile, true)
            fileWriter.appendLine("${dateFormat.format(Date())} [$tag]: $message")
            fileWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
