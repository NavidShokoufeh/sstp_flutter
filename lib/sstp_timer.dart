import 'dart:async';

import 'package:shared_preferences/shared_preferences.dart';

class SSTPTimer {
  int _secondsElapsed = 0;
  late int _hours;
  late int _minutes;
  late int _seconds;
  late Timer _timer;

  startTimer(StreamController<Duration?> connectionTimerStream) {
    _timer = Timer.periodic(const Duration(seconds: 1), (Timer t) async {
      connectionTimerStream.sink.add(await connectionTimer().first);
    });
  }

  stopTimer(StreamController<Duration?> connectionTimerStream) {
    _timer.cancel();
    if (!connectionTimerStream.isClosed) {
      connectionTimerStream.sink.add(const Duration());
    }
    _secondsElapsed = 0;
  }

  Stream<Duration> connectionTimer() async* {
    Duration timer;
    _secondsElapsed++;
    _hours = _secondsElapsed ~/ 3600;
    _minutes = (_secondsElapsed % 3600) ~/ 60;
    _seconds = _secondsElapsed % 60;
    timer = Duration(hours: _hours, minutes: _minutes, seconds: _seconds);
    yield timer;
  }

  saveConnectionTime({
    String? day,
    String? hour,
    String? minute,
    String? second,
  }) async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    prefs.setString("StartConnectionDayTime", day ?? "0");
    prefs.setString("StartConnectionHourTime", hour ?? "00");
    prefs.setString("StartConnectionMinutesTime", minute ?? "00");
    prefs.setString("StartConnectionSecondTime", second ?? "00");
  }

  Future<void> measureConnectedTime() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();

    var hour = int.parse(prefs.getString("StartConnectionHourTime") ?? "00");
    var min = int.parse(prefs.getString("StartConnectionMinutesTime") ?? "00");
    var sec = int.parse(prefs.getString("StartConnectionSecondTime") ?? "00");
    var day = int.parse(prefs.getString("StartConnectionDayTime") ?? "0");

    var now = DateTime.now();
    int durationInSeconds = ((now.day - day) * 86.400.toInt()) +
        ((now.hour - hour) * 3600) +
        ((now.minute - min) * 60) +
        (now.second - sec);
    _secondsElapsed = durationInSeconds;
  }

  static final SSTPTimer _instance = SSTPTimer.internal();
  factory SSTPTimer() => _instance;
  SSTPTimer.internal();
}
