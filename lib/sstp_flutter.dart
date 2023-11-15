
import 'sstp_flutter_platform_interface.dart';

class SstpFlutter {
  Future<String?> getPlatformVersion() {
    return SstpFlutterPlatform.instance.getPlatformVersion();
  }
}
