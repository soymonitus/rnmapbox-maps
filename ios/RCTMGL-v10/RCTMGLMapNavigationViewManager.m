#import <React/RCTBridgeModule.h>
#import <React/RCTViewManager.h>

@interface RCT_EXTERN_REMAP_MODULE(RCTMGLMapNavigationView, RCTMGLMapNavigationViewManager, RCTViewManager)

RCT_REMAP_VIEW_PROPERTY(fromLatitude, reactFromLatitude, double)
RCT_REMAP_VIEW_PROPERTY(fromLongitude, reactFromLongitude, double)
RCT_REMAP_VIEW_PROPERTY(toLatitude, reactToLatitude, double)
RCT_REMAP_VIEW_PROPERTY(toLongitude, reactToLongitude, double)

RCT_REMAP_VIEW_PROPERTY(onShowResumeButton, reactOnShowResumeButton, RCTBubblingEventBlock)
RCT_REMAP_VIEW_PROPERTY(onDidArrive, reactOnDidArrive, RCTBubblingEventBlock)
RCT_REMAP_VIEW_PROPERTY(onUpdateNavigationInfo, reactOnUpdateNavigationInfo, RCTBubblingEventBlock)

RCT_EXTERN_METHOD(setVoiceMuted:(nonnull NSNumber *)reactTag
                  voiceMuted:(BOOL)voiceMuted
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(isVoiceMuted:(nonnull NSNumber *)reactTag
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(recenter:(nonnull NSNumber *)reactTag
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

@end
