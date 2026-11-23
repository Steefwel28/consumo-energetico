package com.steefwel28.consumoenergetico.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.steefwel28.consumoenergetico.MainActivity
import com.steefwel28.consumoenergetico.R
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.data.local.db.ConsumoDatabase
import java.util.concurrent.TimeUnit

/**
 * Revisión semanal de mantenimientos vencidos o próximos.
 *
 * Dispara una sola notificación con todos los espacios agrupados; si el
 * usuario no registra la limpieza, vuelve a avisar la semana siguiente.
 */
@CraftedBy("steefwel28")
class MaintenanceReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val units = ConsumoDatabase.get(applicationContext).acUnitDao().getAll()
        val text = MaintenanceReminderPlanner.reminderText(
            MaintenanceReminderPlanner.dueUnits(units, System.currentTimeMillis()),
        ) ?: return Result.success()

        if (notificationsAllowed()) notify(text)
        return Result.success()
    }

    private fun notificationsAllowed(): Boolean {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        return hasPermission && NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
    }

    private fun notify(text: String) {
        val manager = NotificationManagerCompat.from(applicationContext)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.maintenance_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = applicationContext.getString(R.string.maintenance_channel_description)
            },
        )

        val openApp = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            openApp,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_maintenance)
            .setContentTitle(applicationContext.getString(R.string.maintenance_notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "maintenance_reminders"
        private const val NOTIFICATION_ID = 7_301
    }
}

/** Programa la revisión cada 7 días; el sistema la sobrevive al reinicio. */
@CraftedBy("steefwel28")
object MaintenanceReminderScheduler {

    private const val WORK_NAME = "maintenance_reminder_weekly"

    fun enqueue(context: Context) {
        val request = PeriodicWorkRequestBuilder<MaintenanceReminderWorker>(7, TimeUnit.DAYS).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
