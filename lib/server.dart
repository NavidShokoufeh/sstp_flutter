class SSTPServer {
  final String host;
  final int? port;
  final String username;
  final String password;
  final bool? verifyHostName;
  final bool? useTrustedCert;
  final bool? showDisconnectOnNotification;
  final String? notificationText;
  final String? sslVersion;

  SSTPServer(
      {required this.host,
      this.port,
      required this.username,
      required this.password,
      this.verifyHostName = false,
      this.useTrustedCert = false,
      this.sslVersion,
      this.showDisconnectOnNotification = false,
      this.notificationText = "Connected"
      });
}
