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

## iOS Setup

### <b>1. Add Capabillity</b>
Add <b>Network Extensions</b> capabillity on Runner's Target and enable <b>Packet Tunnel</b>

<img src ='https://github.com/NavidShokoufeh/sstp_flutter/blob/main/example/sc/1.png?raw=true'>

### <b>2. Add New Target</b>

Click + button on bottom left, Choose <b>NETWORK EXTENSION</b>. And set <b>Language</b> and <b>Provider  Type</b> to <b>Objective-C</b> and <b>Packet Tunnel</b> as image below.

<img src ='https://github.com/NavidShokoufeh/sstp_flutter/blob/main/example/sc/2.png?raw=true'>

### <b>3. Add Capabillity to sstp_extension</b>

Repeat the step 1 for new target you created on previous step (sstp_extension)

### <b>4. Add Framework Search Path</b>

Select sstp_extension and add the following lines to your <b>Build Setting</b> > <b>Framework Search Path</b>:

```
$(SRCROOT)/.symlinks/plugins/sstp_flutter/ios/ext
```
```
$(SRCROOT)/.symlinks/plugins/sstp_flutter/ios/openconnect
```

### <b>5. Copy Paste</b>

Open sstp_extension > PacketTunnelProvider.m and copy paste this script <a href="https://raw.githubusercontent.com/NavidShokoufeh/sstp_flutter/refs/heads/main/example/ios/sstp_extension/PacketTunnelProvider.m">PacketTunnelProvider.m</a>


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
   host: hostNameController.text,
   port: int.parse(sslPortController.text),
   username: userNameController.text,
   password: passController.text,
   androidConfiguration: SSTPAndroidConfiguration(
   verifyHostName: false,
   useTrustedCert: false,
   verifySSLCert: false,
   sslVersion: SSLVersions.TLSv1_1,
   showDisconnectOnNotification: true,
   notificationText: "Notification Text Holder",
    ),
   iosConfiguration: SSTPIOSConfiguration(
   enableMSCHAP2: true,
   enableCHAP: false,
   enablePAP: false,
   enableTLS: false,
    ),
   );
  
  // Save created SSTP server
  await sstpFlutter.saveServerData(server: server);

  // Opens files and then returns selected directory path (Android only)
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

  // Get installed apps (Android only)
  List<InstalledAppInfo> installedApps = await sstpFlutter.getInstalledApps();
  print('Installed Apps: $installedApps');

  // Get allowed apps (Android only)
  List<String> allowedApps = await sstpFlutter.getAllowedApps();
  print('Allowed Apps: $allowedApps');

  // Add apps to allowed apps (Android only)
  await sstpFlutter.addToAllowedApps(packages: ['com.example.app']);

  // Enable DNS (Android only)
  await sstpFlutter.enableDns(DNS: '8.8.8.8');

  // Disable DNS (Android only)
  await sstpFlutter.disableDNS();

  // Enable proxy (Android only)
  await sstpFlutter.enableProxy(proxy: SSTPProxy(host: 'proxy.example.com', port: 8080));

  // Disable proxy (Android only)
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
- **USDT (TRC20):** `TJc5v4ktoFaG3WamjY5rvSZy7v2F6tFuuE` 

Thank you for your support! 🚀