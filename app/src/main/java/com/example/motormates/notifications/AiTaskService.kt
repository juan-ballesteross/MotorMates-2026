package com.example.motormates.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.motormates.MainActivity
import com.example.motormates.R
import com.example.motormates.navigation.PendingNavigation

/**
 * Mantiene viva la app mientras corre una tarea de AI (imagen o búsqueda).
 *
 * Sin esto, Android congela el proceso pocos segundos después de que el
 * usuario sale de la app (el "freezer" de apps en segundo plano) y la
 * generación se queda detenida hasta que vuelva. Un servicio en primer
 * plano de tipo shortService (hasta ~3 minutos, sin permisos especiales)
 * es justo para trabajos cortos así; su notificación es la de "trabajando".
 */
class AiTaskService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Trabajando con AI…"
        val text = intent?.getStringExtra(EXTRA_TEXT).orEmpty()
        val route = intent?.getStringExtra(EXTRA_ROUTE).orEmpty()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(this, title, text, route),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            } else {
                0
            }
        )
        return START_NOT_STICKY
    }

    /** Pasados ~3 minutos Android pide cerrar el servicio corto; la tarea sigue si puede. */
    override fun onTimeout(startId: Int) {
        stopSelf()
    }

    companion object {
        const val NOTIFICATION_ID = 2000
        private const val CHANNEL_ID = "ai_progress"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_TEXT = "text"
        private const val EXTRA_ROUTE = "route"

        /** Debe llamarse con la app en primer plano (al tocar "Generar" o "Buscar"). */
        fun start(context: Context, title: String, text: String, route: String) {
            val intent = Intent(context, AiTaskService::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_TEXT, text)
                .putExtra(EXTRA_ROUTE, route)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AiTaskService::class.java))
        }

        /** Cambia el texto de la notificación sin reiniciar el servicio. */
        fun update(context: Context, title: String, text: String, route: String) {
            context.getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(context, title, text, route))
        }

        private fun buildNotification(context: Context, title: String, text: String, route: String) =
            NotificationCompat.Builder(context, ensureChannel(context))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setProgress(0, 0, true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        NOTIFICATION_ID,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            if (route.isNotBlank()) putExtra(PendingNavigation.EXTRA_ROUTE, route)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .build()

        /** Canal de baja importancia: sin sonido ni ventana emergente. */
        private fun ensureChannel(context: Context): String {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                    manager.createNotificationChannel(
                        NotificationChannel(CHANNEL_ID, "AI trabajando", NotificationManager.IMPORTANCE_LOW).apply {
                            description = "Se muestra mientras la AI genera una imagen o busca una pieza"
                        }
                    )
                }
            }
            return CHANNEL_ID
        }
    }
}
