import 'dart:typed_data';

class InstalledAppInfo {
  String? name;
  Uint8List? icon;
  String? packageName;
  String? versionName;
  int? versionCode;

  InstalledAppInfo(
    this.name,
    this.icon,
    this.packageName,
    this.versionName,
    this.versionCode,
  );

  factory InstalledAppInfo.create(dynamic data) {
    return InstalledAppInfo(
      data["name"],
      data["icon"],
      data["package_name"],
      data["version_name"],
      data["version_code"],
    );
  }

  String getVersionInfo() {
    return "$versionName ($versionCode)";
  }
}
