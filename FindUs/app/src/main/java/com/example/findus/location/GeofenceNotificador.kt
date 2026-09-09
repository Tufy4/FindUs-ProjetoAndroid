package com.example.findus.location

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

private const val CANAL = "geofencing"

class GeofenceNotificador(private val context: Context) {
    private val _eventos = MutableSharedFlow<EventoGeofence>(extraBufferCapacity = 5)
    val eventos: SharedFlow<EventoGeofence> = _eventos

    init {
        val canal = NotificationChannel(CANAL, "Geofencing", NotificationManager.IMPORTANCE_DEFAULT)
        NotificationManagerCompat.from(context).createNotificationChannel(canal)
    }

    fun notificar(evento: EventoGeofence) {
        _eventos.tryEmit(evento)

        val semPermissao = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (semPermissao) return

        val acao = if (evento.tipo == TipoEventoGeofence.ENTROU) "entrou no" else "saiu do"
        val notificacao = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("Perímetro monitorado")
            .setContentText("Veículo ${evento.veiculoId} $acao perímetro monitorado")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(evento.veiculoId.toInt(), notificacao)
    }
}
