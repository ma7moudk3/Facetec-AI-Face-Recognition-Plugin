import 'dart:developer';

import 'package:flutter/material.dart';
import 'dart:async';
import 'dart:io';
import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:facetec_flutter_plugin_demo/facetec_flutter_plugin_demo.dart';

void main() {
  runApp(MaterialApp(
    home: MyApp(),
  ));
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  String message = "";
  int messageCounter = 0;
  TextEditingController textFieldController = TextEditingController();
  bool initialized = false;
  File? idImageFile;
  String base64image = "";
  GlobalKey scaffoldKey = GlobalKey();

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  void addMessage(String msg, BuildContext context) {
    setState(() {
      messageCounter++;
      showToast(context, msg);
      message = messageCounter.toString() + ": " + msg + "\n" + message;
    });
  }

  void showToast(BuildContext context, String msg) {
    final scaffold = ScaffoldMessenger.of(scaffoldKey.currentContext!);
    scaffold.showSnackBar(
      SnackBar(
        content: Text(msg),
        action: SnackBarAction(
            label: 'OK', onPressed: scaffold.hideCurrentSnackBar),
      ),
    );
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      platformVersion = await FacetecFlutterPluginDemo.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  Future<void> displayTextInputDialog(
      BuildContext context, String title, Function onOk) async {
    return showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: Text(title),
            content: TextField(
              controller: textFieldController,
              decoration: InputDecoration(hintText: "Enter details"),
            ),
            actions: <Widget>[
              TextButton(
                style:TextButton.styleFrom(backgroundColor: Colors.red,
                primary: Colors.white),
                child: Text('CANCEL'),
                onPressed: () {
                  setState(() {
                    Navigator.pop(context);
                  });
                },
              ),
              TextButton(
                style:TextButton.styleFrom(backgroundColor: Colors.red,
                    primary: Colors.white),
                child: Text('OK'),
                onPressed: () {
                  setState(() {
                    onOk();
                    Navigator.pop(context);
                  });
                },
              ),
            ],
          );
        });
  }

  final ButtonStyle flatButtonStyle = TextButton.styleFrom(
    primary: Colors.white,
    shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(10)),
    backgroundColor: Colors.lightBlue,
  );

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: scaffoldKey,
      appBar: AppBar(
        title: const Text('FaceTec Plugin Demo'),
      ),
      body: Center(
          child: SingleChildScrollView(
        scrollDirection: Axis.vertical,
        child: ConstrainedBox(
          constraints:
              BoxConstraints(maxHeight: MediaQuery.of(context).size.height),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                mainAxisSize: MainAxisSize.max,
                children: [
                  Container(
                    margin: EdgeInsets.all(2),
                    child: TextButton(
                      child: Text(
                        'Initialize',
                        style: TextStyle(fontSize: 20.0),
                      ),
                      onPressed: () async {
                        String publicKey = "-----BEGIN PUBLIC KEY-----\n" +
                            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5PxZ3DLj+zP6T6HFgzzk\n" +
                            "M77LdzP3fojBoLasw7EfzvLMnJNUlyRb5m8e5QyyJxI+wRjsALHvFgLzGwxM8ehz\n" +
                            "DqqBZed+f4w33GgQXFZOS4AOvyPbALgCYoLehigLAbbCNTkeY5RDcmmSI/sbp+s6\n" +
                            "mAiAKKvCdIqe17bltZ/rfEoL3gPKEfLXeN549LTj3XBp0hvG4loQ6eC1E1tRzSkf\n" +
                            "GJD4GIVvR+j12gXAaftj3ahfYxioBH7F7HQxzmWkwDyn3bqU54eaiB7f0ftsPpWM\n" +
                            "ceUaqkL2DZUvgN0efEJjnWy5y1/Gkq5GGWCROI9XG/SwXJ30BbVUehTbVcD70+ZF\n" +
                            "8QIDAQAB\n" +
                            "-----END PUBLIC KEY-----";
                        String serverUrl =
                            "https://api.facetec.com/api/v3.1/biometrics";
                        String appToken = "Enter your FaceTec license key here";
                        String publicKeyMsg =
                            await FacetecFlutterPluginDemo.setPublicKey(
                                publicKey);
                        if (publicKeyMsg.startsWith("success")) {
                          addMessage(publicKeyMsg.substring(7), context);
                        } else {
                          addMessage(publicKeyMsg.substring(5), context);
                        }
                        String serverUrlMsg =
                            await FacetecFlutterPluginDemo.setServerUrl(
                                serverUrl);
                        if (serverUrlMsg.startsWith("success")) {
                          addMessage(serverUrlMsg.substring(7), context);
                        } else {
                          addMessage(serverUrlMsg.substring(5), context);
                        }
                        String initializeMsg =
                            await FacetecFlutterPluginDemo.initialize(
                                appToken, false, "","ar");
                        if (initializeMsg.startsWith("success")) {
                          initialized = true;
                          addMessage(initializeMsg.substring(7), context);
                        } else {
                          addMessage(initializeMsg.substring(5), context);
                        }
                      },
                      style: flatButtonStyle,
                    ),
                  ),
                  Container(
                    margin: EdgeInsets.all(2),
                    child: TextButton(
                      child: Text(
                        'Enroll',
                        style: TextStyle(fontSize: 20.0),
                      ),
                      onPressed: () {
                        if (initialized) {
                          displayTextInputDialog(context, "Enter user Id",
                              () async {
                            String enrollMsg =
                                await FacetecFlutterPluginDemo.enroll(
                                    textFieldController.text);
                            if (enrollMsg.startsWith("success")) {
                              addMessage(enrollMsg.substring(7), context);
                            } else {
                              addMessage(enrollMsg.substring(5), context);
                            }
                          });
                        } else {
                          showToast(context, "SDK not initialized");
                        }
                      },
                      style: flatButtonStyle,
                    ),
                  ),
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                mainAxisSize: MainAxisSize.max,
                children: [
                  Container(
                    margin: EdgeInsets.all(2),
                    child: TextButton(
                      child: Text(
                        'Authenticate',
                        style: TextStyle(fontSize: 20.0),
                      ),
                      onPressed: () {
                        if (initialized) {
                          displayTextInputDialog(context, "Enter user Id",
                              () async {
                            String enrollMsg =
                                await FacetecFlutterPluginDemo.authenticate(
                                    textFieldController.text);
                            if (enrollMsg.startsWith("success")) {
                              addMessage(enrollMsg.substring(7), context);
                            } else {
                              addMessage(enrollMsg.substring(5), context);
                            }
                          });
                        } else {
                          showToast(context, "SDK not initialized");
                        }
                      },
                      style: flatButtonStyle,
                    ),
                  ),
                  Container(
                    margin: EdgeInsets.all(2),
                    child: TextButton(
                      child: Text(
                        'Verify',
                        style: TextStyle(fontSize: 20.0),
                      ),
                      onPressed: () async {
                        if (initialized) {
                          String enrollMsg =
                              await FacetecFlutterPluginDemo.verify();
                          if (enrollMsg.startsWith("success")) {
                            addMessage(enrollMsg.substring(7), context);
                          } else {
                            addMessage(enrollMsg.substring(5), context);
                          }
                        } else {
                          showToast(context, "SDK not initialized");
                        }
                      },
                      style: flatButtonStyle,
                    ),
                  ),
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                mainAxisSize: MainAxisSize.max,
                children: [
                  Container(
                    margin: EdgeInsets.all(2),
                    child: TextButton(
                      child: Text(
                        'IDCheck',
                        style: TextStyle(fontSize: 20.0),
                      ),
                      onPressed: () {
                        if (initialized) {
                          showSimpleDialogIDCheck(context, false);

                        } else {
                          showToast(context, "SDK not initialized");
                        }
                      },
                      style: flatButtonStyle,
                    ),
                  ),
                  Container(
                    margin: EdgeInsets.all(2),
                    child: TextButton(
                      child: Text(
                        'Audit Trail',
                        style: TextStyle(fontSize: 20.0),
                      ),
                      onPressed: () async {
                        if (initialized) {
                          String enrollMsg =
                              await FacetecFlutterPluginDemo.auditTrail();
                          if (enrollMsg.startsWith("success")) {
                            await showDialog(
                                context: context,
                                builder: (_) => imageDialog('IDscanImage',
                                    enrollMsg.substring(7), context));
                          } else {
                            addMessage(enrollMsg.substring(5), context);
                          }
                        } else {
                          showToast(context, "SDK not initialized");
                        }
                      },
                      style: flatButtonStyle,
                    ),
                  ),
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                mainAxisSize: MainAxisSize.max,
                children: [
                  Container(
                    margin: EdgeInsets.all(2),
                    child: TextButton(
                      child: Text(
                        'ID Scan Images',
                        style: TextStyle(fontSize: 20.0),
                      ),
                      onPressed: () async {
                        if (initialized) {
                          String enrollMsg =
                              await FacetecFlutterPluginDemo.idScanImages();
                          if (enrollMsg.startsWith("success")) {
                            await showDialog(
                                context: context,
                                builder: (_) => imageDialog('IDscanImage',
                                    enrollMsg.substring(7), context));
                          } else {
                            addMessage(enrollMsg.substring(5), context);
                          }
                        } else {
                          showToast(context, "SDK not initialized");
                        }
                      },
                      style: flatButtonStyle,
                    ),
                  ),
                  Container(
                    margin: EdgeInsets.all(2),
                    child: TextButton(
                      child: Text(
                        'Set Theme',
                        style: TextStyle(fontSize: 20.0),
                      ),
                      onPressed: () {
                        showSimpleDialog(context);
                      },
                      style: flatButtonStyle,
                    ),
                  ),
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                mainAxisSize: MainAxisSize.max,
                children: [
                  Container(
                    margin: EdgeInsets.all(2),
                    child: TextButton(
                      child: Text(
                        'Get Version',
                        style: TextStyle(fontSize: 20.0),
                      ),
                      onPressed: () async {
                        String versionMsg =
                            await FacetecFlutterPluginDemo.getVersion();
                        if (versionMsg.startsWith("success")) {
                          addMessage(versionMsg.substring(7), context);
                        } else {
                          addMessage(versionMsg.substring(5), context);
                        }
                      },
                      style: flatButtonStyle,
                    ),
                  ),
                  Container(
                    margin: EdgeInsets.all(2),
                    child: TextButton(
                      child: Text(
                        'Get SDK Status',
                        style: TextStyle(fontSize: 20.0),
                      ),
                      onPressed: () async {
                        String statusMsg =
                            await FacetecFlutterPluginDemo.getSdkStatus();
                        if (statusMsg.startsWith("success")) {
                          addMessage(statusMsg.substring(7), context);
                        } else {
                          addMessage(statusMsg.substring(5), context);
                        }
                      },
                      style: flatButtonStyle,
                    ),
                  ),
                ],
              ),
              Container(
                margin: EdgeInsets.all(2),
                child: TextButton(
                  child: Text(
                    'Get User Enrollment Status',
                    style: TextStyle(fontSize: 20.0),
                  ),
                  onPressed: () {
                    if (initialized) {
                      displayTextInputDialog(context, "Enter user Id",
                          () async {
                        String enrollMsg = await FacetecFlutterPluginDemo
                            .getUserEnrollmentStatus(
                                textFieldController.text, "");
                        if (enrollMsg.startsWith("success")) {
                          addMessage(enrollMsg.substring(7), context);
                        } else {
                          addMessage(enrollMsg.substring(5), context);
                        }
                      });
                    } else {
                      showToast(context, "SDK not initialized");
                    }
                  },
                  style: flatButtonStyle,
                ),
              ),
              Container(
                margin: EdgeInsets.all(2),
                child: TextButton(
                  child: Text(
                    'Delete Enrollment',
                    style: TextStyle(fontSize: 20.0),
                  ),
                  onPressed: () async {
                    if (initialized) {
                      displayTextInputDialog(context, "Enter user Id",
                          () async {
                        String enrollMsg =
                            await FacetecFlutterPluginDemo.deleteEnrollment(
                                textFieldController.text, "");
                        if (enrollMsg.startsWith("success")) {
                          addMessage(enrollMsg.substring(7), context);
                        } else {
                          addMessage(enrollMsg.substring(5), context);
                        }
                      });
                    } else {
                      showToast(context, "SDK not initialized");
                    }
                  },
                  style: flatButtonStyle,
                ),
              ),
              Expanded(
                flex: 1,
                child: SingleChildScrollView(
                  scrollDirection: Axis.vertical,
                  child: Text(
                    message,
                  ),
                ),
              ),
            ],
          ),
        ),
      )),
    );
  }

  // show the dialog
  showSimpleDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return SimpleDialog(
          title: const Text('Choose an theme'),
          children: [
            SimpleDialogOption(
              child: const Text("FaceTec Theme"),
              onPressed: () {
                Navigator.of(context).pop();
                setTheme("FaceTec Theme");
              },
            ),
            SimpleDialogOption(
              child: const Text("Pseudo-Fullscreen"),
              onPressed: () {
                Navigator.of(context).pop();
                setTheme("Pseudo-Fullscreen");
              },
            ),
            SimpleDialogOption(
              child: const Text("Well-Rounded"),
              onPressed: () {
                Navigator.of(context).pop();
                setTheme("Well-Rounded");
              },
            ),
            SimpleDialogOption(
              child: const Text("Bitcoin Exchange"),
              onPressed: () {
                Navigator.of(context).pop();
                setTheme("Bitcoin Exchange");
              },
            ),
            SimpleDialogOption(
              child: const Text("eKYC"),
              onPressed: () {
                Navigator.of(context).pop();
                setTheme("eKYC");
              },
            ),
            SimpleDialogOption(
              child: const Text("Sample Bank"),
              onPressed: () {
                Navigator.of(context).pop();
                setTheme("Sample Bank");
              },
            )
          ],
        );
      },
    );
  }

  setTheme(String theme) {
    FacetecFlutterPluginDemo.setTheme(theme);
  }

  Image imageFromBase64String(String base64String) {
    return Image.memory(base64Decode(base64String));
  }

  Widget imageDialog(text, base64, context) {
    return Dialog(
      // backgroundColor: Colors.transparent,
      // elevation: 0,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Padding(
            padding: const EdgeInsets.only(left: 8.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '$text',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
                IconButton(
                  onPressed: () {
                    Navigator.of(context).pop();
                  },
                  icon: Icon(Icons.close_rounded),
                  color: Colors.redAccent,
                ),
              ],
            ),
          ),
          Container(
            width: 220,
            height: 200,
            child: imageFromBase64String(base64),
          ),
        ],
      ),
    );
  }

  showSimpleDialogIDCheck(BuildContext context, bool custom) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return SimpleDialog(
          title: const Text('Choose whether existing or new user'),
          children: [
            SimpleDialogOption(
              child: const Text("Existing User"),
              onPressed: () {
                Navigator.of(context).pop();
                if (custom) {
                } else {
                  displayTextInputDialog(context, "Enter user Id", () async {
                    String enrollMsg = await FacetecFlutterPluginDemo.idCheck(
                        textFieldController.text, false);
                    log("enrollMsg $enrollMsg");
                    if (enrollMsg.startsWith("success")) {
                      addMessage(enrollMsg.substring(7), context);
                    } else {
                      addMessage(enrollMsg.substring(5), context);
                    }
                  });
                }
              },
            ),
            SimpleDialogOption(
              child: const Text("New User"),
              onPressed: () {
                Navigator.of(context).pop();
                if (custom) {
                } else {
                  displayTextInputDialog(context, "Enter user Id", () async {
                    String enrollMsg = await FacetecFlutterPluginDemo.idCheck(
                        textFieldController.text, true);
                    if (enrollMsg.startsWith("success")) {
                      addMessage(enrollMsg.substring(7), context);
                    } else {
                      addMessage(enrollMsg.substring(5), context);
                    }
                  });
                }
              },
            ),
          ],
        );
      },
    );
  }
}
