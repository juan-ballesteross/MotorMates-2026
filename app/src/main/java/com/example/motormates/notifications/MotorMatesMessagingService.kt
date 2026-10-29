package com.example.motormates.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.motormates.MainActivity
import com.example.motormates.R
import com.example.motormates.data.repository.PushTokenRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Punto de entrada de FCM en la app. Lo declara el AndroidManifest con el
 * intent-filter com.google.firebase.MESSAGING_EVENT, y el sistema lo levanta
 * aunque la app esté cerrada.
 */
@AndroidEntryPoint
class MotorMatesMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushTokenRepository: PushTokenRepository

    // Los callbacks del servicio no son suspend y guardar el token sí lo es.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * FCM entrega aquí el token nuevo cuando lo rota (reinstalación, borrado
     * de datos, restauración). Si no se reguarda, la Cloud Function seguiría
     * enviando al token viejo y la notificación no llegaría.
     */
    override fun onNewToken(token: String) {
        serviceScope.launch { pushTokenRepository.saveToken(token) }
    }

    /**
     * Llega por acá cuando la app está en primer plano, o cuando el mensaje
     * trae solo "data". Con la app en segundo plano y un mensaje con
     * "notification", Android la pinta solo y este método no se llama.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: getString(R.string.app_name)
        val body = message.notification?.body ?: return
        showNotification(title, body)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showNotification(title: String, body: String) {
        val manager = NotificationManagerCompat.from(this)
        createChannelIfNeeded(manager)

        // Sin FLAG_ACTIVITY_CLEAR_TOP: si la app ya está abierta se vuelve a
        // ella tal como estaba, no se reinicia la navegación.
        val intent = Intent(this, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // notify no lanza si falta POST_NOTIFICATIONS: simplemente no se
        // muestra nada, así que no hace falta comprobar el permiso acá.
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /** createNotificationChannel es idempotente: volver a crearlo no duplica nada. */
    private fun createChannelIfNeeded(manager: NotificationManagerCompat) {
        val channel = NotificationChannel(
            getString(R.string.notification_channel_id),
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = getString(R.string.notification_channel_description) }
        manager.createNotificationChannel(channel)
    }
}
