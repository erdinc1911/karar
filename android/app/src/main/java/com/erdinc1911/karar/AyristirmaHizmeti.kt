package com.erdinc1911.karar

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat

// Ayrıştırma sırası çalışırken bildirim gösterir ve işlemciyi uyanık tutar; böylece ekran kapalıyken de iş sürer.
class AyristirmaHizmeti : Service() {
    companion object {
        @Volatile var calisiyor = false
        @Volatile private var ornek: AyristirmaHizmeti? = null
        @Volatile private var durIstegi = false
        @Volatile private var bekliyor = false
        const val KANAL = "ayristirma"; const val NO = 7

        fun baslat(c: Context, metin: String) {
            durIstegi = false
            val o = ornek
            if (o != null) { o.bildir(metin); return }          // zaten çalışıyor: yalnız yazıyı güncelle
            bekliyor = true
            ContextCompat.startForegroundService(c, Intent(c, AyristirmaHizmeti::class.java).putExtra("metin", metin))
        }
        // stopService çağrılmaz: başlatma isteği henüz işlenmeden durdurmak Android'de uygulamayı çökertir.
        fun durdur() { val o = ornek; if (o != null) o.bitir() else if (bekliyor) durIstegi = true }
    }
    private var kilit: PowerManager.WakeLock? = null

    override fun onBind(i: Intent?): IBinder? = null

    private fun bildirim(metin: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(KANAL) == null)
            nm.createNotificationChannel(NotificationChannel(KANAL, "Ayrıştırma", NotificationManager.IMPORTANCE_LOW).apply { description = "Ayrıştırma sırası çalışırken görünür" })
        val ac = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, KANAL).setSmallIcon(R.drawable.ic_bildirim).setContentTitle("Karar Eşleştirici").setContentText(metin)
            .setOngoing(true).setContentIntent(ac).setOnlyAlertOnce(true).build()
    }
    fun bildir(metin: String) { try { getSystemService(NotificationManager::class.java).notify(NO, bildirim(metin)) } catch (_: Exception) {} }

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        val metin = i?.getStringExtra("metin")?.takeIf { it.isNotBlank() } ?: "Kanallar ayrılıyor"
        val b = bildirim(metin)
        // her başlatmada önce startForeground: Android bunu birkaç saniye içinde ister
        try {
            if (Build.VERSION.SDK_INT >= 35) startForeground(NO, b, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
            else if (Build.VERSION.SDK_INT >= 29) startForeground(NO, b, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else startForeground(NO, b)
        } catch (e: Exception) { bekliyor = false; stopSelf(); return START_NOT_STICKY }
        ornek = this; bekliyor = false; calisiyor = true
        if (kilit == null) kilit = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "karar:ayristirma").apply { setReferenceCounted(false); acquire(6 * 60 * 60 * 1000L) }
        if (durIstegi) { durIstegi = false; bitir() }
        return START_NOT_STICKY
    }

    fun bitir() {
        kilit?.let { if (it.isHeld) it.release() }; kilit = null
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
        stopSelf()
    }

    override fun onTimeout(startId: Int, fgsType: Int) { bitir() }   // Android 15: günlük süre dolunca

    override fun onDestroy() { kilit?.let { if (it.isHeld) it.release() }; kilit = null; ornek = null; calisiyor = false; super.onDestroy() }
}
