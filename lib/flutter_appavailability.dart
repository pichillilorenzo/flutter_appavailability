import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';

class AppAvailability {
  static const MethodChannel _channel =
      const MethodChannel('com.pichillilorenzo/flutter_appavailability');

  static Future<Map<String, String>> checkAvailability(String uri) async {
    Map<String, dynamic> args = <String, dynamic>{};
    args.putIfAbsent('uri', () => uri);

    if (Platform.isAndroid) {
      Map<dynamic, dynamic> app = await _channel.invokeMethod("checkAvailability", args);
      return {
        "app_name": app["app_name"],
        "package_name": app["package_name"],
        "versionCode": app["versionCode"],
        "version_name": app["version_name"]
      };
    }
    else if (Platform.isIOS) {
      bool appAvailable = await _channel.invokeMethod("checkAvailability", args);
      if (!appAvailable) {
        throw PlatformException(code: "", message: "App not found $uri");
      }
      return {
        "app_name": "",
        "package_name": uri,
        "versionCode": "",
        "version_name": ""
      };
    }

    return null;
  }

  static Future<List<Map<String, String>>> getInstalledApps() async {
    List<dynamic> apps = await _channel.invokeMethod("getInstalledApps");
    if (apps != null && apps is List) {
      List<Map<String, String>> list = new List();
      for (var app in apps) {
        if (app is Map) {
          list.add({
            "app_name": app["app_name"],
            "package_name": app["package_name"],
            "versionCode": app["versionCode"],
            "version_name": app["version_name"]
          });
        }
      }

      return list;
    }
    return new List(0);
  }

  static Future<bool> isAppEnabled(String uri) async {
    Map<String, dynamic> args = <String, dynamic>{};
    args.putIfAbsent('uri', () => uri);
    return await _channel.invokeMethod("isAppEnabled", args);
  }

  static Future<void> launchApp(String uri) async {
    Map<String, dynamic> args = <String, dynamic>{};
    args.putIfAbsent('uri', () => uri);
    if (Platform.isAndroid) {
      await _channel.invokeMethod("launchApp", args);
    }
    else if (Platform.isIOS) {
      bool appAvailable = await _channel.invokeMethod("launchApp", args);
      if (!appAvailable) {
        throw PlatformException(code: "", message: "App not found $uri");
      }
    }

  }

}