class SSTPServer {
  final String host;
  final int? port;
  final String username;
  final String password;
  final bool? verifyHostName;

  SSTPServer({
    required this.host,
    this.port,
    required this.username,
    required this.password,
    this.verifyHostName = false
  });
}
