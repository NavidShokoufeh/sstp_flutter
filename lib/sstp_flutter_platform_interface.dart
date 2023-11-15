import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'sstp_flutter_method_channel.dart';

abstract class SstpFlutterPlatform extends PlatformInterface {
  /// Constructs a SstpFlutterPlatform.
  SstpFlutterPlatform() : super(token: _token);

  static final Object _token = Object();

  static SstpFlutterPlatform _instance = MethodChannelSstpFlutter();

  /// The default instance of [SstpFlutterPlatform] to use.
  ///
  /// Defaults to [MethodChannelSstpFlutter].
  static SstpFlutterPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [SstpFlutterPlatform] when
  /// they register themselves.
  static set instance(SstpFlutterPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
