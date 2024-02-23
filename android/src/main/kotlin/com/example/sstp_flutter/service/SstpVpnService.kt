package com.example.sstp_flutter.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.example.sstp_flutter.FlutterCaller
import com.example.sstp_flutter.R
import com.example.sstp_flutter.client.ClientBridge
import com.example.sstp_flutter.client.control.ControlClient
import com.example.sstp_flutter.client.control.LogWriter
import com.example.sstp_flutter.connectionStatus
import com.example.sstp_flutter.dnsCount
import com.example.sstp_flutter.dnsEnabled
import com.example.sstp_flutter.errorConnection
import com.example.sstp_flutter.flutterChannel
import com.example.sstp_flutter.manuallyDisconnected
import com.example.sstp_flutter.preference.OscPrefKey
import com.example.sstp_flutter.preference.accessor.getBooleanPrefValue
import com.example.sstp_flutter.preference.accessor.getIntPrefValue
import com.example.sstp_flutter.preference.accessor.getURIPrefValue
import com.example.sstp_flutter.preference.accessor.resetReconnectionLife
import com.example.sstp_flutter.preference.accessor.setBooleanPrefValue
import com.example.sstp_flutter.preference.accessor.setIntPrefValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


internal const val ACTION_VPN_CONNECT = "com.example.sstp_flutter.connect"
internal const val ACTION_VPN_DISCONNECT = "com.example.sstp_flutter.disconnect"

internal const val NOTIFICATION_CHANNEL_NAME = "com.example.sstp_flutter.notification.channel"

internal const val NOTIFICATION_ERROR_ID = 1
internal const val NOTIFICATION_RECONNECT_ID = 2
internal const val NOTIFICATION_DISCONNECT_ID = 3

internal class SstpVpnService : VpnService() {
    private lateinit var prefs: SharedPreferences
    private lateinit var listener: SharedPreferences.OnSharedPreferenceChangeListener
    private lateinit var notificationManager: NotificationManagerCompat
    internal lateinit var scope: CoroutineScope

    internal var logWriter: LogWriter? = null
    private var controlClient: ControlClient?  = null

    private var jobReconnect: Job? = null

    private fun setRootState(state: Boolean) {
        setBooleanPrefValue(state, OscPrefKey.ROOT_STATE, prefs)
    }

    private fun requestTileListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TileService.requestListeningState(this,
                ComponentName(this, SstpTileService::class.java)
            )
        }
    }

    override fun onCreate() {
        notificationManager = NotificationManagerCompat.from(this)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == OscPrefKey.ROOT_STATE.name) {
                val newState = getBooleanPrefValue(OscPrefKey.ROOT_STATE, prefs)
                if(newState){
//                    FlutterCaller().connecting()
                }else{
                    val tempMap : HashMap<String,Any>  = HashMap()
                    connectionStatus = OscPrefKey.Disconnected.name
                    tempMap.put("status",connectionStatus)
                    tempMap.put("dns", dnsEnabled)
                    tempMap.put("dnsCount", dnsCount)
                    tempMap.put("error", errorConnection)

                    flutterChannel.invokeMethod("connectResponse", tempMap)
                    errorConnection = false
                }
                println("listener onCreate sstp service $newState")
                setBooleanPrefValue(newState, OscPrefKey.HOME_CONNECTOR, prefs)
                requestTileListening()
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)

        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_VPN_CONNECT -> {
                if(intent.getBooleanExtra("manuallyDisconnected",false)){
                    manuallyDisconnected = false
                }

                FlutterCaller().connecting()
                controlClient?.kill(false, null)

                beForegrounded()
                resetReconnectionLife(prefs)
                if (getBooleanPrefValue(OscPrefKey.LOG_DO_SAVE_LOG, prefs)) {
                    prepareLogWriter()
                }

                logWriter?.write("Establish VPN connection")

                initializeClient()

                setRootState(true)

                Service.START_STICKY
            }

            else -> {
                if(intent?.getBooleanExtra("manuallyDisconnected",false) == true){
                    manuallyDisconnected = true
                }
                connectionStatus = OscPrefKey.Disconnected.name
                // ensure that reconnection has been completely canceled or done
                runBlocking { jobReconnect?.cancelAndJoin() }

                controlClient?.disconnect()
                controlClient = null

                close()

                val tempMap : HashMap<String,Any>  = HashMap()
                connectionStatus = OscPrefKey.Disconnected.name
                tempMap.put("status",connectionStatus)
                tempMap.put("dns", dnsEnabled)
                tempMap.put("dnsCount", dnsCount)
                tempMap.put("error", errorConnection)

                flutterChannel.invokeMethod("connectResponse", tempMap)
                errorConnection = false

                Service.START_NOT_STICKY
            }
        }
    }

    private fun initializeClient() {
        controlClient = ControlClient(ClientBridge(this)).also {
            it.launchJobMain()
        }
    }

    private fun prepareLogWriter() {
        val currentDateTime = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val filename = "log_osc_${currentDateTime}.txt"

        val prefURI = getURIPrefValue(OscPrefKey.LOG_DIR, prefs)
        if (prefURI == null) {
            makeNotification(NOTIFICATION_ERROR_ID, "LOG: ERR_NULL_PREFERENCE")
            return
        }

        val dirURI = DocumentFile.fromTreeUri(this, prefURI)
        if (dirURI == null) {
            makeNotification(NOTIFICATION_ERROR_ID, "LOG: ERR_NULL_DIRECTORY")
            return
        }

        val fileURI = dirURI.createFile("text/plain", filename)
        if (fileURI == null) {
            makeNotification(NOTIFICATION_ERROR_ID, "LOG: ERR_NULL_FILE")
            return
        }

        val stream = contentResolver.openOutputStream(fileURI.uri, "wa")
        if (stream == null) {
            makeNotification(NOTIFICATION_ERROR_ID, "LOG: ERR_NULL_STREAM")
            return
        }

        logWriter = LogWriter(stream)
    }

    internal fun launchJobReconnect() {
        jobReconnect = scope.launch {
            try {
                getIntPrefValue(OscPrefKey.RECONNECTION_LIFE, prefs).also {
                    val life = it - 1
                    setIntPrefValue(life, OscPrefKey.RECONNECTION_LIFE, prefs)

                    val message = "Reconnection will be tried (LIFE = $life)"
                    makeNotification(NOTIFICATION_RECONNECT_ID, message)
                    logWriter?.report(message)
                }

                delay(getIntPrefValue(OscPrefKey.RECONNECTION_INTERVAL, prefs) * 1000L)

                initializeClient()
            } catch (_: CancellationException) { }
            finally {
                cancelNotification(NOTIFICATION_RECONNECT_ID)
            }
        }
    }

    private fun beForegrounded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                NOTIFICATION_CHANNEL_NAME,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).also {
                notificationManager.createNotificationChannel(it)
            }
        }

        val pendingIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, SstpVpnService::class.java).setAction(ACTION_VPN_DISCONNECT).putExtra("manuallyDisconnected",true),
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationText = prefs.getString(OscPrefKey.NotificationText.name,"")
        val showNotification = prefs.getBoolean(OscPrefKey.NOTIFICATION_DO_SHOW_DISCONNECT.name,false)
        println("notificationText : $notificationText")
        println("showNotification : $showNotification")



        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_NAME).also {
            it.priority = NotificationCompat.PRIORITY_DEFAULT
            it.setAutoCancel(true)
//            it.setSmallIcon(R.drawable.flutterlogo)
            it.setContentText(notificationText)
            if(showNotification){
                it.addAction(0, "DISCONNECT", pendingIntent)
            }
        }

        startForeground(NOTIFICATION_DISCONNECT_ID, builder.build())
    }

    internal fun makeNotification(id: Int, message: String) {
        println("err msg : $message")
        errorConnection = true

    }

    internal fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }

    internal fun close() {
        connectionStatus = OscPrefKey.Disconnected.name
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        logWriter?.write("Terminate VPN connection")
        logWriter?.close()
        logWriter = null

        controlClient?.kill(false, null)
        controlClient = null

        scope.cancel()

        setRootState(false)
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
