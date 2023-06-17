import Flutter
import UIKit
import FaceTecSDK

public class SwiftFacetecFlutterPluginDemoPlugin: NSObject, FlutterPlugin,ProcessorDelegate,URLSessionDelegate {
    var publicKey:String!
    var licenseKeyIdentifier:String!
    var errorMessages:String=""
    var requestInProgress: Bool = false
    var pendingResult:FlutterResult!
    var serverUrl:String!
    var latestProcessor: Processor!
    var imageBase64:[String]=["","",""]
    var customIDCheck:Bool = false
    var customID:String!
    var latestNetworkRequest: URLSessionTask!
    var registrar: FlutterPluginRegistrar? = nil
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "facetec_flutter_plugin_demo", binaryMessenger: registrar.messenger())
        let instance = SwiftFacetecFlutterPluginDemoPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
        instance.registrar=registrar
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if (call.method == "getPlatformVersion") {
            result("iOS " + UIDevice.current.systemVersion)
        }
        else if (call.method == "initialize") {
            let args = call.arguments as? Dictionary<String, AnyObject>
            self.licenseKeyIdentifier=args?["appToken"] as? String ?? ""
            let productionMode=args?["productionMode"] as? Bool ?? false
            let productionKeyText=args?["productionKeyText"] as? String ?? ""
            Toast(Title: "Notice!", Text: "This is a demo version", delay: 5)
            if(!productionMode){
                print(licenseKeyIdentifier)
                FaceTec.sdk.initializeInDevelopmentMode(deviceKeyIdentifier: licenseKeyIdentifier, faceScanEncryptionKey: publicKey, completion: { initializationSuccessful in
                    if(initializationSuccessful){
                        ThemeHelpers.setAppTheme(theme: "Config Wizard Theme",registrar:self.registrar)
                        result("successInitialized")
                    }else{
                        result("errorInitialization Error")
                    }
                })
            }else{
                print("productionMode")
                FaceTec.sdk.initializeInProductionMode(productionKeyText: productionKeyText, deviceKeyIdentifier: licenseKeyIdentifier, faceScanEncryptionKey: publicKey,completion: {initializationSuccessful in
                    if(initializationSuccessful){
                        result("successInitialized")
                    }else{
                        result("errorInitialization Error")
                    }
                    
                })
                
            }
        }else if (call.method == "setPublicKey") {
            self.publicKey = call.arguments as? String ?? ""
            print(self.publicKey)
            result("successPublic key set")
        }
        else if (call.method == "setServerUrl") {
            self.serverUrl = call.arguments as? String ?? ""
            print(self.serverUrl)
            result("successServer url set")
            
        }
        else if (call.method == "enroll") {
            result("errorFunctionality only available in full version of plugin. Please contact plugins@snapcommute.com for further details!");
        }
        else if (call.method == "authenticate") {
            result("errorFunctionality only available in full version of plugin. Please contact plugins@snapcommute.com for further details!");
        }
        else if (call.method == "verify") {
            pendingResult = result
            getSessionToken() { sessionToken in
                self.latestProcessor = LivenessCheckProcessor(sessionToken: sessionToken, fromViewController: UIApplication.shared.keyWindow?.rootViewController,baseURL:self.serverUrl,licenseKey:self.licenseKeyIdentifier,delegate:self)
            }
        }
        else if (call.method == "idCheck") {
            result("errorFunctionality only available in full version of plugin. Please contact plugins@snapcommute.com for further details!");
        }
        else if (call.method == "auditTrail") {
            if(imageBase64[0] != ""){
                result("success\(imageBase64[0])")
            }else{
                result("errorNo audit trial images")
            }
        }
        else if (call.method == "idScanImages") {
            result("errorFunctionality only available in full version of plugin. Please contact plugins@snapcommute.com for further details!");
        }
        else if (call.method == "setTheme") {
            var theme = call.arguments as? String ?? ""
            ThemeHelpers.setAppTheme(theme: theme,registrar:self.registrar)
        }
        else if (call.method == "getVersion") {
            result("success"+FaceTec.sdk.version)
        }
        else if (call.method == "getSdkStatus") {
            let status=FaceTec.sdk.getStatus()
            var msg=""
            switch status {
            case FaceTecSDKStatus.neverInitialized:
                msg="NeverInitialized"
                break
            case FaceTecSDKStatus.initialized:
                msg="Initialized"
                break
            case FaceTecSDKStatus.networkIssues:
                msg="NetworkIssues"
                break
            case FaceTecSDKStatus.invalidDeviceKeyIdentifier:
                msg="InvalidLicenseKey"
                break
            case FaceTecSDKStatus.versionDeprecated:
                msg="VersionDeprecated"
                break
            case FaceTecSDKStatus.offlineSessionsExceeded:
                msg="SessionExceeded"
                break
            case FaceTecSDKStatus.unknownError:
                msg="UnknownStatus"
                break
            case FaceTecSDKStatus.deviceLockedOut:
                msg="DeviceLockedOut"
                break
            case FaceTecSDKStatus.deviceInLandscapeMode:
                msg="DeviceInLandscapeMode"
                break
            case FaceTecSDKStatus.deviceInReversePortraitMode:
                msg="DeviceInReversePortraitMode"
                break
            case FaceTecSDKStatus.keyExpiredOrInvalid:
                msg="LicenseExpiredOrInvalid"
                break
            case FaceTecSDKStatus.encryptionKeyInvalid:
                msg="EncryptionKeyInvalid"
                break
            default:
                msg="UnknownStatus"
            }
            result("success"+msg);
        }
        else if (call.method == "getUserEnrollmentStatus") {
            result("errorFunctionality only available in full version of plugin. Please contact plugins@snapcommute.com for further details!");
        }
        else if (call.method == "deleteEnrollment") {
            result("errorFunctionality only available in full version of plugin. Please contact plugins@snapcommute.com for further details!");
        }
        else {
            result("errorFunction not implemented");
        }
    }
    
    func getSessionToken(sessionTokenCallback: @escaping (String) -> ()) {
        //utils.startSessionTokenConnectionTextTimer();
        
        let endpoint = serverUrl + "/session-token"
        let request = NSMutableURLRequest(url: NSURL(string: endpoint)! as URL)
        request.httpMethod = "GET"
        // Required parameters to interact with the FaceTec Managed Testing API.
        request.addValue(licenseKeyIdentifier, forHTTPHeaderField: "X-Device-Key")
        request.addValue(FaceTec.sdk.createFaceTecAPIUserAgentString(""), forHTTPHeaderField: "User-Agent")
        
        let session = URLSession(configuration: URLSessionConfiguration.default, delegate: self, delegateQueue: OperationQueue.main)
        let task = session.dataTask(with: request as URLRequest, completionHandler: { data, response, error in
            // Ensure the data object is not nil otherwise callback with empty dictionary.
            guard let data = data else {
                print("Exception raised while attempting HTTPS call.")
                //self.utils.handleErrorGettingServerSessionToken()
                return
            }
            if let responseJSONObj = try? JSONSerialization.jsonObject(with: data, options: JSONSerialization.ReadingOptions.allowFragments) as! [String: AnyObject] {
                if((responseJSONObj["sessionToken"] as? String) != nil)
                {
                    //self.utils.hideSessionTokenConnectionText()
                    sessionTokenCallback(responseJSONObj["sessionToken"] as! String)
                    return
                }
                else {
                    print("Exception raised while attempting HTTPS call.")
                    //self.utils.handleErrorGettingServerSessionToken()
                }
            }
        })
        task.resume()
    }
    
    func onProcessingComplete(isSuccess: Bool, faceTecSessionResult: FaceTecSessionResult?,errorMsg:String?){
        if(isSuccess){
            imageBase64[0]=faceTecSessionResult!.auditTrailCompressedBase64![0] as! String ?? ""
            imageBase64[1]=""
            imageBase64[2]=""
            pendingResult("successprocess completed")
        }else{
            if(errorMsg == ""){
                pendingResult("errorresult error")
            }else{
                pendingResult("error"+errorMsg!)
            }
        }
        
    }
    func onProcessingIDScanComplete(isSuccess: Bool, faceTecSessionResult: FaceTecSessionResult?,idScanSessionResult:FaceTecIDScanResult?,errorMsg:String?,documentData:String?){
        
        
    }
    func Toast(Title:String ,Text:String, delay:Int) -> Void {
        let alert = UIAlertController(title: Title, message: Text, preferredStyle: .alert)
        if(UIApplication.shared.keyWindow?.rootViewController != nil){
            UIApplication.shared.keyWindow?.rootViewController!.present(alert, animated: true)
        }
        let deadlineTime = DispatchTime.now() + .seconds(delay)
        DispatchQueue.main.asyncAfter(deadline: deadlineTime, execute: {
            alert.dismiss(animated: true, completion: nil)
        })
    }
}
