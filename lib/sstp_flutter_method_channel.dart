import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'sstp_flutter_platform_interface.dart';

/// An implementation of [SstpFlutterPlatform] that uses method channels.
class MethodChannelSstpFlutter extends SstpFlutterPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('sstp_flutter');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
