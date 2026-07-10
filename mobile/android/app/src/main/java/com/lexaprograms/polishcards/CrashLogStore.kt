package com.lexaprograms.polishcards

import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogStore {
    private const val PREFS = "murrlex_crash_logs"
    private const val KEY_LOGS = "last_crash_logs"
    private const val SEPARATOR = "\n\n===== MURRLEX_CRASH_ENTRY =====\n\n"
    private const val MAX_LOGS = 2

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is MurrLexCrashHandler) return
        Thread.setDefaultUncaughtExceptionHandler(MurrLexCrashHandler(appContext, previous))
    }

    fun record(context: Context, throwable: Throwable, threadName: String = Thread.currentThread().name) {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        val entry = buildString {
            append(timestamp())
            append('\n')
            append("App: MurrLex ")
            append(BuildConfig.VERSION_NAME)
            append(" (")
            append(BuildConfig.VERSION_CODE)
            append(")\n")
            append("Thread: ")
            append(threadName)
            append('\n')
            append("Error: ")
            append(throwable.javaClass.name)
            throwable.message?.takeIf { it.isNotBlank() }?.let { message ->
                append(": ")
                append(message)
            }
            append("\n\n")
            append(writer.toString())
        }
        val logs = (listOf(entry) + load(context)).take(MAX_LOGS)
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LOGS, logs.joinToString(SEPARATOR))
            .apply()
    }

    fun load(context: Context): List<String> {
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOGS, "")
            .orEmpty()
            .split(SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(MAX_LOGS)
    }

    fun clear(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LOGS)
            .apply()
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
}

private class MurrLexCrashHandler(
    private val context: Context,
    private val previous: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching { CrashLogStore.record(context, throwable, thread.name) }
        previous?.uncaughtException(thread, throwable)
    }
}
