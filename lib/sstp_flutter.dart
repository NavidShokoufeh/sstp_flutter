import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:sstp_flutter/src/core/Utils/utils.dart';
import 'package:sstp_flutter/traffic.dart';
import 'package:sstp_flutter/app_info.dart';
import 'package:sstp_flutter/proxy.dart';
import 'package:sstp_flutter/server.dart';
import 'package:sstp_flutter/src/data/provider/sstp_flutter_method_channel.dart';

typedef OnConnected = Function(ConnectionTraffic traffic, Duration duration);
typedef OnConnecting = void Function();
typedef OnDisconnected = void Function();
typedef OnError = Function();

class SstpFlutter {
  Timer? _timer;
  int _downloadSpeed = 0;
  int _uploadSpeed = 0;
  int _totalDownload = 0;
  int _totalUpload = 0;
  MethodChannelSstpFlutter channelHandler = MethodChannelSstpFlutter();
  OnConnected? onConnectedResult;

  /// Gains result of current connection status
  /// [OnConnected] get invoked when [ConnectionTraffic] get updates
  Future onResult({
    OnConnected? onConnectedResult,
    OnConnecting? onConnectingResult,
    OnDisconnected? onDisconnectedResult,
    OnError? onError,
  }) async {
    this.onConnectedResult = onConnectedResult;
    MethodChannel channel = const MethodChannel("responseReceiver");

    Future methodCallReceiver(MethodCall call) async {
      var arg = call.arguments;

      if (call.method == 'connectResponse') {
        if (arg["status"] == SSTPConnectionStatusKeys.CONNECTED) {
          _timer = Timer.periodic(
            const Duration(seconds: 1),
            (timer) {
              onConnectedResult!(
                ConnectionTraffic(
                  totalDownloadTraffic: _totalDownload,
                  totalUploadTraffic: _totalUpload,
                  downloadTraffic: _downloadSpeed,
                  uploadTraffic: _uploadSpeed,
                ),
                Duration(seconds: timer.tick),
              );
              _uploadSpeed = 0;
              _downloadSpeed = 0;
            },
          );
          onConnectedResult!(ConnectionTraffic(), Duration.zero);
        } else if (arg["status"] == SSTPConnectionStatusKeys.CONNECTING) {
          _timer?.cancel();
          _timer = null;
          _downloadSpeed = 0;
          _uploadSpeed = 0;
          _totalDownload = 0;
          _totalUpload = 0;
          onConnectingResult!();
        } else if (arg["status"] == SSTPConnectionStatusKeys.DISCONNECTED) {
          _timer?.cancel();
          _timer = null;
          _downloadSpeed = 0;
          _uploadSpeed = 0;
          _totalDownload = 0;
          _totalUpload = 0;
          onDisconnectedResult!();
          bool? error = arg["error"];
          if (error != null && error) onError!();
        }
      } else if (call.method == 'downloadSpeed') {
        _downloadSpeed += call.arguments[0] as int;
        _totalDownload = call.arguments[1] as int;
      } else if (call.method == 'uploadSpeed') {
        _uploadSpeed += call.arguments[0] as int;
        _totalUpload = call.arguments[1] as int;
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
      debugPrint(e.toString());
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
      debugPrint(e.toString());
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
  ///
  /// These packages are allowed to get tunneled , except that they're not getting tunneled
  ///
  /// fixed bug reported in : [https://github.com/NavidShokoufeh/sstp_flutter/issues/8]
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
  Future<String> checkLastConnectionStatus() async {
    String status = await channelHandler.checkLastConnectionStatus();
    if (status.toString() == SSTPConnectionStatusKeys.CONNECTED) {
      _timer = Timer.periodic(
        const Duration(seconds: 1),
        (timer) {
          onConnectedResult?.call(
            ConnectionTraffic(
              totalDownloadTraffic: _totalDownload,
              totalUploadTraffic: _totalUpload,
              downloadTraffic: _downloadSpeed,
              uploadTraffic: _uploadSpeed,
            ),
            Duration(seconds: timer.tick),
          );
          _uploadSpeed = 0;
          _downloadSpeed = 0;
        },
      );
    } else {
      _timer?.cancel();
      _timer = null;
      _downloadSpeed = 0;
      _uploadSpeed = 0;
      _totalDownload = 0;
      _totalUpload = 0;
    }
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
