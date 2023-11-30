import 'package:flutter/services.dart';
import 'package:sstp_flutter/src/core/Utils/utils.dart';
import 'package:sstp_flutter/traffic.dart';
import 'package:sstp_flutter/app_info.dart';
import 'package:sstp_flutter/proxy.dart';
import 'package:sstp_flutter/server.dart';
import 'package:sstp_flutter/src/data/provider/sstp_flutter_method_channel.dart';

typedef OnConnected = Function(ConnectionTraffic);
typedef OnConnecting = void Function();
typedef OnDisconnected = void Function();
typedef OnError =  Function();

class SstpFlutter {

  
  MethodChannelSstpFlutter channelHandler = MethodChannelSstpFlutter();

  Future<String?> getPlatformVersion() {
    return channelHandler.getPlatformVersion();
    
  }


  Future onResult({OnConnected? onConnectedResult,OnConnecting? onConnectingResult,OnDisconnected? onDisconnectedResult,OnError? onError}) async{
    MethodChannel channel = const MethodChannel("responseReceiver");
    double downloadTraffic = 0;
    double uploadTraffic = 0;
    ConnectionTraffic traffic = ConnectionTraffic(downloadTraffic: downloadTraffic,uploadTraffic:uploadTraffic );

    Future methodCallReceiver(MethodCall call) async {
    var arg = call.arguments;
    

    if(call.method == 'connectResponse'){
      if(arg["status"] == UtilKeys.CONNECTED){

      onConnectedResult!(traffic);

    }else if(arg["status"] == UtilKeys.CONNECTING){
       onConnectingResult!();
    }else if(arg["status"] == UtilKeys.DISCONNECTED){

       onDisconnectedResult!();

       bool? error = arg["error"];
       if(error!=null && error) onError!();

    }
  }else if(call.method == 'downloadSpeed'){
    downloadTraffic = double.parse(call.arguments);
    traffic = ConnectionTraffic(downloadTraffic: downloadTraffic,uploadTraffic:uploadTraffic );
    onConnectedResult!(traffic);
  }else if(call.method == 'uploadSpeed'){
    uploadTraffic = double.parse(call.arguments);
    traffic = ConnectionTraffic(downloadTraffic: downloadTraffic,uploadTraffic:uploadTraffic );
    onConnectedResult!(traffic);
  }
}

  channel.setMethodCallHandler(methodCallReceiver);
  }
  
  Future connectVpn() async{
    try{
      var caller = await channelHandler.connectVpn();
      return caller;
    }on PlatformException catch (e){
      print(e);
       rethrow;
    }
  }

  Future takePermission() async {
    try{
     await channelHandler.vpnPermission();
    }catch(e){
      print(e);
      rethrow;
    }
  }
  
  Future disconnect() async {
    await channelHandler.disconnect();
  }
  
  Future<List<InstalledAppInfo>> getInstalledApps() async {

   List<InstalledAppInfo> apps = await channelHandler.getInstalledApps();
   //Todo

    // for (var element in apps) {
    //   pkgNAmes.add(element.packageName?? "");
    // }
    // await enableAllApps(channelHandler);
    // update();
    return apps;
  }

  Future<List<String>> getAllowedApps() async {
    List<String> packages = await channelHandler.getAllowedApps();
    return packages;
  }

  Future addToAllowedApps({required List<String> packages}) async {
    try{
      await channelHandler.addToAllowedApps(packages);
    }catch(e){
      rethrow;
    }
  }
  
  Future enableDns({required String DNS}) async {
    try{
      await channelHandler.enableDNS(customDNS: DNS);
    }catch(e){
      rethrow;
    }
  }

  Future disableDNS() async {
    try{
     await channelHandler.disableDNS();
    }catch(e){
      rethrow;
    }
  }

  Future enableProxy({required SSTPProxy proxy}) async {
    try{
    await channelHandler.enableProxy(proxy: proxy);
    }catch(e){
      rethrow;
    }
  }

  Future disableProxy() async {
    try{
      await channelHandler.disableProxy();
    }catch(e){
      rethrow;
    }
  }

  Future saveServerData({required SSTPServer server}) async {
    try{
      await channelHandler.saveServerData(server: server);
    }catch(e){
      // print(e);
      // rethrow;
    }
  }

  Future<UtilKeys> checkLastConnectionStatus() async {
    UtilKeys status = await channelHandler.checkLastConnectionStatus();
    return status;
  }
   

  static final SstpFlutter _instance = SstpFlutter.internal();
  factory SstpFlutter() => _instance;
  SstpFlutter.internal();

}
