package com.example.motormates.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.motormates.MainActivity
import com.example.motormates.R
import com.example.motormates.navigation.PendingNavigation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notificaciones locales de las tareas de AI (imagen, búsqueda en Google).
 * Interfaz para que AiTaskManager se pruebe en la JVM sin Android.
 */
interface AiNotifier {
    /**
     * "Trabajando…": arranca (o actualiza) el servicio en primer plano que
     * evita que Android congele la app mientras la AI trabaja.
     */
    fun keepAlive(title: String, text: String, route: String)

    /** Ya no queda nada corriendo: se quita el servicio y su notificación. */
    fun stopKeepAlive()

    /** Resultado listo (o error). image: la imagen generada, para mostrarla grande. */
    fun showDone(id: Int, title: String, text: String, route: String, image: ByteArray? = null)

    fun cancel(id: Int)
}

@Singleton
class AndroidAiNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : AiNotifier {

    private val manager = NotificationManagerCompat.from(context)
    private val serviceRunning = AtomicBoolean(false)

    override fun keepAlive(title: String, text: String, route: String) {
        if (serviceRunning.compareAndSet(false, true)) {
            // Solo se puede arrancar con la app en primer plano: se llama al
            // tocar "Generar"/"Buscar". Si Android lo niega, la tarea sigue
            // mientras la app esté abierta.
            runCatching { AiTaskService.start(context, title, text, route) }
                .onFailure { serviceRunning.set(false) }
        } else {
            runCatching { AiTaskService.update(context, title, text, route) }
        }
    }

    override fun stopKeepAlive() {
        if (serviceRunning.getAndSet(false)) runCatching { AiTaskService.stop(context) }
    }

    override fun showDone(id: Int, title: String, text: String, route: String, image: ByteArray?) {
        val builder = builder(title, text, route)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        val bitmap = image?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() }
        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
                .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null as android.graphics.Bitmap?))
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(text))
        }
        notify(id, builder)
    }

    override fun cancel(id: Int) = manager.cancel(id)

    private fun builder(title: String, text: String, route: String): NotificationCompat.Builder {
        ensureChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openRoute(route))
    }

    /** Tocar la notificación abre la app justo en el resultado. */
    private fun openRoute(route: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(PendingNavigation.EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(
            context,
            route.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Sin permiso (Android 13+) o con las notificaciones apagadas, no se intenta. */
    private fun notify(id: Int, builder: NotificationCompat.Builder) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed || !manager.areNotificationsEnabled()) return
        runCatching { manager.notify(id, builder.build()) }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val system = context.getSystemService(NotificationManager::class.java)
        if (system.getNotificationChannel(CHANNEL_ID) != null) return
        system.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Resultados de AI", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Avisa cuando termina una modificación con AI o una búsqueda de piezas"
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "ai_results"
    }
}
