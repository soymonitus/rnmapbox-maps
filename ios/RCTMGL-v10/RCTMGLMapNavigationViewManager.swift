import MapboxMaps
import MapboxNavigation
import MapboxCoreNavigation
import MapboxDirections

@objc(RCTMGLMapNavigationViewManager)
class RCTMGLMapNavigationViewManager: RCTViewManager {
    @objc
    override static func requiresMainQueueSetup() -> Bool {
        return true
    }

    func defaultFrame() -> CGRect {
        return UIScreen.main.bounds
    }

    override func view() -> UIView! {
        let result = RCTMGLMapNavigationView(frame: self.defaultFrame())
        return result
    }
}

// MARK: helpers

extension RCTMGLMapNavigationViewManager {
    func withMapNavigationView(
        _ reactTag: NSNumber,
        name: String,
        rejecter: @escaping RCTPromiseRejectBlock,
        fn: @escaping (_: RCTMGLMapNavigationView) -> Void) -> Void
    {
      self.bridge.uiManager.addUIBlock { (manager, viewRegistry) in
        let view = viewRegistry![reactTag]

        guard let view = view, let view = view as? RCTMGLMapNavigationView else {
          RCTMGLLogError("Invalid react tag, could not find RCTMGLMapNavigationView");
          rejecter(name, "Unknown find reactTag: \(reactTag)", nil)
          return;
        }

        fn(view)
      }
    }
}

// MARK: - react methods

extension RCTMGLMapNavigationViewManager {
    @objc
    func setVoiceMuted(_ reactTag: NSNumber,
                voiceMuted: Bool,
                  resolver: @escaping RCTPromiseResolveBlock,
                  rejecter: @escaping RCTPromiseRejectBlock
    ) -> Void {
      withMapNavigationView(reactTag, name:"setVoiceMuted", rejecter: rejecter) { view in
        NavigationSettings.shared.voiceMuted = voiceMuted
        resolver(nil)
      }
    }

    @objc
    func recenter(_ reactTag: NSNumber,
                  resolver: @escaping RCTPromiseResolveBlock,
                  rejecter: @escaping RCTPromiseRejectBlock
    ) -> Void {
      withMapNavigationView(reactTag, name:"recenter", rejecter: rejecter) { view in
        view.recenter()
        resolver(nil)
      }
    }

    @objc
    func isVoiceMuted(_ reactTag: NSNumber,
                  resolver: @escaping RCTPromiseResolveBlock,
                  rejecter: @escaping RCTPromiseRejectBlock
    ) -> Void {
      withMapNavigationView(reactTag, name:"isVoiceMuted", rejecter: rejecter) { view in
          resolver(["isVoiceMuted": NavigationSettings.shared.voiceMuted])
      }
    }
}
