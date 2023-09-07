//
// Welcome to the annotated FaceTec Device SDK core code for performing secure Enrollment!
//
package Processors;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facetec.sdk.FaceTecCustomization;
import com.facetec.sdk.FaceTecIDScanProcessor;
import com.facetec.sdk.FaceTecIDScanResult;
import com.facetec.sdk.FaceTecIDScanResultCallback;
import com.facetec.sdk.FaceTecIDScanStatus;
import com.facetec.sdk.FaceTecSDK;
import com.facetec.sdk.FaceTecSessionActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;

// This is an example self-contained class to perform Photo ID Scan with the FaceTec SDK.
// You may choose to further componentize parts of this in your own Apps based on your specific requirements.

// Android Note 1:  Some commented "Parts" below are out of order so that they can match iOS and Browser source for this same file on those platforms.
// Android Note 2:  Android does not have a onFaceTecSDKCompletelyDone function that you must implement like "Part 10" of iOS and Android Samples.  Instead, onActivityResult is used as the place in code you get control back from the FaceTec SDK.
public class PhotoIDScanProcessor extends Processor implements FaceTecIDScanProcessor {
    private boolean success = false;
    private String baseUrl;
    private String deviceKeyId;
    private String[] base64Image;
    //private final FaceTecSessionActivity sampleAppActivity;
    public String errorMessage = "";
    public String documentData = "";
    public String langCode = "ar";

    public PhotoIDScanProcessor(String sessionToken, Context context, String deviceKeyId, String baseUrl, String langCode) {
        //this.sampleAppActivity = (FaceTecSessionActivity) context;
        this.deviceKeyId = deviceKeyId;
        this.baseUrl = baseUrl;
        this.langCode = langCode;
        this.base64Image = new String[]{"", "", ""};
        // In v9.2.2+, configure the messages that will be displayed to the User in each of the possible cases.
        // Based on the internal processing and decision logic about how the flow gets advanced, the FaceTec SDK will use the appropriate, configured message.
        FaceTecCustomization.setIDScanUploadMessageOverrides(
                langCode.equals("ar") ? "جاري رفع\n صورة الوثيقة\n مشفرة":"Uploading\nEncrypted\nID Scan", // Upload of ID front-side has started.
                langCode.equals("ar") ? "جاري الرفع \n الاتصال بالانترنت بطيء": "Still Uploading...\nSlow Connection", // Upload of ID front-side is still uploading to Server after an extended period of time.
                langCode.equals("ar") ? "تم الرفع بنجاح":"Upload Complete", // Upload of ID front-side to the Server is complete.
                langCode.equals("ar") ? "جاري معالجة صورة الوثيقة": "Processing ID Scan", // Upload of ID front-side is complete and we are waiting for the Server to finish processing and respond.
                langCode.equals("ar") ? "جاري رفع\nصورة الوثيقة الخلفية": "Uploading\nEncrypted\nBack of ID", // Upload of ID back-side has started.
                langCode.equals("ar") ?"جاري الرفع \n الاتصال بالانترنت بطيء":"Still Uploading...\nSlow Connection", // Upload of ID back-side is still uploading to Server after an extended period of time.
                langCode.equals("ar") ? "تم الرفع بنجاح":"Upload Complete", // Upload of ID back-side to Server is complete.
                langCode.equals("ar") ? "جاري معالجة صورة الوثيقة \nمن الأسفل": "Processing Back of ID", // Upload of ID back-side is complete and we are waiting for the Server to finish processing and respond.
                langCode.equals("ar") ? "جاري الرفع..\nمعلوماتك المؤكدة":"Uploading\nYour Confirmed Info", // Upload of User Confirmed Info has started.
                langCode.equals("ar") ?"جاري الرفع \n الاتصال بالانترنت بطيء":"Still Uploading...\nSlow Connection", // Upload of ID back-side is still uploading to Server after an extended period of time.
                langCode.equals("ar") ? "تم الرفع بنجاح":"Upload Complete", // Upload of ID back-side to Server is complete.
                langCode.equals("ar") ? "جاري المعالجة..":"Processing", // Upload of User Confirmed Info is complete and we are waiting for the Server to finish processing and respond.
                langCode.equals("ar") ? "جاري رفع بيانات الـ\nNFC":"Uploading Encrypted\nNFC Details", // Upload of NFC Details has started.
                langCode.equals("ar") ?"جاري الرفع \n الاتصال بالانترنت بطيء":"Still Uploading...\nSlow Connection", // Upload of ID back-side is still uploading to Server after an extended period of time.
                langCode.equals("ar") ?"تم الرفع بنجاح":"Upload Complete", // Upload of NFC Details to the Server is complete.
                langCode.equals("ar") ?"جاري معالجة بيانات\nNFC": "Processing\nNFC Details", // Upload of NFC Details is complete and we are waiting for the Server to finish processing and respond.
                langCode.equals("ar") ? "جاري رفع بيانات الوثيقة\nمشفرة":"Uploading Encrypted\nID Details", // Upload of ID Details has started.
                langCode.equals("ar") ?"جاري الرفع \n الاتصال بالانترنت بطيء":"Still Uploading...\nSlow Connection", // Upload of ID back-side is still uploading to Server after an extended period of time.
                langCode.equals("ar") ? "تم الرفع بنجاح":"Upload Complete", // Upload of ID back-side to Server is complete.
                langCode.equals("ar") ? "جاري المعالجة..":"Processing" // Upload of User Confirmed Info is complete and we are waiting for the Server to finish processing and respond.
        );


        //
        // Part 1:  Starting the FaceTec Photo ID Scan Session
        //
        // Required parameters:
        // - Context:  Unique for Android, a Context is passed in, which is required for the final onActivityResult function after the FaceTec SDK is done.
        // - PhotoIDScanProcessor:  A class that implements FaceTecIDScanProcessor, which handles the Photo ID Scan when the User completes a Session.  In this example, "self" implements the class.
        // - sessionToken:  A valid Session Token you just created by calling your API to get a Session Token from the Server SDK.
        //
        FaceTecSessionActivity.createAndLaunchSession(context, PhotoIDScanProcessor.this, sessionToken);
    }

    //
    // Part 2:  Handling the Result of an IDScan
    //
    public void processIDScanWhileFaceTecSDKWaits(final FaceTecIDScanResult idScanResult, final FaceTecIDScanResultCallback idScanResultCallback) {
        //
        // DEVELOPER NOTE:  These properties are for demonstration purposes only so the Sample App can get information about what is happening in the processor.
        // In the code in your own App, you can pass around signals, flags, intermediates, and results however you would like.
        //
        // sampleAppActivity.setLatestIDScanResult(idScanResult);

        //
        // Part 3:  Handles early exit scenarios where there is no IDScan to handle -- i.e. User Cancellation, Timeouts, etc.
        //
        if (idScanResult.getStatus() != FaceTecIDScanStatus.SUCCESS) {
            NetworkingHelpers.cancelPendingRequests();
            idScanResultCallback.cancel();
            return;
        }

        // IMPORTANT:  FaceTecSDK.FaceTecIDScanStatus.Success DOES NOT mean the IDScan was Successful.
        // It simply means the User completed the Session. You still need to perform the IDScan on your Servers.

        //
        // Part 4: Get essential data off the FaceTecIDScanResult
        //
        JSONObject parameters = new JSONObject();
        try {
            parameters.put("idScan", idScanResult.getIDScanBase64());

            ArrayList<String> frontImagesCompressedBase64 = idScanResult.getFrontImagesCompressedBase64();
            ArrayList<String> backImagesCompressedBase64 = idScanResult.getBackImagesCompressedBase64();
            if (frontImagesCompressedBase64.size() > 0) {
                parameters.put("idScanFrontImage", frontImagesCompressedBase64.get(0));
                base64Image[0] = frontImagesCompressedBase64.get(0);
            }
            if (backImagesCompressedBase64.size() > 0) {
                parameters.put("idScanBackImage", backImagesCompressedBase64.get(0));
                base64Image[1] = backImagesCompressedBase64.get(0);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to create JSON payload for upload.");
        }

        //
        // Part 5:  Make the Networking Call to Your Servers.  Below is just example code, you are free to customize based on how your own API works.
        //
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(baseUrl + "/idscan-only")
                .header("Content-Type", "application/json")
                .header("X-Device-Key", deviceKeyId)
                .header("User-Agent", FaceTecSDK.createFaceTecAPIUserAgentString(idScanResult.getSessionId()))
                .header("X-User-Agent", FaceTecSDK.createFaceTecAPIUserAgentString(idScanResult.getSessionId()))

                //
                // Part 6:  Demonstrates updating the Progress Bar based on the progress event.
                //
                .post(new ProgressRequestBody(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), parameters.toString()),
                        new ProgressRequestBody.Listener() {
                            @Override
                            public void onUploadProgressChanged(long bytesWritten, long totalBytes) {
                                final float uploadProgressPercent = ((float) bytesWritten) / ((float) totalBytes);
                                idScanResultCallback.uploadProgress(uploadProgressPercent);
                            }
                        }))
                .build();

        //
        // Part 7:  Actually send the request.
        //
        NetworkingHelpers.getApiClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {

                //
                // Part 8:  In our Sample, we evaluate a boolean response and treat true as was successfully processed and should proceed to next step,
                // and handle all other responses by cancelling out.
                // You may have different paradigms in your own API and are free to customize based on these.
                //

                String responseString = response.body().string();

                response.body().close();
                try {
                    JSONObject responseJSON = new JSONObject(responseString);
                    documentData = responseJSON.toString();
                    boolean wasProcessed = responseJSON.getBoolean("wasProcessed");
                    String scanResultBlob = responseJSON.getString("scanResultBlob");

                    // In v9.2.0+, we key off a new property called wasProcessed to determine if we successfully processed the Session result on the Server.
                    // Device SDK UI flow is now driven by the proceedToNextStep function, which should receive the scanResultBlob from the Server SDK response.
                    if (wasProcessed) {

                        // In v9.2.0+, configure the messages that will be displayed to the User in each of the possible cases.
                        // Based on the internal processing and decision logic about how the flow gets advanced, the FaceTec SDK will use the appropriate, configured message.
                        // Please note that this programmatic API overrides these same Strings that can also be set via our standard, non-programmatic Text Customization & Localization APIs.
                        FaceTecCustomization.setIDScanResultScreenMessageOverrides(
                                langCode.equals("ar") ? "تم معالجة الوثيقة بنجاح": "ID Scan Complete", // Successful scan of ID front-side (ID Types with no back-side).
                                langCode.equals("ar") ? "تم مسح الوثيقة\nمن الأعلى بنجاح": "Front of ID\nScanned", // Successful scan of ID front-side (ID Types that have a back-side).
                                langCode.equals("ar") ? "تم مسح الوثيقة\nمن الأعلى بنجاح": "Front of ID\nScanned", // Successful scan of ID front-side (ID Types that have a back-side).
                                langCode.equals("ar") ?"تم معالجة الوثيقة بنجاح":"ID Scan Complete", // Successful scan of the ID back-side (ID Types that do not have NFC).
                                langCode.equals("ar") ?"تم مسح الوثيقة\nمن الأسفل":"Back of ID\nScanned", // Successful scan of the ID back-side (ID Types that do have NFC).
                                langCode.equals("ar") ?"تم معالجة\nجواز السفر": "Passport Scan Complete", // Successful scan of a Passport that does not have NFC.
                                langCode.equals("ar") ?"تم مسح جواز السفر":"Passport Scanned", // Successful scan of a Passport that does have NFC.
                                langCode.equals("ar") ?"تم مسح صورة الوثيقة\nبنجاح": "Photo ID Scan\nComplete", // Successful upload of final IDScan containing User-Confirmed ID Text.
                                langCode.equals("ar") ?"تم مسح صورة الوثيقة":"ID Scan Complete", // Successful upload of the scanned NFC chip information.
                                langCode.equals("ar") ?"لم يتطابق الوجه مع الوثيقة\nبشكل كافي":"Face Didn't Match\nHighly Enough", // Case where a Retry is needed because the Face on the Photo ID did not Match the User's Face highly enough.
                                langCode.equals("ar") ?"بيانات الوثيقة غير ظاهرة بشكل\nكافي": "ID Document\nNot Fully Visible", // Case where a Retry is needed because a Full ID was not detected with high enough confidence.
                                langCode.equals("ar") ?"نص الوثيقة غير مقروء":"ID Text Not Legible", // Case where a Retry is needed because the OCR did not produce good enough results and the User should Retry with a better capture.
                                langCode.equals("ar") ?"نوع الوثيقة غير متطابق\nالرجاء المحاولة لاحقًا":"ID Type Mismatch\nPlease Try Again", // Case where there is likely no OCR Template installed for the document the User is attempting to scan.
                                langCode.equals("ar") ?"تم رفع بيانات\nالوثيقة":"ID Details\nUploaded" // Case where NFC Scan was skipped due to the user's interaction or an unexpected error.
                        );

                        // In v9.2.0+, simply pass in scanResultBlob to the proceedToNextStep function to advance the User flow.
                        // scanResultBlob is a proprietary, encrypted blob that controls the logic for what happens next for the User.
                        // Cases:
                        //   1.  User must re-scan the same side of the ID that they just tried.
                        //   2.  User succeeded in scanning the Front Side of the ID, there is no Back Side, and the User is now sent to the User OCR Confirmation UI.
                        //   3.  User succeeded in scanning the Front Side of the ID, there is a Back Side, and the User is sent to the Auto-Capture UI for the Back Side of their ID.
                        //   4.  User succeeded in scanning the Back Side of the ID, and the User is now sent to the User OCR Confirmation UI.
                        //   5.  The entire process is complete.  This occurs after sending up the final IDScan that contains the User OCR Data.
                        success = idScanResultCallback.proceedToNextStep(scanResultBlob);

                        if (responseJSON.has("errorMessage")) {
                            errorMessage = responseJSON.getString("errorMessage");
                            idScanResultCallback.cancel();
                        }
                    } else {
                        // CASE:  UNEXPECTED response from API.  Our Sample Code keys off a wasProcessed boolean on the root of the JSON object --> You define your own API contracts with yourself and may choose to do something different here based on the error.
                        idScanResultCallback.cancel();
                    }
                } catch (JSONException e) {
                    // CASE:  Parsing the response into JSON failed --> You define your own API contracts with yourself and may choose to do something different here based on the error.  Solid server-side code should ensure you don't get to this case.
                    e.printStackTrace();
                    Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to parse JSON result.");
                    idScanResultCallback.cancel();
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // CASE:  Network Request itself is erroring --> You define your own API contracts with yourself and may choose to do something different here based on the error.
                Log.d("FaceTecSDKSampleApp", "Exception raised while attempting HTTPS call.");
                idScanResultCallback.cancel();
            }
        });
    }

    public boolean isSuccess() {
        return this.success;
    }

    @Override
    public String[] getBase64Images() {
        return base64Image;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getDocumentData() {
        return documentData;
    }

    @Override
    public String getLastExternalRefId() {
        return "";
    }
}