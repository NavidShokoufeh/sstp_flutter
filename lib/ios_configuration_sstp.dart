class SSTPIOSConfiguration {
  bool? enableTLS;
  bool? enablePAP;
  bool? enableCHAP;
  bool? enableMSCHAP2;

  SSTPIOSConfiguration(
      {this.enableTLS = false,
      this.enablePAP = false,
      this.enableCHAP = false,
      this.enableMSCHAP2 = true});
}
