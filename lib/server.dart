import 'package:sstp_flutter/android_configuration_sstp.dart';
import 'package:sstp_flutter/ios_configuration_sstp.dart';

class SSTPServer {
  final String host;
  final int port;
  final String username;
  final String password;
  final SSTPAndroidConfiguration androidConfiguration;
  final SSTPIOSConfiguration iosConfiguration;

  SSTPServer(
      {required this.host,
      this.port = 443,
      required this.username,
      required this.password,
      required this.androidConfiguration,
      required this.iosConfiguration});
}
