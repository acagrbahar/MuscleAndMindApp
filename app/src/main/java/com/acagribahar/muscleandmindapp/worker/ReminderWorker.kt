package com.acagribahar.muscleandmindapp.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.acagribahar.muscleandmindapp.MainActivity // Uygulamayı açacak Intent için
import com.acagribahar.muscleandmindapp.R // İkon için R importu

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "MindMuscleReminderChannel"
        const val NOTIFICATION_ID = 1
    }

    override suspend fun doWork(): Result {
        // Burada normalde kullanıcının o günkü görevlerini yapıp yapmadığı kontrol edilebilir.
        // Şimdilik basit bir hatırlatma bildirimi gönderelim.
        println("ReminderWorker çalışıyor...") // Loglama
        showNotification("Mind & Muscle", "Bugünkü görevlerini tamamlamayı unutma!")
        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        // Bildirim kanalını oluştur (sadece API 26+ için)
        createNotificationChannel()

        // Bildirime tıklandığında MainActivity'yi açacak Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )


        // Bildirimi oluştur
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Varsayılan ikon, değiştirilebilir
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // Tıklama aksiyonu
            .setAutoCancel(true) // Tıklanınca bildirimi kapat

        // Bildirimi göster (İzin kontrolü önemli!)
        with(NotificationManagerCompat.from(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ için izin kontrolü
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    // İzin yoksa bildirim gösterilemez.
                    // Burada kullanıcıya izin istemesi için bir mekanizma kurulabilir
                    // veya sadece loglama yapılıp geçilebilir.
                    println("Bildirim izni verilmemiş!")
                    return // Bildirimi gösterme
                }
            }
            // İzin varsa veya eski sürümse bildirimi göster
            notify(NOTIFICATION_ID, builder.build())
            println("Bildirim gönderildi.") // Loglama
        }
    }

    private fun createNotificationChannel() {
        // Sadece API 26+ (Oreo) ve üzeri için kanal oluştur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Görev Hatırlatıcı"
            val descriptionText = "Günlük görev hatırlatmaları"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Kanalı sisteme kaydet
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            println("Bildirim kanalı oluşturuldu.") // Loglama
        }
    }
}