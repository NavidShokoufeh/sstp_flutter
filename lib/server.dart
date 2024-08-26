import 'ssl_versions.dart';

class SSTPServer {
  final String host;
  final int port;
  final String username;
  final String password;
  final bool verifyHostName;
  final bool verifySSLCert;
  final bool useTrustedCert;
  final bool showDisconnectOnNotification;
  final String notificationText;
  final String sslVersion;

  SSTPServer({
    required this.host,
    this.port = 443,
    required this.username,
    required this.password,
    this.verifyHostName = false,
    this.verifySSLCert = false,
    this.useTrustedCert = false,
    this.sslVersion = SSLVersions.DEFAULT,
    this.showDisconnectOnNotification = false,
    this.notificationText = "Connected",
  });
}
