package com.example.sstp_flutter

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
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
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

lateinit var flutterChannel : MethodChannel
var dnsEnabled : Boolean = false
var dnsCount : Int = 0
var connectionStatus : String = OscPrefKey.Disconnected.name
var localPermission = false
var manuallyDisconnected : Boolean = false
var errorConnection : Boolean = false

/** SstpFlutterPlugin by Navid Shokoufeh*/
class SstpFlutterPlugin: FlutterPlugin, MethodCallHandler , FlutterFragmentActivity() {

  lateinit var prefs : SharedPreferences

  private val preparationLauncher = (this as ComponentActivity).registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()) { result ->
    localPermission = result.resultCode == Activity.RESULT_OK
  }

  private lateinit var channel : MethodChannel

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "sstp_flutter")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when {
      call.method.equals("takePermission") -> {
        Thread{
          VpnService.prepare(this)?.also { intent ->
            preparationLauncher.launch(intent)
            result.success(localPermission)
          } ?: result.success(localPermission)
        }.start()
      }

      call.method.equals("connect") -> {
        if(localPermission){
          FlutterCaller().connecting()
          startVpnService(ACTION_VPN_CONNECT)
        }else{
          takePermission(call, true)
        }
      }

      call.method.equals("disconnect") -> {
        startVpnService(ACTION_VPN_DISCONNECT)
      }

      call.method.equals("getApps") -> {
        Thread {
          val apps: List<Map<String, Any?>> =
            getInstalledApps()
          result.success(apps)
        }.start()
      }

      call.method.equals("addAllowedApps") -> {
        Thread {
          val status =
            call.argument<ArrayList<String>>("packageName")?.let { memorizeAllowedApps(it) }
          result.success(status)
        }.start()
      }

      call.method.equals("getAllowedApps") -> {
      Thread {
        val apps : List<String> =
          getAllowedApps()
        result.success(apps)
      }.start()
    }

      call.method.equals("enableDns") -> {
        val dns = call.argument<String>("customDns")
        setBooleanPrefValue(true, OscPrefKey.DNS_DO_USE_CUSTOM_SERVER,prefs)
        if (dns != null) {
          setStringPrefValue(dns, OscPrefKey.DNS_CUSTOM_ADDRESS,prefs)
        }
        dnsEnabled = true
        dnsCount++
        result.success(true)
      }

      call.method.equals("disableDns") -> {
        setBooleanPrefValue(false, OscPrefKey.DNS_DO_USE_CUSTOM_SERVER,prefs)
        setStringPrefValue("", OscPrefKey.DNS_CUSTOM_ADDRESS,prefs)
        dnsEnabled = false
        dnsCount = 0
        result.success(true)
      }

      call.method.equals("enableProxy") -> {
        val proxyIp = call.argument<String>("proxyIp")
        val proxyPort = call.argument<String>("proxyPort")
        val proxyUserName = call.argument<String>("proxyUserName")
        val proxyPassword = call.argument<String>("proxyPassword")

        setBooleanPrefValue(true, OscPrefKey.PROXY_DO_USE_PROXY,prefs)

        if (proxyIp != null) {
          setStringPrefValue(proxyIp, OscPrefKey.PROXY_HOSTNAME,prefs)
        }
        if (proxyPort != null) {
          setStringPrefValue(proxyPort, OscPrefKey.PROXY_PORT,prefs)
        }
        if (proxyUserName != null) {
          setStringPrefValue(proxyUserName, OscPrefKey.PROXY_USERNAME,prefs)
        }
        if (proxyPassword != null) {
          setStringPrefValue(proxyPassword, OscPrefKey.PROXY_PASSWORD,prefs)
        }

      }

      call.method.equals("disableProxy") -> {
        setBooleanPrefValue(false, OscPrefKey.PROXY_DO_USE_PROXY,prefs)
        setStringPrefValue("", OscPrefKey.PROXY_HOSTNAME,prefs)
        setStringPrefValue("", OscPrefKey.PROXY_PORT,prefs)
        setStringPrefValue("", OscPrefKey.PROXY_USERNAME,prefs)
        setStringPrefValue("", OscPrefKey.PROXY_PASSWORD,prefs)
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
      val intent = Intent(this, SstpVpnService::class.java).setAction(action).putExtra("manuallyDisconnected",false)
      this.startForegroundService(intent)
    } else {
      val intent = Intent(this, SstpVpnService::class.java).setAction(action).putExtra("manuallyDisconnected",true)
      this.startService(intent)
    }
  }

  private fun takePermission(
    call: MethodCall,
    connect: Boolean,
  ) : Boolean {
    VpnService.prepare(this)?.also { intent ->
      preparationLauncher.launch(intent)
    } ?:
    if(connect){
      FlutterCaller().connecting()
      saveData(call)
      startVpnService(ACTION_VPN_CONNECT)
    }

    return localPermission
  }

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
}
