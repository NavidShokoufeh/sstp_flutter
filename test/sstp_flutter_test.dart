import 'package:flutter_test/flutter_test.dart';
import 'package:sstp_flutter/sstp_flutter.dart';
import 'package:sstp_flutter/sstp_flutter_platform_interface.dart';
import 'package:sstp_flutter/sstp_flutter_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockSstpFlutterPlatform
    with MockPlatformInterfaceMixin
    implements SstpFlutterPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final SstpFlutterPlatform initialPlatform = SstpFlutterPlatform.instance;

  test('$MethodChannelSstpFlutter is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelSstpFlutter>());
  });

  test('getPlatformVersion', () async {
    SstpFlutter sstpFlutterPlugin = SstpFlutter();
    MockSstpFlutterPlatform fakePlatform = MockSstpFlutterPlatform();
    SstpFlutterPlatform.instance = fakePlatform;

    expect(await sstpFlutterPlugin.getPlatformVersion(), '42');
  });
}
