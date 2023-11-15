package com.example.sstp_flutter

import com.example.sstp_flutter.preference.OscPrefKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FlutterCaller {

//    suspend fun disconnect(){
//        withContext(Dispatchers.Main) {
//            val tempMap : HashMap<String,Any>  = HashMap()
//
//            tempMap.put("disconnected",true)
//            tempMap.put("dns",dnsEnabled)
//            tempMap.put("dnsCount", dnsCount)
//            connectionStatus = OscPrefKey.Disconnected.name
//            flutterChannel.invokeMethod("connectResponse", tempMap)
//        }
//    }
     fun connect(){
         connectionStatus = OscPrefKey.Connected.name
        flutterChannel.invokeMethod("connectResponse", "connected")
    }

    fun connecting(){
        connectionStatus = OscPrefKey.Connecting.name
        flutterChannel.invokeMethod("connectResponse", "connecting")
    }

    suspend fun DownloadSpeed(downloadSpeed : String){
        withContext(Dispatchers.Main) {
            flutterChannel.invokeMethod("downloadSpeed",downloadSpeed )
        }

    }

    suspend fun UploadSpeed(uploadSpeed : String  ){
        withContext(Dispatchers.Main) {
            flutterChannel.invokeMethod("uploadSpeed",uploadSpeed )
        }

    }

}