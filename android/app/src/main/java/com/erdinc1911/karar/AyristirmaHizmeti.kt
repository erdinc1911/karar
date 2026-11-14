package com.erdinc1911.karar

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

// Ayrıştırma sırası çalışırken bildirim gösterir ve işlemciyi uyanık tutar; böylece ekran kapalıyken de iş sürer.
class AyristirmaHizmeti : Service() {
    companion object { @Volatile var calisiyor = false; const val KANAL = "ayristirma"; const val NO = 7 }
    private var kilit: PowerManager.WakeLock? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        val metin = i?.getStringExtra("metin")?.takeIf { it.isNotBlank() } ?: "Kanallar ayrılıyor"
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(KANAL) == null)
            nm.createNotificationChannel(NotificationChannel(KANAL, "Ayrıştırma", NotificationManager.IMPORTANCE_LOW).apply { description = "Ayrıştırma sırası çalışırken görünür" })
        val ac = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val b = Notification.Builder(this, KANAL).setSmallIcon(R.drawable.ic_bildirim).setContentTitle("Karar Eşleştirici").setContentText(metin)
            .setOngoing(true).setContentIntent(ac).setOnlyAlertOnce(true).build()
        if (Build.VERSION.SDK_INT >= 35) startForeground(NO, b, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
        else if (Build.VERSION.SDK_INT >= 29) startForeground(NO, b, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(NO, b)
        if (kilit == null) kilit = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "karar:ayristirma").apply { setReferenceCounted(false); acquire(6 * 60 * 60 * 1000L) }
        calisiyor = true
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) { stopSelf() }   // Android 15: günlük süre dolunca

    override fun onDestroy() { kilit?.let { if (it.isHeld) it.release() }; kilit = null; calisiyor = false; super.onDestroy() }
}
