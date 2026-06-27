package com.lexaprograms.polishcards

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class CardNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        val repository = CardRepository(applicationContext)
        if (activePracticeNotificationCount() >= repository.loadMaxActiveNotifications()) return finish(repository)
        val (lesson, card) = repository.findRandomNotificationCard() ?: return finish(repository)
        ensureChannel()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EXTRA_LESSON_ID, lesson.id)
            putExtra(EXTRA_CARD_ID, card.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            card.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val starText = card.starDisplay()
        val practiceText = card.mistakeText().ifBlank { card.nativeText().ifBlank { card.correctText() } }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("MurrLex - $starText")
            .setContentText(practiceText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$practiceText\n\nLesson: ${lesson.title}\nStars: $starText"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_TAG, nextNotificationId(), notification)
        return finish(repository)
    }

    private fun finish(repository: CardRepository): Result {
        schedule(applicationContext, repository.loadNotificationIntervalMinutes())
        return Result.success()
    }

    private fun activePracticeNotificationCount(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return 0
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.activeNotifications.count { it.tag == NOTIFICATION_TAG }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "MurrLex practice",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Weighted flashcard reminders from your lessons"
        }
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun Flashcard.starDisplay(): String {
        val count = starCount()
        return "$count/3 stars"
    }

    private fun nextNotificationId(): Int {
        return NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 100_000).toInt()
    }

    companion object {
        const val EXTRA_LESSON_ID = "extra_lesson_id"
        const val EXTRA_CARD_ID = "extra_card_id"
        private const val CHANNEL_ID = "polish_cards_practice"
        private const val WORK_NAME = "polish_cards_notifications"
        private const val NOTIFICATION_TAG = "murrlex_practice"
        private const val NOTIFICATION_ID_BASE = 9009

        fun schedule(context: Context, intervalMinutes: Int) {
            val request = OneTimeWorkRequestBuilder<CardNotificationWorker>()
                .setInitialDelay(intervalMinutes.coerceAtLeast(1).toLong(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}



