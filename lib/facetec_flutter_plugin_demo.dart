import 'dart:async';
import 'dart:developer';

import 'package:flutter/services.dart';

class FacetecFlutterPluginDemo {
  static const MethodChannel _channel =
      MethodChannel('facetec_flutter_plugin_demo');

  /// shows the platform version
  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  /// This function is used to set the FaceTec provided public key
  static Future<String> setPublicKey(String publicKey) async {
    final String result =
        await _channel.invokeMethod('setPublicKey', publicKey);
    return result;
  }

  /// This function is used to provide the url to your server hosting FaceTec Server SDK
  static Future<String> setServerUrl(String serverUrl) async {
    final String result =
        await _channel.invokeMethod('setServerUrl', serverUrl);
    return result;
  }

  /// This function needs to be called before using any FaceTec SDK functionality
  static Future<String> initialize({
    required String licenseKey,
    required bool productionMode,
    required String productionKeyText,
    required String langCode,
    required String apiKey,
  }) async {
    final String result = await _channel.invokeMethod('initialize', {
      'licenseKey': licenseKey,
      'productionMode': productionMode,
      'productionKeyText': productionKeyText,
      "langCode": langCode,
      "apiKey": apiKey
    });
    log("initialize call back");
    log(result);
    return result;
  }

  /// This function is for enrolling a new user
  static Future<String> enroll(String userId) async {
    final String result = await _channel.invokeMethod('enroll', userId);
    return result;
  }

  /// This function is for authenticating an enrolled user
  static Future<String> authenticate(String userId) async {
    final String result = await _channel.invokeMethod('authenticate', userId);
    return result;
  }

  /// This function is for doing a liveness check
  static Future<String> verify() async {
    final String result = await _channel.invokeMethod('verify');
    return result;
  }

  /// This function is used for doing a ID Check along with face authentication
  static Future<String> idCheck(String userId, bool newUser) async {
    final String result = await _channel
        .invokeMethod('idCheck', {"userId": userId, "newUser": newUser});
    return result;
  }

  /// This function returns the latest audit trail image
  static Future<String> auditTrail() async {
    final String result = await _channel.invokeMethod('auditTrail');
    return result;
  }

  /// This function returns the latest ID Check image
  static Future<String> idScanImages() async {
    final String result = await _channel.invokeMethod('idScanImages');
    return result;
  }

  /// Through this function you can change the theme of the FaceTec SDK
  static Future<String> setTheme(String theme) async {
    final String result = await _channel.invokeMethod('setTheme', theme);
    return result;
  }

  static Future<String> getVersion() async {
    final String result = await _channel.invokeMethod('getVersion');
    return result;
  }

  static Future<String> getSdkStatus() async {
    final String result = await _channel.invokeMethod('getSdkStatus');
    return result;
  }

  static Future<String> getUserEnrollmentStatus(
      String userId, String customEnrolmentStatusEndPoint) async {
    final String result = await _channel.invokeMethod(
        'getUserEnrollmentStatus', {
      "userId": userId,
      "customEnrolmentStatusEndPoint": customEnrolmentStatusEndPoint
    });
    return result;
  }

  static Future<String> deleteEnrollment(
      String userId, String customDeleteEnrolmentEndPoint) async {
    final String result = await _channel.invokeMethod('deleteEnrollment', {
      "userId": userId,
      "customDeleteEnrolmentEndPoint": customDeleteEnrolmentEndPoint
    });
    return result;
  }
}
