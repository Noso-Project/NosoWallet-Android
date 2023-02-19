package com.s7evensoftware.nosowallet.popservice

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import com.s7evensoftware.nosowallet.MainActivity
import com.s7evensoftware.nosowallet.R
import com.s7evensoftware.nosowallet.nosocore.mpDisk
import com.s7evensoftware.nosowallet.util.Log

const val NOSO_POP_RECURRENT_ACTION = "com.s7evensoftware.nosowallet.PPB_NOSO_ACTION"
const val NOSO_NOTIFICATION_CHANNEL_ID = "NOSO_POP_CHANNEL_7"
const val NOSO_NOTIFICATION_CHANNEL_NAME = "Noso PoP Service"
const val NOSO_NOTIFICATION_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_HIGH
const val NOSO_POP_NOTIFICATION_ID = 7020090
const val NOSO_POP_RECURRENT_TASK_CODE = 729927
const val NOSO_INTENT_POP_COUNTER = "POP_COUNTER"

class PoPService: Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val claimNosoTask = object: Runnable {
        override fun run() {
            Log.e("PPS","Claiming fake noso []")
            handler.postDelayed(this, 10*60*1000)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        mpDisk.setContext(this)
        createNotificationChannel()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showForegroundNotification()
        runAndSchedule(0L)
        return START_STICKY
    }

    private fun showForegroundNotification(){
        val notificationIntent = Intent(this, MainActivity::class.java)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags)

        val notification = NotificationCompat.Builder(this, NOSO_NOTIFICATION_CHANNEL_ID).apply {
            setContentTitle("Noso PoP Service")
            setContentText("You're earnings noso...")
            setSmallIcon(R.drawable.ic_alertmark_24)
            setContentIntent(pendingIntent)
            setOngoing(true)
            foregroundServiceBehavior = FOREGROUND_SERVICE_IMMEDIATE
        }.build()

        startForeground(NOSO_POP_NOTIFICATION_ID, notification)
    }
    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOSO_NOTIFICATION_CHANNEL_ID,
                NOSO_NOTIFICATION_CHANNEL_NAME,
                NOSO_NOTIFICATION_CHANNEL_IMPORTANCE
            )

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun runAndSchedule(counter:Long){
        Log.e("RTP", "Fake noso claim packet [$counter]")

        val popInterval = 1*60*1000
        val intent = Intent(this, PoPBroadcast::class.java)
        intent.putExtra(NOSO_INTENT_POP_COUNTER, counter.inc())
        intent.action = NOSO_POP_RECURRENT_ACTION

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getBroadcast(this, NOSO_POP_RECURRENT_TASK_CODE, intent, flags)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+popInterval, pendingIntent)
    }
}

class PoPBroadcast: BroadcastReceiver() {

    private fun runAndSchedule(context: Context?, counter:Long){
        Log.e("RTP", "Fake noso claim packet [$counter]")

        val popInterval = 1*60*1000
        val intent = Intent(context, PoPBroadcast::class.java)
        intent.putExtra(NOSO_INTENT_POP_COUNTER, counter.inc())
        intent.action = NOSO_POP_RECURRENT_ACTION

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getBroadcast(context, NOSO_POP_RECURRENT_TASK_CODE, intent, flags)
        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+popInterval, pendingIntent)
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        runAndSchedule(p0,(p1?.getLongExtra(NOSO_INTENT_POP_COUNTER, 0L))?:0L)
    }
}