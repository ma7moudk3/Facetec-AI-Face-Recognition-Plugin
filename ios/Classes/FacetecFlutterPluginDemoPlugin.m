#import "FacetecFlutterPluginDemoPlugin.h"
#if __has_include(<facetec_flutter_plugin_demo/facetec_flutter_plugin_demo-Swift.h>)
#import <facetec_flutter_plugin_demo/facetec_flutter_plugin_demo-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "facetec_flutter_plugin_demo-Swift.h"
#endif

@implementation FacetecFlutterPluginDemoPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    [SwiftFacetecFlutterPluginDemoPlugin registerWithRegistrar:registrar];
}
@end