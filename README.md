# SstpFlutter

SstpFlutter is a Flutter plugin for SSTP VPN connections. It provides a convenient way to manage SSTP VPN connections, monitor connection status, and configure various settings.

## Features

- Connect to SSTP VPN server
- Monitor connection status and duration
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
  sstp_flutter: ^version
```

Then, run `flutter pub get` to install the dependency.

## Example

```dart
import 'package:sstp_flutter/sstp_flutter.dart';

void main() async {
  SstpFlutter sstpFlutter = SstpFlutter();
  var cert_dir = "";

  // Take VPN permission
  await sstpFlutter.takePermission();
  
  // Create an SSTP server object
  SSTPServer server = SSTPServer(
    host: 'example.com',
    port: 443,
    username: 'test.user',
    password: 'test.pass',
    verifyHostName: false,
    useTrustedCert: false,
    verifySSLCert: false,
    sslVersion: SSLVersions.TLSv1_1,
    showDisconnectOnNotification: true,
    notificationText: "Notification Text Holder",
    );
  
  // Save created SSTP server
  await sstpFlutter.saveServerData(server: server);

  // Opens files and then returns selected directory path
  certDir = await sstpFlutterPlugin.addCertificate();

  // Connect to SSTP VPN
  await sstpFlutter.connectVpn();

  // Monitor connection status
  sstpFlutterPlugin.onResult(
   onConnectedResult: (ConnectionTraffic traffic, Duration duration) {
    setState(() {
      connectionTimer = duration;
      connectionStatus = "connected";
      downSpeed = traffic.downloadTraffic ?? 0;
      upSpeed = traffic.uploadTraffic ?? 0;
      });
    },
   onConnectingResult: () {
    debugPrint("onConnectingResult");
    setState(() {å
     connectionStatus = "connecting";
     });
    },
   onDisconnectedResult: () {
    debugPrint("onDisconnectedResult");
    setState(() {
     connectionStatus = "disconnected";
     downSpeed = 0;
     upSpeed = 0;
     });
    },
   onError: () {});


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

## Support this Project

If you find this project helpful, consider supporting it by making a donation. Major of Your contribution will spend on charity every month.

[!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://www.buymeacoffee.com/navidshokoufeh)

[!["زرین پال"](https://cdn.zarinpal.com/badges/trustLogo/1.png)](https://zarinp.al/navid_shokoufeh)

- **Bitcoin (BTC):** `bc1qgwfqm5e3fhyw879ycy23zljcxl2pvs575c3j7w`
- **Ethereum (ETH):** `0x7Db7D431B170bCC9D1DF005226dd2434Df51e470`

Thank you for your support! 🚀//zarinp.al/navid_shokoufeh