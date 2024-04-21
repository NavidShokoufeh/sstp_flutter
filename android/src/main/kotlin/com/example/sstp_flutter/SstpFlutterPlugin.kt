package com.example.sstp_flutter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.PreferenceManager
import com.example.sstp_flutter.preference.OscPrefKey
import com.example.sstp_flutter.preference.accessor.getBooleanPrefValue
import com.example.sstp_flutter.preference.accessor.getSetPrefValue
import com.example.sstp_flutter.preference.accessor.getURIPrefValue
import com.example.sstp_flutter.preference.accessor.setBooleanPrefValue
import com.example.sstp_flutter.preference.accessor.setIntPrefValue
import com.example.sstp_flutter.preference.accessor.setSetPrefValue
import com.example.sstp_flutter.preference.accessor.setStringPrefValue
import com.example.sstp_flutter.preference.accessor.setURIPrefValue
import com.example.sstp_flutter.preference.custom.DirectoryPreference
import com.example.sstp_flutter.service.ACTION_VPN_CONNECT
import com.example.sstp_flutter.service.ACTION_VPN_DISCONNECT
import com.example.sstp_flutter.service.SstpVpnService
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry

lateinit var flutterChannel : MethodChannel
var dnsEnabled : Boolean = false
var dnsCount : Int = 0
var connectionStatus : String = OscPrefKey.Disconnected.name
var manuallyDisconnected : Boolean = false
var errorConnection : Boolean = false
var assignedIp = ""

/** SstpFlutterPlugin by Navid Shokoufeh*/
class SstpFlutterPlugin: FlutterPlugin, MethodCallHandler , ActivityAware, FlutterFragmentActivity() {
  var activityResultListener: PluginRegistry.ActivityResultListener? = null
  private lateinit var activityBinding: ActivityPluginBinding
  private lateinit var channel : MethodChannel
  lateinit var prefs : SharedPreferences
  lateinit var context : Context
  lateinit var  tempResult : Result

  private val certDirLauncher = PluginRegistry.ActivityResultListener { req, res, data ->

    val uri = if (res == Activity.RESULT_OK) data?.data?.also {
      context.contentResolver.takePersistableUriPermission(
        it, Intent.FLAG_GRANT_READ_URI_PERMISSION
      )
    } else null

    setURIPrefValue(uri, OscPrefKey.SSL_CERT_DIR, prefs)
    Thread{
      tempResult.success(getURIPrefValue(OscPrefKey.SSL_CERT_DIR, prefs).toString())
    }.start()
    true
  }


  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "sstp_flutter")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
    prefs = PreferenceManager.getDefaultSharedPreferences(flutterPluginBinding.applicationContext)
    flutterChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "responseReceiver")
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activityBinding = binding

  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    tempResult = result
    when {
      call.method.equals("takePermission") -> {
        val intent = VpnService.prepare(activityBinding.activity.applicationContext)

        if(intent != null){

          activityResultListener = PluginRegistry.ActivityResultListener { req, res, _ ->
            result.success(req == 0 && res == RESULT_OK)
            activityResultListener?.let { activityBinding.removeActivityResultListener(it) }
            true
          }
          activityBinding.addActivityResultListener(activityResultListener!!)
          activityBinding.activity.startActivityForResult(intent, 0)
        }else{
          result.success(true)
        }
      }

      call.method.equals("connect") -> {
        Thread {
          startVpnService(ACTION_VPN_CONNECT)
          result.success(true)
        }.start()
      }

      call.method.equals("disconnect") -> {
        Thread{
          startVpnService(ACTION_VPN_DISCONNECT)
          result.success(true);
        }.start()
      }

      call.method.equals("getApps") -> {
        Thread {
          val apps: List<Map<String, Any?>> =
            getInstalledApps()
          result.success(apps)
        }.start()
      }

      call.method.equals("addAllowedApps") -> {
        println("adding allowed apps")
        Thread {
          val status =
            call.argument<ArrayList<String>>("packageName")?.let { memorizeAllowedApps(it) }
          result.success(status)
        }.start()

      }

      call.method.equals("getAllowedApps") ->{
        Thread {
          val apps : List<String> =
            getAllowedApps()
          result.success(apps)
        }.start()

      }

      call.method.equals("enableDns") -> {
        val dns = call.argument<String>("customDns")
        setBooleanPrefValue(true,OscPrefKey.DNS_DO_USE_CUSTOM_SERVER,prefs)
        if (dns != null) {
          setStringPrefValue(dns,OscPrefKey.DNS_CUSTOM_ADDRESS,prefs)
        }
        dnsEnabled = true
        dnsCount++
        result.success(true)
      }

      call.method.equals("disableDns") -> {
        setBooleanPrefValue(false,OscPrefKey.DNS_DO_USE_CUSTOM_SERVER,prefs)
        setStringPrefValue("",OscPrefKey.DNS_CUSTOM_ADDRESS,prefs)
        dnsEnabled = false
        dnsCount = 0
        result.success(true)
      }

      call.method.equals("enableProxy") -> {
        val proxyIp = call.argument<String>("proxyIp")
        val proxyPort = call.argument<String>("proxyPort")
        val proxyUserName = call.argument<String>("proxyUserName")
        val proxyPassword = call.argument<String>("proxyPassword")

        setBooleanPrefValue(true,OscPrefKey.PROXY_DO_USE_PROXY,prefs)

        if (proxyIp != null) {
          setStringPrefValue(proxyIp,OscPrefKey.PROXY_HOSTNAME,prefs)
        }
        if (proxyPort != null) {
          setStringPrefValue(proxyPort,OscPrefKey.PROXY_PORT,prefs)
        }
        if (proxyUserName != null) {
          setStringPrefValue(proxyUserName,OscPrefKey.PROXY_USERNAME,prefs)
        }
        if (proxyPassword != null) {
          setStringPrefValue(proxyPassword,OscPrefKey.PROXY_PASSWORD,prefs)
        }
      }

      call.method.equals("disableProxy") -> {
        setBooleanPrefValue(false,OscPrefKey.PROXY_DO_USE_PROXY,prefs)
        setStringPrefValue("",OscPrefKey.PROXY_HOSTNAME,prefs)
        setStringPrefValue("",OscPrefKey.PROXY_PORT,prefs)
        setStringPrefValue("",OscPrefKey.PROXY_USERNAME,prefs)
        setStringPrefValue("",OscPrefKey.PROXY_PASSWORD,prefs)
      }

      call.method.equals("checkLastConnectionStatus") -> {
        Thread {
          result.success(connectionStatus)
        }.start()
      }

      call.method.equals("saveServer") -> {
        Thread {
          saveData(call)
          result.success(true)
        }.start()
      }

      call.method.equals("addTrustedCertificate") -> {
        addTrustedCertificate()
      }
    }
  }

  fun addTrustedCertificate(): String {

    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
      intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
      activityBinding.addActivityResultListener(certDirLauncher)
      activityBinding.activity.startActivityForResult(intent, 0).also {
        return getURIPrefValue(OscPrefKey.SSL_CERT_DIR, prefs).toString()
      }
    }

  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  private fun saveData(call: MethodCall) {
    val hostName = call.argument<String>("hostName")
    val userName = call.argument<String>("userName")
    val password = call.argument<String>("password")
    val sslPort = call.argument<Int>("sslPort")
    val verifyHostName = call.argument<Boolean>("verifyHostName")
    val useTrustedCert = call.argument<Boolean>("useTrustedCert")
    val sslVersion = call.argument<String>("sslVersion")
    val showDisconnectOnNotification = call.argument<Boolean>("showDisconnectOnNotification")
    val notificationText = call.argument<String>("notificationText")

    setStringPrefValue(
      hostName ?:"", OscPrefKey.HOME_HOSTNAME,prefs
    )
    setStringPrefValue(
      userName ?:"", OscPrefKey.HOME_USERNAME,prefs
    )
    setStringPrefValue(
      password ?:"", OscPrefKey.HOME_PASSWORD,prefs
    )
    setIntPrefValue(
      sslPort ?: 443, OscPrefKey.SSL_PORT,prefs
    )

    setBooleanPrefValue(showDisconnectOnNotification ?: false,OscPrefKey.NOTIFICATION_DO_SHOW_DISCONNECT,prefs)
    setBooleanPrefValue(useTrustedCert ?: false,OscPrefKey.SSL_DO_ADD_CERT,prefs)

    setStringPrefValue(
      sslVersion ?:"", OscPrefKey.SSL_VERSION,prefs
    )

    if(!notificationText.isNullOrEmpty()){
      setStringPrefValue(
        notificationText , OscPrefKey.NotificationText,prefs
      )
    }else{
      setStringPrefValue(
        "", OscPrefKey.NotificationText,prefs
      )
    }
    if (verifyHostName == true){
      setBooleanPrefValue(true,OscPrefKey.SSL_DO_VERIFY,prefs)
    }else{
      setBooleanPrefValue(false,OscPrefKey.SSL_DO_VERIFY,prefs)
    }
  }

  private fun startVpnService(
    action: String,
  ) {


    if (action == ACTION_VPN_CONNECT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val intent = Intent(context, SstpVpnService::class.java).setAction(action).putExtra("manuallyDisconnected",false)
      context.startForegroundService(intent)
    } else {
      val intent = Intent(context, SstpVpnService::class.java).setAction(action).putExtra("manuallyDisconnected",true)
      context.startService(intent)
    }
  }

//  private fun takePermission(
//    call: MethodCall,
//    connect: Boolean,
//  ) : Boolean {
//    VpnService.prepare(context)?.also { intent ->
//      preparationLauncher.launch(intent)
//    } ?:
//    if(connect){
//      FlutterCaller().connecting()
//      saveData(call)
//      startVpnService(ACTION_VPN_CONNECT)
//    }
//
//    return localPermission
//  }

  private fun getInstalledApps(): List<Map<String, Any?>> {
    val packageManager = context.packageManager
    var installedApps = packageManager.getInstalledApplications(0)
    installedApps =
      installedApps.filter { app -> !isSystemApp(packageManager, app.packageName) }
    return installedApps.map { app -> Util.convertAppToMap(packageManager, app, true) }
  }

  private fun isSystemApp(packageManager: PackageManager, packageName: String): Boolean {
    return packageManager.getLaunchIntentForPackage(packageName) == null
  }

  private fun memorizeAllowedApps(packageName: ArrayList<String>): Boolean {
    val allowed = mutableSetOf<String>()
    for(element in packageName){
      allowed.add(element)
    }
    if(allowed.size >0){
      setBooleanPrefValue(true,OscPrefKey.ROUTE_DO_ENABLE_APP_BASED_RULE, prefs)
    }else{
      setBooleanPrefValue(false,OscPrefKey.ROUTE_DO_ENABLE_APP_BASED_RULE, prefs)
    }
    setSetPrefValue(allowed, OscPrefKey.ROUTE_ALLOWED_APPS, prefs)
    return true
  }

  private  fun getAllowedApps(): List<String> {
    val allowed = getSetPrefValue(OscPrefKey.ROUTE_ALLOWED_APPS, prefs)
    return allowed.toList()
  }

  override fun onDestroy() {
    super.onDestroy()
  }

  override fun onDetachedFromActivityForConfigChanges() {
    TODO("Not yet implemented")
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    TODO("Not yet implemented")
  }

  override fun onDetachedFromActivity() {
    TODO("Not yet implemented")
  }
}
