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
typedef OnError = Function();

class SstpFlutter {
  MethodChannelSstpFlutter channelHandler = MethodChannelSstpFlutter();

  /// Gains result of current connection status
  /// [OnConnected] get invoked when [ConnectionTraffic] get updates
  Future onResult(
      {OnConnected? onConnectedResult,
      OnConnecting? onConnectingResult,
      OnDisconnected? onDisconnectedResult,
      OnError? onError}) async {
    MethodChannel channel = const MethodChannel("responseReceiver");
    double downloadTraffic = 0;
    double uploadTraffic = 0;
    ConnectionTraffic traffic = ConnectionTraffic(
        downloadTraffic: downloadTraffic, uploadTraffic: uploadTraffic);

    Future methodCallReceiver(MethodCall call) async {
      var arg = call.arguments;

      if (call.method == 'connectResponse') {
        if (arg["status"] == SSTPConnectionStatusKeys.CONNECTED) {
          onConnectedResult!(traffic);
        } else if (arg["status"] == SSTPConnectionStatusKeys.CONNECTING) {
          onConnectingResult!();
        } else if (arg["status"] == SSTPConnectionStatusKeys.DISCONNECTED) {
          onDisconnectedResult!();

          bool? error = arg["error"];
          if (error != null && error) onError!();
        }
      } else if (call.method == 'downloadSpeed') {
        downloadTraffic = double.parse(call.arguments);
        traffic = ConnectionTraffic(
            downloadTraffic: downloadTraffic, uploadTraffic: uploadTraffic);
        onConnectedResult!(traffic);
      } else if (call.method == 'uploadSpeed') {
        uploadTraffic = double.parse(call.arguments);
        traffic = ConnectionTraffic(
            downloadTraffic: downloadTraffic, uploadTraffic: uploadTraffic);
        onConnectedResult!(traffic);
      }
    }

    channel.setMethodCallHandler(methodCallReceiver);
  }

  /// Tries to take vpn permission
  /// If user decline the permission , it will appears again when use [connectVpn] until the permission will be granted
  Future takePermission() async {
    try {
      await channelHandler.vpnPermission();
    } catch (e) {
      print(e);
      rethrow;
    }
  }

  /// Starts connection between client and provided server config in [saveServerData]
  /// Before try to connect , make sure you have saved your server config using [saveServerData]
  Future connectVpn() async {
    try {
      var caller = await channelHandler.connectVpn();
      return caller;
    } on PlatformException catch (e) {
      print(e);
      rethrow;
    }
  }

  /// Disconnects current running sstp connection
  Future disconnect() async {
    try {
      await channelHandler.disconnect();
    } catch (e) {
      rethrow;
    }
  }

  /// Returns all installed apps on user's device
  Future<List<InstalledAppInfo>> getInstalledApps() async {
    List<InstalledAppInfo> apps = await channelHandler.getInstalledApps();
    return apps;
  }

  /// Returns all allowed apps package name
  /// These packages are allowed to get tunneled , except that they're not getting tunneled
  Future<List<String>> getAllowedApps() async {
    List<String> packages = await channelHandler.getAllowedApps();
    return packages;
  }

  /// Adds provided List of package names to allowed apps to get tunneled
  /// you can simply get installed apps package names using [getInstalledApps]
  Future addToAllowedApps({required List<String> packages}) async {
    try {
      await channelHandler.addToAllowedApps(packages);
    } catch (e) {
      rethrow;
    }
  }

  /// Enables dns for provided connection
  /// Note : the provided dns will be use for next connection, not current one
  Future enableDns({required String dns}) async {
    try {
      await channelHandler.enableDNS(customDNS: dns);
    } catch (e) {
      rethrow;
    }
  }

  /// Disables dns for provided connection
  /// Note : dns will be disabled for next connection, not current one
  Future disableDNS() async {
    try {
      await channelHandler.disableDNS();
    } catch (e) {
      rethrow;
    }
  }

  /// Enables proxy for provided connection
  /// Note : the provided proxy will be use for next connection, not current one
  Future enableProxy({required SSTPProxy proxy}) async {
    try {
      await channelHandler.enableProxy(proxy: proxy);
    } catch (e) {
      rethrow;
    }
  }

  /// Disables proxy for provided connection
  /// Note : proxy will be disabled for next connection, not current one
  Future disableProxy() async {
    try {
      await channelHandler.disableProxy();
    } catch (e) {
      rethrow;
    }
  }

  /// Saves provided [SSTPServer] configuration.
  Future saveServerData({required SSTPServer server}) async {
    try {
      await channelHandler.saveServerData(server: server);
    } catch (e) {
      rethrow;
    }
  }

  /// Returns last connection status
  Future<SSTPConnectionStatusKeys> checkLastConnectionStatus() async {
    SSTPConnectionStatusKeys status =
        await channelHandler.checkLastConnectionStatus();
    return status;
  }

  /// Opens files and then returns selected directory path
  Future<String> addCertificate() async {
    String caller = await channelHandler.addCertificate();
    return caller;
  }

  static final SstpFlutter _instance = SstpFlutter.internal();
  factory SstpFlutter() => _instance;
  SstpFlutter.internal();
}
