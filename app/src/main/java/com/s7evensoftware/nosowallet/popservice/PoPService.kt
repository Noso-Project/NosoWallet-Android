package com.s7evensoftware.nosowallet.popservice

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import com.s7evensoftware.nosowallet.MainActivity
import com.s7evensoftware.nosowallet.R
import com.s7evensoftware.nosowallet.nosocore.mpDisk
import com.s7evensoftware.nosowallet.nosocore.mpNetwork
import com.s7evensoftware.nosowallet.ui.theme.nosoColor
import com.s7evensoftware.nosowallet.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ThreadLocalRandom

const val NOSO_POP_RECURRENT_ACTION = "com.s7evensoftware.nosowallet.PPB_NOSO_ACTION"
const val NOSO_NOTIFICATION_CHANNEL_ID = "NOSO_POP_CHANNEL_7"
const val NOSO_NOTIFICATION_CHANNEL_NAME = "Noso PoP Service"
const val NOSO_NOTIFICATION_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_HIGH
const val NOSO_POP_NOTIFICATION_ID = 7020090
const val NOSO_POP_RECURRENT_TASK_CODE = 729927

const val NOSO_INTENT_POP_ADDRESS = "POP_ADDRESS"
const val NOSO_INTENT_POP_PASSWORD = "POP_COUNTER"
const val NOSO_INTENT_POP_BLOCK = "POP_START_BLOCK"
const val NOSO_INTENT_POP_POOLS = "POP_POOL_LIST"

class PoPService: Service() {

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        mpDisk.setContext(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val minerAddress = intent.getStringExtra(NOSO_INTENT_POP_ADDRESS)
        val minerPassword = intent.getStringExtra(NOSO_INTENT_POP_PASSWORD)
        val currentBlock = intent.getLongExtra(NOSO_INTENT_POP_BLOCK, 0L)
        val pools =
            intent.getSerializableExtra(NOSO_INTENT_POP_POOLS) as ArrayList<HashMap<Int, String>>

        showForegroundNotification(pools.size)

        runAndSchedule(
            currentBlock = currentBlock,
            minerAddress = minerAddress ?: "INVALID_ADDRESS",
            minerPassword = minerPassword ?: "INVALID_PASSWORD",
            poolList = pools
        )

        return START_STICKY
    }

    private fun runAndSchedule(currentBlock: Long, minerAddress: String, minerPassword: String, poolList: List<HashMap<Int, String>>){
        val blockAge = (System.currentTimeMillis()%600000) / 1000
        val popInterval = if(blockAge in 11..584){
            0L
        } else if(blockAge < 10) {
            (10L-blockAge)*1000
        } else {
            (600-blockAge+10L)*1000
        }
        val intent = Intent(this, PoPBroadcast::class.java).apply {
            putExtra(NOSO_INTENT_POP_ADDRESS,minerAddress)
            putExtra(NOSO_INTENT_POP_PASSWORD,minerPassword)
            putExtra(NOSO_INTENT_POP_POOLS, ArrayList(poolList))
            putExtra(NOSO_INTENT_POP_BLOCK, currentBlock)
            action = NOSO_POP_RECURRENT_ACTION
        }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getBroadcast(this, NOSO_POP_RECURRENT_TASK_CODE, intent, flags)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+popInterval, pendingIntent)
    }

    private fun showForegroundNotification(poolSize:Int){
        val notificationIntent = Intent(this, MainActivity::class.java)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags)

        val notification = NotificationCompat.Builder(this, NOSO_NOTIFICATION_CHANNEL_ID).apply {
            setContentTitle("Noso PoP Service")
            setContentText("[Waiting Pool Sync] PoP Accepted 0/$poolSize")
            setSmallIcon(R.drawable.ic_notification_simple)
            setContentIntent(pendingIntent)
            setOngoing(true)
            color = nosoColor.toArgb()
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
}

class PoPBroadcast: BroadcastReceiver() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private fun getRawNotification(context:Context): NotificationCompat.Builder {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val notificationFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val notificationPendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, notificationFlags)

        return NotificationCompat.Builder(context, NOSO_NOTIFICATION_CHANNEL_ID).apply {
            setContentTitle("Noso PoP Service")
            setSmallIcon(R.drawable.ic_notification_simple)
            setContentIntent(notificationPendingIntent)
            setOngoing(true)
            color = nosoColor.toArgb()
            foregroundServiceBehavior = FOREGROUND_SERVICE_IMMEDIATE
        }
    }

    private fun randomInterval():Long{
        val startLimit = 10
        val finishLimit = 585

        ThreadLocalRandom.current().nextInt(finishLimit-startLimit).let {
            val blockAge = (System.currentTimeMillis()%600000) / 1000
            val timeTilNextParticipation = 600-blockAge+(startLimit + it.toLong())
            Log.e("PPS","Time until next participation $timeTilNextParticipation seg.")
            return timeTilNextParticipation*1000
        }
    }

    private fun runAndSchedule(
        currentBlock: Long,
        minerAddress:String,
        minerPassword:String,
        poolList:List<HashMap<Int, String>>,
        context: Context
    ){
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        scope.launch {
            var popAccepted = 0
            var updatedBlock = currentBlock
            poolList.forEach { pool ->
                var retry = 5
                var poolData = mpNetwork.getPoolData(
                    address = pool[0] as String,
                    port = (pool[1] as String).toInt(),
                    minerAddress = minerAddress,
                    minerPassword = minerPassword
                )

                while(poolData.Invalid && retry > 0){
                    delay(1500)
                    poolData = mpNetwork.getPoolData(
                        address = pool[0] as String,
                        port = (pool[1] as String).toInt(),
                        minerAddress = minerAddress,
                        minerPassword = minerPassword
                    )
                    retry -= 1
                }

                if(!poolData.Invalid){
                    popAccepted += 1
                }

                if(poolData.CurrentBlock > updatedBlock){
                    updatedBlock = poolData.CurrentBlock
                }
            }

            getRawNotification(context).let {
                it.setContentText("[${updatedBlock}] PoP Accepted $popAccepted/${poolList.size}")
                notificationManager.notify(NOSO_POP_NOTIFICATION_ID, it.build())
            }
        }

        val popInterval = randomInterval()
        val intent = Intent(context, PoPBroadcast::class.java).apply {
            putExtra(NOSO_INTENT_POP_ADDRESS,minerAddress)
            putExtra(NOSO_INTENT_POP_PASSWORD,minerPassword)
            putExtra(NOSO_INTENT_POP_POOLS, ArrayList(poolList))
            putExtra(NOSO_INTENT_POP_BLOCK, currentBlock)
            action = NOSO_POP_RECURRENT_ACTION
        }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getBroadcast(context, NOSO_POP_RECURRENT_TASK_CODE, intent, flags)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+popInterval, pendingIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val minerAddress = intent.getStringExtra(NOSO_INTENT_POP_ADDRESS)
        val minerPassword = intent.getStringExtra(NOSO_INTENT_POP_PASSWORD)
        val currentBlock = intent.getLongExtra(NOSO_INTENT_POP_BLOCK, 0L)
        val pools = intent.getSerializableExtra(NOSO_INTENT_POP_POOLS) as ArrayList<HashMap<Int, String>>

        runAndSchedule(
            currentBlock = currentBlock,
            minerAddress = minerAddress?:"INVALID_ADDRESS",
            minerPassword = minerPassword?:"INVALID_PASSWORD",
            poolList =  pools,
            context = context
        )
    }
}