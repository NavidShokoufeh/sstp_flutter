import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:sstp_flutter/src/core/Utils/utils.dart';
import 'package:sstp_flutter/app_info.dart';
import 'package:sstp_flutter/proxy.dart';
import 'package:sstp_flutter/server.dart';

class MethodChannelSstpFlutter {
  final methodChannelCaller = const MethodChannel('sstp_flutter');

  Future<String?> getPlatformVersion() async {
    final version =
        await methodChannelCaller.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  Future connectVpn() async {
    try {
      await methodChannelCaller.invokeMethod("connect");
    } on PlatformException catch (e) {
      print(e);
      rethrow;
    }
  }

  Future disconnect() async {
    try {
      await methodChannelCaller.invokeMethod("disconnect");
    } on PlatformException catch (e) {
      print(e);
      rethrow;
    }
  }

  Future saveCnnectionStatus(bool value) async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    prefs.setBool("connected", value);
  }

  Future vpnPermission() async {
    try {
      await methodChannelCaller.invokeMethod("takePermission");
    } on PlatformException catch (e) {
      print(e);
      rethrow;
    }
  }

  Future<List<InstalledAppInfo>> getInstalledApps() async {
    List<InstalledAppInfo> appInfoList = [];
    List<dynamic> apps = await methodChannelCaller.invokeMethod("getApps");
    appInfoList = apps.map((app) => InstalledAppInfo.create(app)).toList();

    appInfoList.sort((a, b) => a.name!.compareTo(b.name!));
    return appInfoList;
  }

  Future<List<String>> getAllowedApps() async {
    List<String> caller = await methodChannelCaller
        .invokeMethod("getAllowedApps"); //returns list of package names
    return caller;
  }

  Future addToAllowedApps(List<String> pkgName) async {
    var caller = await methodChannelCaller
        .invokeMethod("addAllowedApps", {"packageName": pkgName});
    return caller;
  }

  Future enableDNS({required String customDNS}) async {
    await methodChannelCaller
        .invokeMethod("enableDns", {"customDns": customDNS});
  }

  Future disableDNS() async {
    await methodChannelCaller.invokeMethod("disableDns");
  }

  Future enableProxy({required SSTPProxy proxy}) async {
    await methodChannelCaller.invokeMethod("enableProxy", {
      "proxyIp": proxy.ip,
      "proxyPort": proxy.port,
      "proxyUserName": proxy.userName ?? "",
      "proxyPassword": proxy.password ?? "",
    });
  }

  Future disableProxy() async {
    await methodChannelCaller.invokeMethod("disableProxy");
  }

  Future saveServerData({required SSTPServer server}) async {
    try {
      var res = await methodChannelCaller.invokeMethod("saveServer", {
        "hostName": server.host,
        "sslPort": server.port,
        "userName": server.username,
        "password": server.password,
        "verifyHostName": server.verifyHostName,
        "useTrustedCert": server.useTrustedCert,
        "showDisconnectOnNotification": server.showDisconnectOnNotification,
        "notificationText": server.notificationText
      });
      print(res);
    } catch (e) {
      print(e);
    }
  }

  Future<SSTPConnectionStatusKeys> checkLastConnectionStatus() async {
    SSTPConnectionStatusKeys status =
        await methodChannelCaller.invokeMethod("checkLastConnectionStatus");
    return status;
  }

  Future<String> addCertificate() async {
    String caller =
        await methodChannelCaller.invokeMethod("addTrustedCertificate");
    return caller;
  }

  static final MethodChannelSstpFlutter _instance =
      MethodChannelSstpFlutter.internal();
  factory MethodChannelSstpFlutter() => _instance;
  MethodChannelSstpFlutter.internal();
}
