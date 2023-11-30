
# SstpFlutter

SstpFlutter is a Flutter plugin for SSTP VPN connections. It provides a convenient way to manage SSTP VPN connections, monitor connection status, and configure various settings.

## Features

- Connect to SSTP VPN server
- Monitor connection status
- Retrieve download and upload speed
- Enable and disable DNS
- Enable and disable proxy
- Save server data for quick connection
- Check the last connection status
- Get installed apps and manage allowed apps

## Getting Started

To use this plugin, add `sstp_flutter` as a dependency in your `pubspec.yaml` file.

```yaml
dependencies:
  sstp_flutter: ^1.0.3
```

Then, run `flutter pub get` to install the dependency.

## Example

```dart
import 'package:sstp_flutter/sstp_flutter.dart';

void main() async {
  SstpFlutter sstpFlutter = SstpFlutter();

  // Save server data
  await sstpFlutter.saveServerData(server: SSTPServer(host: 'example.com', username: 'user', password: 'password'));

  // Connect to SSTP VPN
  await sstpFlutter.connectVpn();

  // Monitor connection status
  sstpFlutter.onResult(
    onConnectedResult: (ConnectionTraffic traffic) {
      print('Connected - Download Traffic: ${traffic.downloadTraffic}, Upload Traffic: ${traffic.uploadTraffic}');
    },
    onConnectingResult: () {
      print('Connecting...');
    },
    onDisconnectedResult: () {
      print('Disconnected');
    },
    onError: () {
      print('Error occurred');
    },
  );

  // Take VPN permission
  await sstpFlutter.takePermission();

  // Disconnect from SSTP VPN
  await sstpFlutter.disconnect();

  // Get installed apps
  List<InstalledAppInfo> installedApps = await sstpFlutter.getInstalledApps();
  print('Installed Apps: $installedApps');

  // Get allowed apps
  List<String> allowedApps = await sstpFlutter.getAllowedApps();
  print('Allowed Apps: $allowedApps');

  // Add apps to allowed apps
  await sstpFlutter.addToAllowedApps(packages: ['com.example.app']);

  // Enable DNS
  await sstpFlutter.enableDns(DNS: '8.8.8.8');

  // Disable DNS
  await sstpFlutter.disableDNS();

  // Enable proxy
  await sstpFlutter.enableProxy(proxy: SSTPProxy(host: 'proxy.example.com', port: 8080));

  // Disable proxy
  await sstpFlutter.disableProxy();

  // Check last connection status
  UtilKeys status = await sstpFlutter.checkLastConnectionStatus();
  print('Last Connection Status: $status');
}
```

Please note that the plugin methods may throw exceptions (`PlatformException`). Handle them appropriately in your application.

## Contributions and Issues

Feel free to contribute to this project by submitting pull requests or reporting issues on the [GitHub repository](https://github.com/NavidShokoufeh/sstp_flutter).

This addition emphasizes that the purpose of the plugin is to provide a secure means for web surfing using SSTP VPN connections. Adjustments can be made based on your specific requirements.