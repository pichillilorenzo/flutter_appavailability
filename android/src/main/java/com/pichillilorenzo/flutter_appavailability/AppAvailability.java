package com.pichillilorenzo.flutter_appavailability;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.os.Build;
import android.annotation.TargetApi;

/** AppAvailability */
public class AppAvailability implements MethodCallHandler {

  private final Activity activity;
  private final Registrar registrar;

  private final int SYSTEM_APP_MASK = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

  public AppAvailability(Registrar registrar, Activity activity) {
    this.registrar = registrar;
    this.activity = activity;
  }

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.pichillilorenzo/flutter_appavailability");
    channel.setMethodCallHandler(new AppAvailability(registrar, registrar.activity()));
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    String uriSchema;
    switch (call.method) {
      case "checkAvailability":
        uriSchema = call.argument("uri").toString();
        this.checkAvailability( uriSchema, result );
        break;
      case "getInstalledApps":
        boolean systemApps = call.hasArgument("system_apps") && (Boolean) (call.argument("system_apps"));
        boolean onlyAppsWithLaunchIntent = call.hasArgument("only_with_launch_intent") && (Boolean) (call.argument("only_with_launch_intent"));
        result.success(getInstalledApps(systemApps, onlyAppsWithLaunchIntent));
        break;
      case "isAppEnabled":
        uriSchema = call.argument("uri").toString();
        this.isAppEnabled( uriSchema, result );
        break;
      case "launchApp":
        uriSchema = call.argument("uri").toString();
        this.launchApp(uriSchema , result);
        break;
      default:
        result.notImplemented();
    }
  }

  private void checkAvailability(String uri, Result result) {
    PackageInfo info = getAppPackageInfo(uri);

    if(info != null) {
      result.success(this.convertPackageInfoToJson(info));
      return;
    }
    result.error("", "App not found " + uri, null);
  }

  private List<Map<String, Object>> getInstalledApps(boolean includeSystemApps, boolean onlyAppsWithLaunchIntent) {
    PackageManager packageManager = registrar.context().getPackageManager();
    List<PackageInfo> apps = packageManager.getInstalledPackages(0);
    List<Map<String, Object>> installedApps = new ArrayList<>(apps.size());

    for (PackageInfo pInfo : apps) {
      if (!includeSystemApps && isSystemApp(pInfo)) {
        continue;
      }
      if (onlyAppsWithLaunchIntent && packageManager.getLaunchIntentForPackage(pInfo.packageName) == null) {
          continue;
      }
      Map<String, Object> map = this.convertPackageInfoToJson(pInfo);
      installedApps.add(map);
    }

    return installedApps;
  }

  private PackageInfo getAppPackageInfo(String uri) {
    Context ctx = activity.getApplicationContext();
    final PackageManager pm = ctx.getPackageManager();

    try {
      return pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
    }
    catch(PackageManager.NameNotFoundException e) {

    }

    return null;
  }

  private Map<String, Object> convertPackageInfoToJson(PackageInfo info) {
    PackageManager packageManager = registrar.context().getPackageManager();
    Map<String, Object> map = new HashMap<>();
    map.put("app_name", info.applicationInfo.loadLabel(packageManager).toString());
    map.put("package_name", info.packageName);
    map.put("version_code", String.valueOf(info.versionCode));
    map.put("version_name", info.versionName);
    map.put("data_dir", info.applicationInfo.dataDir);
    map.put("system_app", isSystemApp(info));
    map.put("launch_intent", packageManager.getLaunchIntentForPackage(info.packageName) != null);
    try {
      Drawable icon = packageManager.getApplicationIcon(info.packageName);
      String encodedImage = encodeToBase64(getBitmapFromDrawable(icon), Bitmap.CompressFormat.PNG, 100);
      map.put("app_icon", encodedImage);
    } catch (PackageManager.NameNotFoundException ignored) {
    }
    return map;
  }

  private boolean isSystemApp(PackageInfo pInfo) {
      return (pInfo.applicationInfo.flags & SYSTEM_APP_MASK) != 0;
  }

  private String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality) {
      ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
      image.compress(compressFormat, quality, byteArrayOS);
      return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.NO_WRAP);
  }

  private Bitmap getBitmapFromDrawable(Drawable drawable) {
      final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
      final Canvas canvas = new Canvas(bmp);
      drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
      drawable.draw(canvas);
      return bmp;
  }

  private void isAppEnabled(String packageName, Result result) {
    boolean appStatus = false;
    try {
      ApplicationInfo ai = registrar.context().getPackageManager().getApplicationInfo(packageName, 0);
      if (ai != null) {
        appStatus = ai.enabled;
      }
    } catch (PackageManager.NameNotFoundException e) {
      result.error("", e.getMessage() + " " + packageName, e);
      return;
    }
    result.success(appStatus);
  }

  @TargetApi(Build.VERSION_CODES.CUPCAKE)
  private void launchApp(String packageName, Result result) {
    PackageInfo info = getAppPackageInfo(packageName);

    if(info != null) {
      Intent launchIntent = registrar.context().getPackageManager().getLaunchIntentForPackage(packageName);
      if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        registrar.context().startActivity(launchIntent);
        result.success(null);
        return;
      }
    }

    result.error("", "App not found " + packageName, null);
  }
}
