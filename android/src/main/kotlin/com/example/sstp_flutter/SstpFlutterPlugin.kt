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
import com.example.sstp_flutter.preference.accessor.getSetPrefValue
import com.example.sstp_flutter.preference.accessor.setBooleanPrefValue
import com.example.sstp_flutter.preference.accessor.setSetPrefValue
import com.example.sstp_flutter.preference.accessor.setStringPrefValue
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
  private lateinit var activityBinding: ActivityPluginBinding
  lateinit var prefs : SharedPreferences

  lateinit var context : Context

  private lateinit var channel : MethodChannel

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "sstp_flutter")
    channel.setMethodCallHandler(this)
    println("bf prefs")
    context = flutterPluginBinding.applicationContext
    prefs = PreferenceManager.getDefaultSharedPreferences(flutterPluginBinding.applicationContext)
    flutterChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "responseReceiver")
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activityBinding = binding

  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when {
      call.method.equals("takePermission") -> {
        val intent = VpnService.prepare(activityBinding.activity.applicationContext)

        if(intent != null){
          println("not null")
          var listener: PluginRegistry.ActivityResultListener? = null
          listener = PluginRegistry.ActivityResultListener { req, res, _ ->
            result.success(req == 0 && res == RESULT_OK)
            listener?.let { activityBinding.removeActivityResultListener(it) }
            true
          }
          activityBinding.addActivityResultListener(listener)
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
        println("enabling dns $dns")
        setBooleanPrefValue(true,OscPrefKey.DNS_DO_USE_CUSTOM_SERVER,prefs)
        if (dns != null) {
          setStringPrefValue(dns,OscPrefKey.DNS_CUSTOM_ADDRESS,prefs)
        }
        dnsEnabled = true
        dnsCount++
        println("enabled dns")
        result.success(true)
      }

      call.method.equals("disableDns") -> {
        println("disabling dns")
        setBooleanPrefValue(false,OscPrefKey.DNS_DO_USE_CUSTOM_SERVER,prefs)
        setStringPrefValue("",OscPrefKey.DNS_CUSTOM_ADDRESS,prefs)
        println("disabled dns")
        dnsEnabled = false
        dnsCount = 0
        result.success(true)
      }

      call.method.equals("enableProxy") -> {
        val proxyIp = call.argument<String>("proxyIp")
        val proxyPort = call.argument<String>("proxyPort")
        val proxyUserName = call.argument<String>("proxyUserName")
        val proxyPassword = call.argument<String>("proxyPassword")
        println("enabling dns $proxyIp")

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
        println("enabled proxy")
      }

      call.method.equals("disableProxy") -> {
        println("disabling proxy")
        setBooleanPrefValue(false,OscPrefKey.PROXY_DO_USE_PROXY,prefs)
        setStringPrefValue("",OscPrefKey.PROXY_HOSTNAME,prefs)
        setStringPrefValue("",OscPrefKey.PROXY_PORT,prefs)
        setStringPrefValue("",OscPrefKey.PROXY_USERNAME,prefs)
        setStringPrefValue("",OscPrefKey.PROXY_PASSWORD,prefs)
        println("disabled proxy")
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
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  private fun saveData(call: MethodCall) {
    val hostName = call.argument<String>("hostName")
    val userName = call.argument<String>("userName")
    val password = call.argument<String>("password")
    setStringPrefValue(
      hostName ?:"", OscPrefKey.HOME_HOSTNAME,prefs
    )
    setStringPrefValue(
      userName ?:"", OscPrefKey.HOME_USERNAME,prefs
    )
    setStringPrefValue(
      password ?:"", OscPrefKey.HOME_PASSWORD,prefs
    )
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
    val packageManager = packageManager
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
