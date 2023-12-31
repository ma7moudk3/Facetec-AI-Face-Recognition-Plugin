//
// Welcome to the annotated FaceTec Device SDK core code for performing secure Liveness Checks!
//
package Processors;

import android.content.Context;
import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

//import com.facetec.sampleapp.SampleAppActivity;
import com.facetec.sdk.*;

// This is an example self-contained class to perform Liveness Checks with the FaceTec SDK.
// You may choose to further componentize parts of this in your own Apps based on your specific requirements.

// Android Note 1:  Some commented "Parts" below are out of order so that they can match iOS and Browser source for this same file on those platforms.
// Android Note 2:  Android does not have a onFaceTecSDKCompletelyDone function that you must implement like "Part 10" of iOS and Android Samples.  Instead, onActivityResult is used as the place in code you get control back from the FaceTec SDK.
public class LivenessCheckProcessor extends Processor implements FaceTecFaceScanProcessor {
    private boolean success = false;
    //    final private SampleAppActivity sampleAppActivity;
    private String licenseKey;
    private String baseUrl;
    private String[] base64Image;
    public String errorMessage = "";
    public  String langCode = "ar";

    @Override
    public String[] getBase64Images() {
        return base64Image;
    }




    public LivenessCheckProcessor(String sessionToken, Context context, String licenseKey, String baseUrl,String langCode) {
        //this.sampleAppActivity = (SampleAppActivity) context;
        this.licenseKey = licenseKey;
        this.baseUrl = baseUrl;
        this.base64Image = new String[]{"", "", ""};
        this.langCode = langCode;

        //
        // Part 1:  Starting the FaceTec Session
        //
        // Required parameters:
        // - Context:  Unique for Android, a Context is passed in, which is required for the final onActivityResult function after the FaceTec SDK is done.
        // - FaceTecFaceScanProcessor:  A class that implements FaceTecFaceScanProcessor, which handles the FaceScan when the User completes a Session.  In this example, "self" implements the class.
        // - sessionToken:  A valid Session Token you just created by calling your API to get a Session Token from the Server SDK.
        //
        FaceTecSessionActivity.createAndLaunchSession(context, LivenessCheckProcessor.this, sessionToken);
    }

    //
    // Part 2:  Handling the Result of a FaceScan
    //
    public void processSessionWhileFaceTecSDKWaits(final FaceTecSessionResult sessionResult, final FaceTecFaceScanResultCallback faceScanResultCallback) {
        //
        // DEVELOPER NOTE:  These properties are for demonstration purposes only so the Sample App can get information about what is happening in the processor.
        // In the code in your own App, you can pass around signals, flags, intermediates, and results however you would like.
        //
        //sampleAppActivity.setLatestSessionResult(sessionResult);

        //
        // Part 3:  Handles early exit scenarios where there is no FaceScan to handle -- i.e. User Cancellation, Timeouts, etc.
        //
        Log.d("FaceScan Result", "FaceScan result received");
        if (sessionResult.getStatus() != FaceTecSessionStatus.SESSION_COMPLETED_SUCCESSFULLY) {
            NetworkingHelpers.cancelPendingRequests();
            faceScanResultCallback.cancel();
            return;
        }

        // IMPORTANT:  FaceTecSDK.FaceTecSessionStatus.SessionCompletedSuccessfully DOES NOT mean the Liveness Check was Successful.
        // It simply means the User completed the Session and a 3D FaceScan was created.  You still need to perform the Liveness Check on your Servers.
        base64Image[0] = sessionResult.getAuditTrailCompressedBase64()[0];
        //
        // Part 4:  Get essential data off the FaceTecSessionResult
        //
        JSONObject parameters = new JSONObject();
        try {
            parameters.put("faceScan", sessionResult.getFaceScanBase64());
            parameters.put("auditTrailImage", sessionResult.getAuditTrailCompressedBase64()[0]);
            parameters.put("lowQualityAuditTrailImage", sessionResult.getLowQualityAuditTrailCompressedBase64()[0]);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to create JSON payload for upload.");
        }

        //
        // Part 5:  Make the Networking Call to Your Servers.  Below is just example code, you are free to customize based on how your own API works.
        //
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(baseUrl + "/liveness-3d")
                .header("Content-Type", "application/json")
                .header("X-Device-Key", licenseKey)
                .header("User-Agent", FaceTecSDK.createFaceTecAPIUserAgentString(sessionResult.getSessionId()))

                //
                // Part 7:  Demonstrates updating the Progress Bar based on the progress event.
                //
                .post(new ProgressRequestBody(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), parameters.toString()),
                        new ProgressRequestBody.Listener() {
                            @Override
                            public void onUploadProgressChanged(long bytesWritten, long totalBytes) {

                                final float uploadProgressPercent = ((float) bytesWritten) / ((float) totalBytes);
                                Log.d("uploadProgressPercent", Float.toString(uploadProgressPercent));

                                faceScanResultCallback.uploadProgress(uploadProgressPercent);
                            }
                        }))
                .build();

        //
        // Part 8:  Actually send the request.
        //
        NetworkingHelpers.getApiClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {

                //
                // Part 6:  In our Sample, we evaluate a boolean response and treat true as was successfully processed and should proceed to next step,
                // and handle all other responses by cancelling out.
                // You may have different paradigms in your own API and are free to customize based on these.
                //

                String responseString = response.body().string();
                response.body().close();
                try {
                    JSONObject responseJSON = new JSONObject(responseString);
                    if (responseJSON.has("wasProcessed")) {
                        boolean wasProcessed = responseJSON.getBoolean("wasProcessed");
                        String scanResultBlob = responseJSON.getString("scanResultBlob");

                        // In v9.2.0+, we key off a new property called wasProcessed to determine if we successfully processed the Session result on the Server.
                        // Device SDK UI flow is now driven by the proceedToNextStep function, which should receive the scanResultBlob from the Server SDK response.
                        if (wasProcessed) {

                            // Demonstrates dynamically setting the Success Screen Message.
                            FaceTecCustomization.overrideResultScreenSuccessMessage = langCode.equals("ar") ? "تم تأكيد أنك لست\nروبوت" : "Liveness\nConfirmed";

                            // In v9.2.0+, simply pass in scanResultBlob to the proceedToNextStep function to advance the User flow.
                            // scanResultBlob is a proprietary, encrypted blob that controls the logic for what happens next for the User.
                            success = faceScanResultCallback.proceedToNextStep(scanResultBlob);
                            Log.d("FaceTec", String.valueOf(success));

                        } else {
                            // CASE:  UNEXPECTED response from API.  Our Sample Code keys off a wasProcessed boolean on the root of the JSON object --> You define your own API contracts with yourself and may choose to do something different here based on the error.
                            faceScanResultCallback.cancel();
                        }
                    } else {
                        if (responseJSON.has("errorMessage")) {
                            errorMessage = responseJSON.getString("errorMessage");
                            faceScanResultCallback.cancel();
                        }
                    }
                } catch (JSONException e) {
                    // CASE:  Parsing the response into JSON failed --> You define your own API contracts with yourself and may choose to do something different here based on the error.  Solid server-side code should ensure you don't get to this case.
                    e.printStackTrace();
                    Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to parse JSON result.");
                    faceScanResultCallback.cancel();
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @Nullable IOException e) {
                // CASE:  Network Request itself is erroring --> You define your own API contracts with yourself and may choose to do something different here based on the error.
                Log.d("FaceTecSDKSampleApp", "Exception raised while attempting HTTPS call.");
                faceScanResultCallback.cancel();
            }
        });
    }

    public boolean isSuccess() {

        Log.d("isSuccess", "in isSuccess");
        return this.success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getDocumentData(){ return "";}

    @Override
    public String getLastExternalRefId() {
        return "" ;

    }
}