package com.snapcommute.facetec_flutter_plugin_demo;

import androidx.annotation.NonNull;

import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.PluginRegistry;

import Processors.*;

import com.facetec.sdk.*;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * FacetecFlutterPluginDemoPlugin
 */
public class FacetecFlutterPluginDemoPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    private String zoomServerBaseURL = "https://api.facetec.com/api/v3.1/biometrics";
    private static String TAG = "FaceTec";
    // wrong key
    //private String licenseKey = "d1lThcmc6tJy4SjN8pBkAzy7ennbAwCP";
    // valid key
     private String licenseKey = "drDxEKEZbySGfPtev0xfuW3lYWyK5IHe";
    private String publicKey =
            "-----BEGIN PUBLIC KEY-----\n" +
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5PxZ3DLj+zP6T6HFgzzk\n" +
                    "M77LdzP3fojBoLasw7EfzvLMnJNUlyRb5m8e5QyyJxI+wRjsALHvFgLzGwxM8ehz\n" +
                    "DqqBZed+f4w33GgQXFZOS4AOvyPbALgCYoLehigLAbbCNTkeY5RDcmmSI/sbp+s6\n" +
                    "mAiAKKvCdIqe17bltZ/rfEoL3gPKEfLXeN549LTj3XBp0hvG4loQ6eC1E1tRzSkf\n" +
                    "GJD4GIVvR+j12gXAaftj3ahfYxioBH7F7HQxzmWkwDyn3bqU54eaiB7f0ftsPpWM\n" +
                    "ceUaqkL2DZUvgN0efEJjnWy5y1/Gkq5GGWCROI9XG/SwXJ30BbVUehTbVcD70+ZF\n" +
                    "8QIDAQAB\n" +
                    "-----END PUBLIC KEY-----";

    private String productionKeyText = "";
    private static Result pendingCallbackContext = null;
    private String errorMessages = "";
    private String langCode = "ar";
    private Context context;
    private Activity activity;
    public Processor latestProcessor;
    public String[] base64Image;
    public String customID;
    public boolean customIDCheck = false;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "facetec_flutter_plugin_demo");
        context = flutterPluginBinding.getApplicationContext();
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "initialize":
                if (call.arguments.toString().length() > 0) {
                    // licenseKey = call.argument("appToken");
                    boolean productionMode = Boolean.TRUE.equals(call.argument("productionMode"));
                    langCode = call.argument("langCode");
                    Log.d(TAG, "onMethodCall: langCode: " + langCode);
                    // Override application language with the selected locale
                    Locale locale = new Locale(langCode);
                    Configuration config = activity.getBaseContext().getResources().getConfiguration();
                    config.setLocale(locale);
                    // Update current activity's configuration
                    activity.getBaseContext().getResources().updateConfiguration(config, activity.getBaseContext().getResources().getDisplayMetrics());
                    // Update application's configuration so the FaceTec SDK will be updated
                    activity.getApplicationContext().getResources().updateConfiguration(config, activity.getBaseContext().getResources().getDisplayMetrics());
                    productionKeyText = call.argument("productionKeyText");
                    // Toast.makeText(context,"This is a Demo Version",Toast.LENGTH_LONG).show();
                    if (productionMode) {
                        FaceTecSDK.initializeInProductionMode(context, productionKeyText, licenseKey, publicKey, new FaceTecSDK.InitializeCallback() {
                            @Override
                            public void onCompletion(final boolean successful) {
                                if (successful) {
                                    Log.d(TAG, "Initialization Successful.");
                                    ThemeHelpers.setAppTheme(context, "Pseudo-Fullscreen",langCode);
                                    result.success("successinitialized");
                                } else {
                                    result.success("Initialization failed (Check Your Licence Key)");
                                }

                                // Displays the FaceTec SDK Status to text field.

                            }
                        });
                    } else {
                        FaceTecSDK.initializeInDevelopmentMode(context, licenseKey, publicKey, new FaceTecSDK.InitializeCallback() {

                            @Override
                            public void onCompletion(final boolean successful) {
                                if (successful) {
                                    Log.d(TAG, "Initialization Successful.");
                                    ThemeHelpers.setAppTheme(context, "Pseudo-Fullscreen",langCode);

                                    result.success("successinitialized");
                                } else {
                                    result.success("Initialization failed (Check Your Licence Key)");
                                }

                                // Displays the FaceTec SDK Status to text field.

                            }
                        });
                    }
                } else {
                    result.success("errorParameters for initialization not shared");
                }
                break;
            case "setPublicKey":
                Log.d(TAG, call.arguments.toString());
                if (call.arguments.toString().length() > 0) {
                    publicKey = call.arguments.toString();
                    //Config.DeviceKeyIdentifier=publicKey;
                    //ZoomGlobalState.PublicFaceMapEncryptionKey = publicKey;
                    result.success("successPublic Key Set");
                } else {
                    result.success("errorNo Public Key Shared");
                }
                break;
            case "setServerUrl":

                if (call.arguments.toString().length() > 0) {
                    zoomServerBaseURL = call.arguments.toString();
                    //Config.BaseURL=zoomServerBaseURL;
                    //ZoomGlobalState.ZoomServerBaseURL = zoomServerBaseURL;
                    result.success("successServer url Set");
                } else {
                    result.success("errorNo server url Shared");
                }

                break;
            case "enroll":
                result.success("errorFunctionality only available in full version of plugin. Please contact plugins@snapcommute.com for further details!");
                break;
            case "authenticate":
                result.success("errorFunctionality only available in full version of plugin. Please contact plugins@snapcommute.com for further details!");
                break;
            case "verify":
                getSessionToken(new SessionTokenCallback() {
                    @Override
                    public void onSessionTokenReceived(String sessionToken) {
                        pendingCallbackContext = new MethodResultWrapper(result);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("verify", "Callinf processor");
                                latestProcessor = new LivenessCheckProcessor(sessionToken, activity, licenseKey, zoomServerBaseURL, langCode);
                            }
                        });
                    }
                });
                break;
            case "idCheck":

                getSessionToken(new SessionTokenCallback() {
                    @Override
                    public void onSessionTokenReceived(String sessionToken) {
                        pendingCallbackContext = new MethodResultWrapper(result);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                latestProcessor = new PhotoIDMatchProcessor(sessionToken, activity, licenseKey, zoomServerBaseURL, langCode);

                            }
                        });
                    }
                });


                break;
            case "auditTrail":
                if (this.base64Image != null) {
                    if (base64Image.length > 0 && !base64Image[0].equals("")) {
                        result.success("success" + base64Image[0]);
                    } else {
                        result.success("errorNo audit trial images");
                    }
                } else {
                    result.success("errorNo audit trial images");
                }
                break;
            case "idScanImages":
                getSessionToken(new SessionTokenCallback() {
                    @Override
                    public void onSessionTokenReceived(String sessionToken) {
                        pendingCallbackContext = new MethodResultWrapper(result);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                latestProcessor = new PhotoIDScanProcessor(sessionToken, activity, licenseKey, zoomServerBaseURL, langCode);
                            }
                        });
                    }
                });


                break;
            case "setTheme":
                if (call.arguments.toString().length() > 0) {
                    ThemeHelpers.setAppTheme(context, call.arguments.toString(),langCode);
                    result.success("successTheme Set");
                } else {
                    result.success("errorNo server url Shared");
                }

                break;
            case "getVersion":
                result.success("success" + FaceTecSDK.version());
                break;
            case "getSdkStatus":
                result.success("success" + getSdkStatusString());
                break;
            case "getUserEnrollmentStatus":
                result.success("errorFunctionality only available in full version of plugin. Please contact plugins@snapcommute.com for further details!");
                break;
            case "deleteEnrollment":
                result.success("errorFunctionality only available in full version of plugin. Please contact plugins@snapcommute.com for further details!");
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }


    private String getSdkStatusString() {
        //Context context = this.cordova.getActivity();

        FaceTecSDKStatus status = FaceTecSDK.getStatus(context);
        switch (status) {
            case NEVER_INITIALIZED:
                return "NeverInitialized";
            case INITIALIZED:
                return "Initialized";
            case VERSION_DEPRECATED:
                return "VersionDeprecated";
            case NETWORK_ISSUES:
                return "NetworkIssues";
            case DEVICE_NOT_SUPPORTED:
                return "DeviceNotSupported";
            case DEVICE_IN_LANDSCAPE_MODE:
                return "DeviceInLandscapeMode";
            case DEVICE_IN_REVERSE_PORTRAIT_MODE:
                return "DeviceInReversePortraitMode";
            case DEVICE_LOCKED_OUT:
                return "DeviceLockedOut";
      /*case LICENSE_EXPIRED_OR_INVALID:
        return "LicenseExpiredOrInvalid";
      case INVALID_DEVICE_LICENSE_KEY_IDENTIFIER:
        return "InvalidLicenseKey";
      case GRACE_PERIOD_EXCEEDED:
        return "GracePreroidExceeded";*/
            case ENCRYPTION_KEY_INVALID:
                return "EncryptionKeyInvalid";
        }
        return "UnknownStatus";
    }

    interface SessionTokenCallback {
        void onSessionTokenReceived(String sessionToken);
    }

    public void getSessionToken(final SessionTokenCallback sessionTokenCallback) {


        // Do the network call and handle result
        okhttp3.Request request = new okhttp3.Request.Builder()
                .header("X-Device-Key", licenseKey)
                .header("User-Agent", FaceTecSDK.createFaceTecAPIUserAgentString(""))
                .url(zoomServerBaseURL + "/session-token")
                .get()
                .build();

        NetworkingHelpers.getApiClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Exception raised while attempting HTTPS call.");

                // If this comes from HTTPS cancel call, don't set the sub code to NETWORK_ERROR.
                if (!e.getMessage().equals(NetworkingHelpers.OK_HTTP_RESPONSE_CANCELED)) {
                    //utils.handleErrorGettingServerSessionToken();
                }
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                String responseString = response.body().string();
                response.body().close();
                try {
                    JSONObject responseJSON = new JSONObject(responseString);
                    if (responseJSON.has("sessionToken")) {
                        //utils.hideSessionTokenConnectionText();
                        sessionTokenCallback.onSessionTokenReceived(responseJSON.getString("sessionToken"));
                    } else {
                        //utils.handleErrorGettingServerSessionToken();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "Exception raised while attempting to parse JSON result.");
                    //utils.handleErrorGettingServerSessionToken();
                }
            }
        });
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("onActivityResult", "received result");
        if (latestProcessor == null) {
            Log.d("onActivityResult", "latestProcessor null");
            return false;
        }

        //utils.displayStatus("See logs for more details.");
        //utils.fadeInMainUI();

        // At this point, you have already handled all results in your Processor code.
        if (!this.latestProcessor.isSuccess()) {
            // Reset the enrollment identifier.
            //String latestExternalDatabaseRefID = "";
            Log.d("isSuccess", "not isSuccess");
            //Log.d("isSuccess",this.latestProcessor.errorMessage);
            if (!this.latestProcessor.getErrorMessage().equals("")) {
                pendingCallbackContext.success("error" + this.latestProcessor.getErrorMessage());
            }
//            else {
//                pendingCallbackContext.success("errorresult error");
//            }
        } else {
            if (!customIDCheck) {
                base64Image = latestProcessor.getBase64Images();
                if (latestProcessor.getDocumentData().equals("")) {
                    JSONObject scannedDocumentDataResult = new JSONObject();

                    try {
                        scannedDocumentDataResult.put("externalDatabaseRefID",
                                latestProcessor.getLastExternalRefId()
                        );
                        scannedDocumentDataResult.put(
                                "documentData",
                                latestProcessor.getDocumentData()
                        );
                        if (latestProcessor.getBase64Images().length >= 1) {
                            scannedDocumentDataResult.put(
                                    "frontIdImage",
                                    latestProcessor.getBase64Images()[0]
                            );
                        }
                        if (latestProcessor.getBase64Images().length >= 2) {
                            scannedDocumentDataResult.put(
                                    "backIdImage",
                                    latestProcessor.getBase64Images()[1]
                            );
                        }
                        if (latestProcessor.getBase64Images().length >= 3) {
                            scannedDocumentDataResult.put(
                                    "selfieImage",
                                    latestProcessor.getBase64Images()[2]
                            );
                        }
                        if (latestProcessor.getBase64Images().length >= 4) {
                            scannedDocumentDataResult.put(
                                    "faceScan",
                                    latestProcessor.getBase64Images()[3]
                            );
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    customIDCheck = true;
                    latestProcessor = null;
                    pendingCallbackContext.success(scannedDocumentDataResult.toString());

                } else {
                    JSONObject scannedDocumentDataResult = new JSONObject();
                    try {
                        scannedDocumentDataResult.put("externalDatabaseRefID",
                                latestProcessor.getLastExternalRefId()
                        );
                        scannedDocumentDataResult.put(
                                "documentData",
                                latestProcessor.getDocumentData()
                        );
                        if (latestProcessor.getBase64Images().length >= 1) {
                            scannedDocumentDataResult.put(
                                    "frontIdImage",
                                    latestProcessor.getBase64Images()[0]
                            );
                        }
                        if (latestProcessor.getBase64Images().length >= 2) {
                            scannedDocumentDataResult.put(
                                    "backIdImage",
                                    latestProcessor.getBase64Images()[1]
                            );
                        }
                        if (latestProcessor.getBase64Images().length >= 3) {
                            scannedDocumentDataResult.put(
                                    "selfieImage",
                                    latestProcessor.getBase64Images()[2]
                            );
                        }
                        if (latestProcessor.getBase64Images().length >= 4) {
                            scannedDocumentDataResult.put(
                                    "faceScan",
                                    latestProcessor.getBase64Images()[3]
                            );
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    customIDCheck = true;
                    latestProcessor = null;
                    pendingCallbackContext.success(scannedDocumentDataResult.toString());
                }
            } else {
                JSONObject parameters = new JSONObject();
                try {
                    parameters.put("image0", customID);
                    parameters.put("image1", latestProcessor.getBase64Images()[1]);
                    parameters.put("minMatchLevel", 3);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to create JSON payload for upload.");
                }
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(zoomServerBaseURL + "/match-2d-2d")
                        .header("Content-Type", "application/json")
                        .header("X-Device-Key", licenseKey)

                        //
                        // Part 7:  Demonstrates updating the Progress Bar based on the progress event.
                        //
                        .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), parameters.toString()))
                        .build();
                NetworkingHelpers.getApiClient().newCall(request).enqueue(new Callback() {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {

                        //
                        // Part 6:  In our Sample, we evaluate a boolean response and treat true as was successfully processed and should proceed to next step,
                        // and handle all other responses by cancelling out.
                        // You may have different paradigms in your own API and are free to customize based on these.
                        //

                        String responseString = response.body().string();
                        Log.v("responseString", responseString);

                        response.body().close();
                        try {
                            JSONObject responseJSON = new JSONObject(responseString);
                            Log.v("responseJSON", responseJSON.toString());

                            boolean wasProcessed = responseJSON.getBoolean("success");
                            Log.v("responseJSON", responseJSON.toString());

                            //String status = responseJSON.getString("status");
                            //result.success("successstatus");
                            // In v9.2.0+, we key off a new property called wasProcessed to determine if we successfully processed the Session result on the Server.
                            // Device SDK UI flow is now driven by the proceedToNextStep function, which should receive the scanResultBlob from the Server SDK response.
                            if (wasProcessed) {
                                base64Image = latestProcessor.getBase64Images();
                                pendingCallbackContext.success("successmatchLevel:" + responseJSON.getInt("matchLevel") + "documentData:" + latestProcessor.getDocumentData());
                                // Demonstrates dynamically setting the Success Screen Message.
                                //FaceTecCustomization.overrideResultScreenSuccessMessage = "Authenticated";

                                // In v9.2.0+, simply pass in scanResultBlob to the proceedToNextStep function to advance the User flow.
                                // scanResultBlob is a proprietary, encrypted blob that controls the logic for what happens next for the User.
                                //success = faceScanResultCallback.proceedToNextStep(scanResultBlob);
                            } else {
                                pendingCallbackContext.success("error Not processed (UNEXPECTED response from API)");
                                // CASE:  UNEXPECTED response from API.  Our Sample Code keys off a wasProcessed boolean on the root of the JSON object --> You define your own API contracts with yourself and may choose to do something different here based on the error.
                                //faceScanResultCallback.cancel();
                            }
                        } catch (JSONException e) {
                            // CASE:  Parsing the response into JSON failed --> You define your own API contracts with yourself and may choose to do something different here based on the error.  Solid server-side code should ensure you don't get to this case.
                            e.printStackTrace();
                            Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to parse JSON result.");
                            //faceScanResultCallback.cancel();
                            pendingCallbackContext.success("json parsing exception (Parsing the response into JSON failed)");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call call, IOException e) {
                        // CASE:  Network Request itself is erroring --> You define your own API contracts with yourself and may choose to do something different here based on the error.
                        Log.d("FaceTecSDKSampleApp", "Exception raised while attempting HTTPS call.");
                        //faceScanResultCallback.cancel();
                        pendingCallbackContext.success("errorHTTP call error (Network Request itself is erroring)");
                    }
                });
            }
        }
        return true;
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding activityPluginBinding) {
        activity = activityPluginBinding.getActivity();
        activityPluginBinding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding activityPluginBinding) {
        activity = activityPluginBinding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {

    }

    private static class MethodResultWrapper implements MethodChannel.Result {
        private final MethodChannel.Result methodResult;
        private final Handler handler;

        MethodResultWrapper(final MethodChannel.Result result) {
            this.methodResult = result;
            this.handler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void success(final Object result) {
            Log.d(TAG, "Resulttt:: " + result.toString());

            this.handler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            MethodResultWrapper.this.methodResult.success(result);
                        }
                    });
        }

        @Override
        public void error(
                final String errorCode, final String errorMessage, final Object errorDetails) {
            this.handler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            MethodResultWrapper.this.methodResult.error(errorCode, errorMessage, errorDetails);
                        }
                    });
        }

        @Override
        public void notImplemented() {
            this.handler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            MethodResultWrapper.this.methodResult.notImplemented();
                        }
                    });
        }
    }

}
