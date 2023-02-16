@_spi(Restricted) import MapboxMaps
import Turf
import MapKit
import MapboxNavigation
import MapboxCoreNavigation
import MapboxDirections

struct NavigationInfo: Equatable {
    let distanceRemaining: Double
    let durationRemaining: Double

    static func == (lhs: NavigationInfo, rhs: NavigationInfo) -> Bool {
        abs(lhs.distanceRemaining - rhs.distanceRemaining) < 10 && abs(lhs.durationRemaining - rhs.durationRemaining) < 10
    }
}

protocol NavigationDelegate: AnyObject {
    func updateNavigationInfo(info: NavigationInfo)
    func didArrive()
    func showResumeButton(_ show: Bool)
}

@objc(RCTMGLMapNavigationView)
open class RCTMGLMapNavigationView : UIView {

    weak var navigationViewController: EmbeddedMapboxNavigationViewController?

  var reactOnShowResumeButton : RCTBubblingEventBlock?
  var reactOnDidArrive : RCTBubblingEventBlock?
  var reactOnUpdateNavigationInfo : RCTBubblingEventBlock?

    var reactFromLatitude : Double = 0
    var reactFromLongitude : Double = 0
    var reactToLatitude : Double = 0
    var reactToLongitude : Double = 0

    var hasLayout : Bool = false

  override public required init(frame:CGRect) {
      super.init(frame: frame)
  }

  public required init (coder: NSCoder) {
      fatalError("not implemented")
  }

    private func startNavigationIfAllCoordinatesSet() {
        if (reactFromLatitude != 0 && reactFromLongitude != 0 && reactToLatitude != 0 && reactToLongitude != 0) {
            //get directions
            setNeedsLayout()
        }
    }

  @objc public override func layoutSubviews() {
      super.layoutSubviews()

      if navigationViewController == nil {
          embed()
      } else {
          navigationViewController?.view.frame = bounds
      }

      hasLayout = true
  }

    private func embed() {
        guard let parentVC = parentViewController, reactFromLatitude != 0, reactFromLongitude != 0, reactToLatitude != 0, reactToLongitude != 0 else {
            return
        }

        let vc = EmbeddedMapboxNavigationViewController(delegate: self)

        parentVC.addChild(vc)
        addSubview(vc.view)
        vc.view.frame = bounds
        vc.didMove(toParent: parentVC)

        self.navigationViewController = vc

        vc.startEmbeddedNavigation(fromLatitude: self.reactFromLatitude, fromLongitude: self.reactFromLongitude, toLatitude: self.reactToLatitude, toLongitude: self.reactToLongitude)

    }

  public override func updateConstraints() {
      super.updateConstraints()
  }

    @objc func setReactFromLatitude(_ value: Double) {
        reactFromLatitude = value
        startNavigationIfAllCoordinatesSet()
    }

    @objc func setReactFromLongitude(_ value: Double) {
        reactFromLongitude = value
        startNavigationIfAllCoordinatesSet()
    }

    @objc func setReactToLatitude(_ value: Double) {
        reactToLatitude = value
        startNavigationIfAllCoordinatesSet()
    }

    @objc func setReactToLongitude(_ value: Double) {
        reactToLongitude = value
        startNavigationIfAllCoordinatesSet()
    }

}

extension RCTMGLMapNavigationView {
    @objc func recenter() {
        navigationViewController?.recenter()
    }
}

extension RCTMGLMapNavigationView: NavigationDelegate {
    func updateNavigationInfo(info: NavigationInfo) {
        let event = RCTMGLEvent(type: .navigationUpdateNavigationInfo, payload: [
            "distanceRemaining": info.distanceRemaining,
            "durationRemaining": info.durationRemaining,
        ])
        self.fireEvent(event: event, callback: reactOnUpdateNavigationInfo)
    }
    func didArrive() {
        let event = RCTMGLEvent(type: .navigationDidArrive, payload: [:])
        self.fireEvent(event: event, callback: reactOnDidArrive)
    }
    func showResumeButton(_ show: Bool) {
        let event = RCTMGLEvent(type: .navigationShowResumeButton, payload: ["showResumeButton": show])
        self.fireEvent(event: event, callback: reactOnShowResumeButton)
    }
}

extension RCTMGLMapNavigationView {

    private func fireEvent(event: RCTMGLEvent, callback: RCTBubblingEventBlock?) {
      guard let callback = callback else {
        Logger.log(level: .error, message: "fireEvent failed: \(event) - callback is null")
        return
      }
      fireEvent(event: event, callback: callback)
    }

    private func fireEvent(event: RCTMGLEvent, callback: @escaping RCTBubblingEventBlock) {
      callback(event.toJSON())
    }

    @objc func setReactOnShowResumeButton(_ value: @escaping RCTBubblingEventBlock) {
        self.reactOnShowResumeButton = value
    }

    @objc func setReactOnDidArrive(_ value: @escaping RCTBubblingEventBlock) {
        self.reactOnDidArrive = value
    }

    @objc func setReactOnUpdateNavigationInfo(_ value: @escaping RCTBubblingEventBlock) {
        self.reactOnUpdateNavigationInfo = value
    }
}

class EmbeddedMapboxNavigationViewController: UIViewController {

    weak var delegate: NavigationDelegate?

    weak var navigationViewController: NavigationViewController?

    init(delegate: NavigationDelegate?) {
        self.delegate = delegate
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        self.view.backgroundColor = UIColor.black

    }

    override var preferredStatusBarStyle: UIStatusBarStyle {
        .lightContent
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        UIKit.NotificationCenter.default.addObserver(self,
                                               selector: #selector(navigationCameraStateDidChange(_:)),
                                               name: .navigationCameraStateDidChange,
                                                     object: self.navigationViewController?.navigationMapView?.navigationCamera)

//        UIApplication.setStatusBarStyle(.lightContent)
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)

        UIKit.NotificationCenter.default.removeObserver(self,
                                                  name: .navigationCameraStateDidChange,
                                                  object: nil)
    }

    func startEmbeddedNavigation(fromLatitude: Double, fromLongitude: Double, toLatitude: Double, toLongitude: Double) {
        let routeOptions = NavigationRouteOptions(coordinates: [CLLocationCoordinate2DMake(fromLatitude, fromLongitude), CLLocationCoordinate2DMake(toLatitude, toLongitude)])
        Directions.shared.calculate(routeOptions) { [weak self] (_, result) in
            switch result {
            case .failure(let error):
                print(error.localizedDescription)
            case .success(let routeResponse):
                guard let `self` = self else { return }

                let topViewController: TopBannerViewController = .init()
                let navigationOptions = NavigationOptions(styles: [CustomDayStyle()], topBanner: topViewController, bottomBanner: CustomBottomBarViewController())
                var route: RouteOptions
                switch routeResponse.options {
                case .match(let match):
                    route = RouteOptions(matchOptions: match)
                case .route(let routeOptions):
                    route = routeOptions
                }
//                let controller = NavigationViewController(for: IndexedRouteResponse(routeResponse: routeResponse, routeIndex: 0), navigationOptions: navigationOptions)
                let controller = NavigationViewController(for: routeResponse, routeIndex: 0, routeOptions: route, navigationOptions: navigationOptions)
                controller.showsEndOfRouteFeedback = false
                controller.delegate = self
                controller.detailedFeedbackEnabled = false
                controller.automaticallyAdjustsStyleForTimeOfDay = false
                controller.floatingButtons = []
                self.addController(navigationViewController: controller)
            }
        }
    }

    func addController(navigationViewController: NavigationViewController) {
        self.children.forEach { viewController in
            viewController.removeFromParent()
        }
        self.view.subviews.forEach { view in
            view.removeFromSuperview()
        }
        self.navigationViewController = navigationViewController
        addChild(navigationViewController)
        self.view.addSubview(navigationViewController.view)
        navigationViewController.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            navigationViewController.view.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 0),
            navigationViewController.view.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: 0),
            navigationViewController.view.topAnchor.constraint(equalTo: view.topAnchor, constant: 0),
            navigationViewController.view.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: 0)
        ])
        self.didMove(toParent: self)
    }

    @objc func recenter() {
        self.navigationViewController?.topBanner(TopBannerViewController(), didDisplayStepsController: StepsViewController())

    }

    @objc func navigationCameraStateDidChange(_ notification: Notification) {
        guard let navigationCameraState = notification.userInfo?[NavigationCamera.NotificationUserInfoKey.state] as? NavigationCameraState else { return }

        switch navigationCameraState {
        case .transitionToFollowing, .following:
            delegate?.showResumeButton(false)
            break
        case .idle, .transitionToOverview, .overview:
            delegate?.showResumeButton(true)
            break
        }
    }
}

extension EmbeddedMapboxNavigationViewController: TopBannerViewControllerDelegate {
    func topBanner(_ banner: TopBannerViewController, didSwipeInDirection direction: UISwipeGestureRecognizer.Direction) {

    }
}

extension EmbeddedMapboxNavigationViewController: NavigationViewControllerDelegate {
    func navigationViewController(_ navigationViewController: NavigationViewController, shouldRerouteFrom location: CLLocation) -> Bool {
        return true
    }

    func navigationViewControllerDidDismiss(_ navigationViewController: NavigationViewController, byCanceling canceled: Bool) {
        navigationController?.popViewController(animated: true)
    }

    func navigationViewController(_ navigationViewController: NavigationViewController, didUpdate progress: RouteProgress, with location: CLLocation, rawLocation: CLLocation) {
        delegate?.updateNavigationInfo(info: NavigationInfo(distanceRemaining: progress.distanceRemaining, durationRemaining: progress.durationRemaining))
    }

    func navigationViewController(_ navigationViewController: NavigationViewController, didArriveAt waypoint: Waypoint) -> Bool {
        delegate?.didArrive()
        return true
    }
}


extension UIView {
    var parentViewController: UIViewController? {
        var parentResponder: UIResponder? = self
        while parentResponder != nil {
            parentResponder = parentResponder!.next
            if let viewController = parentResponder as? UIViewController {
                return viewController
            }
        }
        return nil
    }
}

class CustomDayStyle: NightStyle {

    required init() {
        super.init()
        mapStyleURL = URL(string: "mapbox://styles/gomothership/cl0cqi0hz005g14pfd7076cw9")!
        previewMapStyleURL = mapStyleURL
        styleType = .day
        statusBarStyle = .lightContent
    }

    open override func apply() {
        super.apply()

        let traitCollection = UIScreen.main.traitCollection

        TopBannerView.appearance(for: traitCollection).backgroundColor = .black
        BottomBannerView.appearance(for: traitCollection).backgroundColor = .black
        BottomPaddingView.appearance(for: traitCollection).backgroundColor = .black

        SpeedLimitView.appearance(for: traitCollection).signBackColor = .white

        InstructionsCardContainerView.appearance(for: traitCollection, whenContainedInInstancesOf: [InstructionsCardCell.self]).customBackgroundColor = .black
        InstructionsBannerView.appearance(for: traitCollection).backgroundColor = .black

        UserPuckCourseView.appearance(for: traitCollection).puckColor = #colorLiteral(red: 1, green: 1, blue: 1, alpha: 1)
        UserPuckCourseView.appearance(for: traitCollection).fillColor = #colorLiteral(red: 0, green: 0, blue: 0, alpha: 1)
        UserPuckCourseView.appearance(for: traitCollection).shadowColor = .clear

        InstructionsBannerView.appearance(for: traitCollection).backgroundColor = .black
        InstructionsCardContainerView.appearance(for: traitCollection, whenContainedInInstancesOf: [InstructionsCardCell.self]).customBackgroundColor = .black

        LaneView.appearance(for: traitCollection).primaryColor = .white
        LanesView.appearance(for: traitCollection).backgroundColor = .black
        LaneView.appearance(whenContainedInInstancesOf: [LanesView.self]).primaryColor = .white
        LanesView.appearance().backgroundColor = .black

        SeparatorView.appearance(for: traitCollection).backgroundColor = .black

        ManeuverView.appearance(for: traitCollection).backgroundColor = .clear
        ManeuverView.appearance(for: traitCollection, whenContainedInInstancesOf: [InstructionsBannerView.self]).primaryColor = .white
        ManeuverView.appearance(for: traitCollection, whenContainedInInstancesOf: [InstructionsBannerView.self]).secondaryColor = .white
        ManeuverView.appearance(for: traitCollection, whenContainedInInstancesOf: [NextBannerView.self]).primaryColor = .white
        ManeuverView.appearance(for: traitCollection, whenContainedInInstancesOf: [NextBannerView.self]).secondaryColor = .white
        ManeuverView.appearance(for: traitCollection, whenContainedInInstancesOf: [StepInstructionsView.self]).primaryColor = .white
        ManeuverView.appearance(for: traitCollection, whenContainedInInstancesOf: [StepInstructionsView.self]).secondaryColor = .white
        ManeuverView.appearance(for: traitCollection, whenContainedInInstancesOf: [InstructionsCardView.self]).primaryColor = .white
        ManeuverView.appearance(for: traitCollection, whenContainedInInstancesOf: [InstructionsCardView.self]).secondaryColor = .white
    }
}

class CustomTopBarViewController: TopBannerViewController {

}

class CustomBottomBarViewController: ContainerViewController {

    static let height: CGFloat = 18

    lazy var clearView: UIView = {
        let clearView = UIView()
        clearView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            clearView.heightAnchor.constraint(equalToConstant: CustomBottomBarViewController.height)
        ])
        return clearView
    }()

    override func loadView() {
        super.loadView()
        view.addSubview(clearView)
        let safeArea = view.layoutMarginsGuide
        NSLayoutConstraint.activate([
            clearView.leadingAnchor.constraint(equalTo: safeArea.leadingAnchor),
            clearView.trailingAnchor.constraint(equalTo: safeArea.trailingAnchor),
            clearView.heightAnchor.constraint(equalTo: view.heightAnchor),
            clearView.bottomAnchor.constraint(equalTo: safeArea.bottomAnchor)
        ])
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        setupConstraints()
    }

    private func setupConstraints() {
        if let superview = view.superview?.superview {
            view.bottomAnchor.constraint(equalTo: superview.safeAreaLayoutGuide.bottomAnchor).isActive = true
        }
    }

}
