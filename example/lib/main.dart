import 'package:flutter/material.dart';
import 'package:sstp_flutter/server.dart';
import 'package:sstp_flutter/ssl_versions.dart';
import 'package:sstp_flutter/sstp_flutter.dart';
import 'package:sstp_flutter/traffic.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final sstpFlutterPlugin = SstpFlutter();
  var connectionStatus = "disconnected";
  var cert_dir = "none";
  var downSpeed = 0.0;
  var upSpeed = 0.0;

  TextEditingController hostNameController = TextEditingController();
  TextEditingController sslPortController = TextEditingController();
  TextEditingController userNameController = TextEditingController();
  TextEditingController passController = TextEditingController();

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
            child: Padding(
          padding: EdgeInsets.symmetric(horizontal: 20),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Text("connectionStatus : $connectionStatus"),
                  Text("download Speed : $downSpeed KBps"),
                  Text("upload Speed : $downSpeed KBps"),
                  Text("certificate dir : $cert_dir"),
                ],
              ),
              TextField(
                controller: hostNameController,
                decoration: InputDecoration(hintText: "host name"),
              ),
              TextField(
                controller: sslPortController,
                decoration: InputDecoration(hintText: "ssl port"),
              ),
              TextField(
                controller: userNameController,
                decoration: InputDecoration(hintText: "user name"),
              ),
              TextField(
                controller: passController,
                decoration: InputDecoration(hintText: "password"),
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  ElevatedButton(
                      onPressed: () async {
                        SSTPServer server = SSTPServer(
                            host: hostNameController.text,
                            port: int.parse(sslPortController.text),
                            username: userNameController.text,
                            password: passController.text,
                            verifyHostName: false,
                            useTrustedCert: true,
                            sslVersion: SSLVersions.TLSv1_1,
                            showDisconnectOnNotification: true,
                            notificationText: "Notification Text Holder");

                        try {
                          await sstpFlutterPlugin
                              .takePermission()
                              .then((value) async {
                            await sstpFlutterPlugin
                                .saveServerData(server: server)
                                .then((value) async {
                              await sstpFlutterPlugin.connectVpn();
                            });
                          });
                        } catch (e) {
                          print(e);
                        }

                        sstpFlutterPlugin.onResult(
                            onConnectedResult: (ConnectionTraffic traffic) {
                              setState(() {
                                connectionStatus = "connected";
                                downSpeed = traffic.downloadTraffic;
                                upSpeed = traffic.uploadTraffic;
                              });
                            },
                            onConnectingResult: () {
                              print("onConnectingResult");
                              setState(() {
                                connectionStatus = "connecting";
                              });
                            },
                            onDisconnectedResult: () {
                              print("onDisconnectedResult");
                              setState(() {
                                connectionStatus = "disconnected";
                                downSpeed = 0.0;
                                upSpeed = 0.0;
                              });
                            },
                            onError: () {});
                      },
                      child: Text("Connect")),
                  ElevatedButton(
                      onPressed: () async {
                        await sstpFlutterPlugin.disconnect();
                      },
                      child: Text("Disconnect"))
                ],
              ),
              ElevatedButton(
                  onPressed: () async {
                    cert_dir = await sstpFlutterPlugin.addCertificate();
                    setState(() {});
                  },
                  child: Text("Certificate"))
            ],
          ),
        )),
      ),
    );
  }
}
