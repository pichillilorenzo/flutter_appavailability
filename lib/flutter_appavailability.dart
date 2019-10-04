import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';

/// Main class of the plugin.
class AppAvailability {
  static const MethodChannel _channel =
      const MethodChannel('com.pichillilorenzo/flutter_appavailability');

  /// Check if an app is available with the given [uri] scheme.
  ///
  /// Returns a [Map<String, String>] containing info about the App or throws a [PlatformException]
  /// if the app isn't found.
  ///
  /// The returned [Map] has a form like this:
  /// ```dart
  /// {
  ///   "app_name": "",
  ///   "package_name": "",
  ///   "versionCode": "",
  ///   "version_name": ""
  /// }
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

  /// Only for **Android**.
  ///
  /// Get the list of all installed apps, where
  /// each app has a form like [checkAvailability()].
  static Future<List<Map<String, dynamic>>> getInstalledApps(
    { 
      bool addSystemApps: false,
      bool onlyAppsWithLaunchIntent: false
    }
  ) async {
    List<dynamic> apps = await _channel.invokeMethod("getInstalledApps", {
      'system_apps': addSystemApps,
      'only_with_launch_intent': onlyAppsWithLaunchIntent
    });
    if (apps != null && apps is List) {
      List<Map<String, dynamic>> list = new List();
      for (var app in apps) {
        if (app is Map) {
          list.add({
            "app_name": app["app_name"],
            "package_name": app["package_name"],
            "version_code": app["version_code"],
            "version_name": app["version_name"],
            "data_dir": app["data_dir"],
            "system_app": app["system_app"],
            "app_icon": app["app_icon"]
          });
        }
      }

      return list;
    }
    return new List(0);
  }

  /// Only for **Android**.
  ///
  /// Check if the app is enabled or not with the given [uri] scheme.
  ///
  /// If the app isn't found, then a [PlatformException] is thrown.
  static Future<bool> isAppEnabled(String uri) async {
    Map<String, dynamic> args = <String, dynamic>{};
    args.putIfAbsent('uri', () => uri);
    return await _channel.invokeMethod("isAppEnabled", args);
  }

  /// Launch an app with the given [uri] scheme if it exists.
  ///
  /// If the app app isn't found, then a [PlatformException] is thrown.
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