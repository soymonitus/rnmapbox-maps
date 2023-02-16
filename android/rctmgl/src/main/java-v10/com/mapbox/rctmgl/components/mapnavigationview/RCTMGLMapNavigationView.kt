package com.mapbox.rctmgl.components.mapnavigationview

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewTreeLifecycleOwner
import com.facebook.react.bridge.*
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedInfoApi
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedInfoView
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.PercentDistanceTraveledFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.mapbox.maps.MapboxMap
import com.mapbox.rctmgl.events.AndroidCallbackEvent
import java.util.Date
import java.util.Locale
import com.mapbox.rctmgl.events.OnDidArriveEvent
import com.mapbox.rctmgl.events.OnShowResumeButtonEvent
import com.mapbox.rctmgl.events.OnUpdateNavigationInfoEvent
import androidx.constraintlayout.widget.ConstraintLayout
import com.mapbox.rctmgl.utils.Logger
import com.mapbox.rctmgl.R
import android.os.Handler


interface RCTMGLMapNavigationViewLifecycleOwner : LifecycleOwner {
    fun handleLifecycleEvent(event: Lifecycle.Event)
}

//open class RCTMGLMapNavigationView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
//                                                         defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
open class RCTMGLMapNavigationView(private val mContext: Context, var mManager: RCTMGLMapNavigationViewManager, var mAccessToken: String) : FrameLayout(mContext) {

    private val speedLimit: MapboxSpeedInfoView
    private val mapView: MapView
    private val maneuverView: MapboxManeuverView

    init {
        //TODO
        inflate(context, R.layout.mapbox_activity_turn_by_turn_experience, this)
        speedLimit = findViewById(R.id.speedLimit)
        mapView = findViewById(R.id.mapView)
        maneuverView = findViewById(R.id.maneuverView)
        maneuverView.updatePrimaryManeuverTextAppearance(R.style.MyMapboxStylePrimaryManeuver)
        maneuverView.updateSubManeuverTextAppearance(R.style.MyMapboxStyleSubManeuver)
        maneuverView.updateSecondaryManeuverTextAppearance(R.style.MyMapboxStyleSecondaryManeuver)
        maneuverView.updateStepDistanceTextAppearance(R.style.MyMapboxStyleStepDistance)
        maneuverView.updateUpcomingManeuverStepDistanceTextAppearance(R.style.MyMapboxStyleStepDistance)

        // Required for rendering properly in Android Oreo
        viewTreeObserver.dispatchOnGlobalLayout()
    }

    fun init() {
        // Required for rendering properly in Android Oreo
        viewTreeObserver.dispatchOnGlobalLayout()
        Logger.w("RCTMGLMapNavigationView", "RCTMGLMapNavigationView.init from " + mFromLatitude + ", " + mFromLongitude + " to " + mToLatitude + ", " + mToLongitude)
    }

    private var lifecycleOwner : RCTMGLMapNavigationViewLifecycleOwner? = null

    override fun onAttachedToWindow() {
        Logger.w("RCTMGLMapNavigationView", "onAttachedToWindow")
        if (lifecycleOwner == null) {
            lifecycleOwner = object : RCTMGLMapNavigationViewLifecycleOwner {
                private lateinit var lifecycleRegistry: LifecycleRegistry
                init {
                    lifecycleRegistry = LifecycleRegistry(this)
                    lifecycleRegistry.currentState = Lifecycle.State.CREATED
                }

                override fun handleLifecycleEvent(event: Lifecycle.Event) {
                    try {
                        lifecycleRegistry.handleLifecycleEvent(event)
                    } catch (e: RuntimeException) {
                    }
                }

                override fun getLifecycle(): Lifecycle {
                    return lifecycleRegistry
                }
            }
            ViewTreeLifecycleOwner.set(this, lifecycleOwner);
        } else {
            lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        super.onAttachedToWindow()

        // initialize Navigation Camera
        viewportDataSource = MapboxNavigationViewportDataSource(mapView.getMapboxMap())
        navigationCamera = NavigationCamera(
            mapView.getMapboxMap(),
            mapView.camera,
            viewportDataSource
        )
        // set the animations lifecycle listener to ensure the NavigationCamera stops
        // automatically following the user location when the map is interacted with
        mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )
        val _this = this
        navigationCamera.registerNavigationCameraStateChangeObserver { navigationCameraState ->
            // shows/hide the recenter button depending on the camera state
            when (navigationCameraState) {
                NavigationCameraState.TRANSITION_TO_FOLLOWING,
                NavigationCameraState.FOLLOWING -> mManager.handleEvent(OnShowResumeButtonEvent(_this, false))
                NavigationCameraState.TRANSITION_TO_OVERVIEW,
                NavigationCameraState.OVERVIEW,
                NavigationCameraState.IDLE -> mManager.handleEvent(OnShowResumeButtonEvent(_this, true))
            }
        }
        // set the padding values depending on screen orientation and visible view layout
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.overviewPadding = landscapeOverviewPadding
        } else {
            viewportDataSource.overviewPadding = overviewPadding
        }
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.followingPadding = landscapeFollowingPadding
        } else {
            viewportDataSource.followingPadding = followingPadding
        }

        // make sure to use the same DistanceFormatterOptions across different features
        val distanceFormatterOptions = DistanceFormatterOptions.Builder(context).build()

        // initialize maneuver api that feeds the data to the top banner maneuver view
        maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(distanceFormatterOptions)
        )

        // initialize bottom progress view
        tripProgressApi = MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(context)
                .distanceRemainingFormatter(
                    DistanceRemainingFormatter(distanceFormatterOptions)
                )
                .timeRemainingFormatter(
                    TimeRemainingFormatter(context)
                )
                .percentRouteTraveledFormatter(
                    PercentDistanceTraveledFormatter()
                )
                .estimatedTimeToArrivalFormatter(
                    EstimatedTimeToArrivalFormatter(context, TimeFormat.NONE_SPECIFIED)
                )
                .build()
        )

        // initialize voice instructions api and the voice instruction player
        speechApi = MapboxSpeechApi(
            context,
            mAccessToken,
            Locale.US.language
        )
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
            context,
            mAccessToken,
            Locale.US.language
        )

        isVoiceInstructionsMuted = false

        // initialize route line, the withRouteLineBelowLayerId is specified to place
        // the route line below road labels layer on the map
        // the value of this option will depend on the style that you are using
        // and under which layer the route line should be placed on the map layers stack
        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(context)
            .withRouteLineBelowLayerId("road-label-navigation")
            .build()
        routeLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        // initialize maneuver arrow view to draw arrows on the map
        val routeArrowOptions = RouteArrowOptions.Builder(context).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)

        // load map style
        mapView.getMapboxMap().loadStyleUri("mapbox://styles/gomothership/cl0cqi0hz005g14pfd7076cw9") {
            // add long click listener that search for a route to the clicked destination
            mapView.gestures.addOnMapLongClickListener { point ->
                Logger.w("LOG", "addOnMapLongClickListener ")
                findRoute(point)
                true
            }
        }

        mapboxNavigation = MapboxNavigation(
                NavigationOptions.Builder(context)
                    .accessToken(mAccessToken)
                    .locationEngine(replayLocationEngine) // comment out the location engine.locationEngine(replayLocationEngine) setting block to disable simulation
                    .build()
                )

        mapboxNavigation?.registerRoutesObserver(routesObserver)
        mapboxNavigation?.registerArrivalObserver(arrivalObserver)
        mapboxNavigation?.registerLocationObserver(locationObserver)
        mapboxNavigation?.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation?.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        // start the trip session to being receiving location updates in free drive
        // and later when a route is set also receiving route progress updates
        mapboxNavigation?.startTripSession()

        initNavigation()
    }

    override fun onDetachedFromWindow() {
        Logger.w("RCTMGLMapNavigationView", "onDetachedFromWindow")
        lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        super.onDetachedFromWindow()

        mapboxReplayer.finish()
        maneuverApi.cancel()
        routeLineApi.cancel()
        routeLineView.cancel()
        speechApi.cancel()
        voiceInstructionsPlayer.shutdown()

        mapboxNavigation?.unregisterRoutesObserver(routesObserver)
        mapboxNavigation?.unregisterArrivalObserver(arrivalObserver)
        mapboxNavigation?.unregisterLocationObserver(locationObserver)
        mapboxNavigation?.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.unregisterRouteProgressObserver(replayProgressObserver)
        mapboxNavigation?.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)

        mapboxNavigation?.onDestroy()
    }

    fun getMapboxMap(): MapboxMap {
        return mapView.getMapboxMap()
    }

    fun onDropViewInstance() {
        lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    /**
     * Debug tool used to play, pause and seek route progress events that can be used to produce mocked location updates along the route.
     */
    private val mapboxReplayer = MapboxReplayer()

    /**
     * Debug tool that mocks location updates with an input from the [mapboxReplayer].
     */
    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)

    /**
     * Debug observer that makes sure the replayer has always an up-to-date information to generate mock updates.
     */
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    /**
     * Used to execute camera transitions based on the data generated by the [viewportDataSource].
     * This includes transitions from route overview to route following and continuously updating the camera as the location changes.
     */
    private lateinit var navigationCamera: NavigationCamera

    /**
     * Produces the camera frames based on the location and routing data for the [navigationCamera] to execute.
     */
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    /*
     * Below are generated camera padding values to ensure that the route fits well on screen while
     * other elements are overlaid on top of the map (including instruction view, buttons, etc.)
     */
    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            140.0 * pixelDensity,
            40.0 * pixelDensity,
            120.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeOverviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            20.0 * pixelDensity
        )
    }
    private val followingPadding: EdgeInsets by lazy {
        EdgeInsets(
            180.0 * pixelDensity,
            40.0 * pixelDensity,
            150.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeFollowingPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }

    /**
     * Generates updates for the [MapboxManeuverView] to display the upcoming maneuver instructions
     * and remaining distance to the maneuver point.
     */
    private lateinit var maneuverApi: MapboxManeuverApi

    /**
     * Generates updates for the [MapboxTripProgressView] that include remaining time and distance to the destination.
     */
    private lateinit var tripProgressApi: MapboxTripProgressApi

    /**
     * Generates updates for the [routeLineView] with the geometries and properties of the routes that should be drawn on the map.
     */
    private lateinit var routeLineApi: MapboxRouteLineApi

    /**
     * Draws route lines on the map based on the data from the [routeLineApi]
     */
    private lateinit var routeLineView: MapboxRouteLineView

    /**
     * Generates updates for the [routeArrowView] with the geometries and properties of maneuver arrows that should be drawn on the map.
     */
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()

    /**
     * Draws maneuver arrows on the map based on the data [routeArrowApi].
     */
    private lateinit var routeArrowView: MapboxRouteArrowView

    /**
     * Stores and updates the state of whether the voice instructions should be played as they come or muted.
     */
    private var isVoiceInstructionsMuted = false
        set(value) {
            field = value
            if (value) {
                voiceInstructionsPlayer.volume(SpeechVolume(0f))
            } else {
                voiceInstructionsPlayer.volume(SpeechVolume(1f))
            }
        }

    /**
     * Extracts message that should be communicated to the driver about the upcoming maneuver.
     * When possible, downloads a synthesized audio file that can be played back to the driver.
     */
    private lateinit var speechApi: MapboxSpeechApi

    /**
     * Plays the synthesized audio files with upcoming maneuver instructions
     * or uses an on-device Text-To-Speech engine to communicate the message to the driver.
     * NOTE: do not use lazy initialization for this class since it takes some time to initialize
     * the system services required for on-device speech synthesis. With lazy initialization
     * there is a high risk that said services will not be available when the first instruction
     * has to be played. [MapboxVoiceInstructionsPlayer] should be instantiated in
     * `Activity#onCreate`.
     */
    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer

    /**
     * Observes when a new voice instruction should be played.
     */
    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        speechApi.generate(voiceInstructions, speechCallback)
    }

    private val speedInfoApi = MapboxSpeedInfoApi()

    /**
     * Based on whether the synthesized audio file is available, the callback plays the file
     * or uses the fall back which is played back using the on-device Text-To-Speech engine.
     */
    private val speechCallback =
        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
            expected.fold(
                { error ->
                    // play the instruction via fallback text-to-speech engine
                    voiceInstructionsPlayer.play(
                        error.fallback,
                        voiceInstructionsPlayerCallback
                    )
                },
                { value ->
                    // play the sound file from the external generator
                    voiceInstructionsPlayer.play(
                        value.announcement,
                        voiceInstructionsPlayerCallback
                    )
                }
            )
        }

    /**
     * When a synthesized audio file was downloaded, this callback cleans up the disk after it was played.
     */
    private val voiceInstructionsPlayerCallback =
        MapboxNavigationConsumer<SpeechAnnouncement> { value ->
            // remove already consumed file to free-up space
            speechApi.clean(value)
        }

    /**
     * [NavigationLocationProvider] is a utility class that helps to provide location updates generated by the Navigation SDK
     * to the Maps SDK in order to update the user location indicator on the map.
     */
    private val navigationLocationProvider = NavigationLocationProvider()

    /**
     * Gets notified with location updates.
     *
     * Exposes raw updates coming directly from the location services
     * and the updates enhanced by the Navigation SDK (cleaned up and matched to the road).
     */
    private val locationObserver = object : LocationObserver {
        var firstLocationUpdateReceived = false

        override fun onNewRawLocation(rawLocation: Location) {
            // not handled
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            // update location puck's position on the map
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            // update camera position to account for new location
            viewportDataSource.onLocationChanged(enhancedLocation)
            viewportDataSource.evaluate()

            // update speed limit
            val speedInfo = speedInfoApi.updatePostedAndCurrentSpeed(locationMatcherResult,
                DistanceFormatterOptions.Builder(context).unitType(UnitType.IMPERIAL).locale(Locale.US).build()
            )
            speedLimit.render(speedInfo)

            // if this is the first location update the activity has received,
            // it's best to immediately move the camera to the current user location
            if (!firstLocationUpdateReceived) {
                firstLocationUpdateReceived = true
                navigationCamera.requestNavigationCameraToOverview(
                    stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                        .maxDuration(0) // instant transition
                        .build()
                )
            }
        }
    }

    private val arrivalObserver: ArrivalObserver = object : ArrivalObserver {
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            mManager.handleEvent(OnDidArriveEvent(this@RCTMGLMapNavigationView))
        }

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
        }

        override fun onWaypointArrival(routeProgress: RouteProgress) {
        }
    }

    /**
     * Gets notified with progress along the currently active route.
     */
    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        // update the camera position to account for the progressed fragment of the route
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        // draw the upcoming maneuver arrow on the map
        val style = mapView.getMapboxMap().getStyle()
        if (style != null) {
            val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
            routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
        }

        // update top banner with maneuver instructions
        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        maneuvers.fold(
            { error ->

            },
            {
                maneuverView.visibility = View.VISIBLE
                maneuverView.renderManeuvers(maneuvers)
            }
        )

        mManager.handleEvent(OnUpdateNavigationInfoEvent(this, routeProgress.distanceRemaining.toDouble(), routeProgress.durationRemaining.toDouble()))
    }

    /**
     * Gets notified whenever the tracked routes change.
     *
     * A change can mean:
     * - routes get changed with [MapboxNavigation.setRoutes]
     * - routes annotations get refreshed (for example, congestion annotation that indicate the live traffic along the route)
     * - driver got off route and a reroute was executed
     */
    private val routesObserver = RoutesObserver { routeUpdateResult ->
        if (routeUpdateResult.navigationRoutes.isNotEmpty()) {
            // generate route geometries asynchronously and render them
            routeLineApi.setNavigationRoutes(
                routeUpdateResult.navigationRoutes
            ) { value ->
                mapView.getMapboxMap().getStyle()?.apply {
                    routeLineView.renderRouteDrawData(this, value)
                }
            }

            // update the camera position to account for the new route
            viewportDataSource.onRouteChanged(routeUpdateResult.navigationRoutes.first())
            viewportDataSource.evaluate()
        } else {
            // remove the route line and route arrow from the map
            val style = mapView.getMapboxMap().getStyle()
            if (style != null) {
                routeLineApi.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(
                        style,
                        value
                    )
                }
                routeArrowView.render(style, routeArrowApi.clearArrows())
            }

            // remove the route reference from camera position evaluations
            viewportDataSource.clearRouteData()
            viewportDataSource.evaluate()
        }
    }

    private var mapboxNavigation: MapboxNavigation? = null

    private fun initNavigation() {
        Logger.w("RCTMGLMapNavigationView", "initNavigation")

        // initialize location puck
        //TODO
        mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    context,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            enabled = true
        }

        replayOriginLocation()

        Handler().postDelayed({
            startNavigation()
        }, 1000)
    }

    private fun replayOriginLocation() {
        Logger.w("RCTMGLMapNavigationView", "replayOriginLocation from " + mFromLatitude + ", " + mFromLongitude + " to " + mToLatitude + ", " + mToLongitude)

        mapboxReplayer.pushEvents(
            listOf(
                ReplayRouteMapper.mapToUpdateLocation(
                    Date().time.toDouble(),
                    Point.fromLngLat(mFromLongitude, mFromLatitude)
                )
            )
        )
        mapboxReplayer.playFirstLocation()
        mapboxReplayer.playbackSpeed(3.0)
    }

    fun startNavigation() {
        Logger.w("RCTMGLMapNavigationView", "startNavigation")
        findRoute(Point.fromLngLat(mToLongitude, mToLatitude))
    }

    private fun findRoute(destination: Point) {
        val originLocation = navigationLocationProvider.lastLocation
        val originPoint = originLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: return

        // execute a route request
        // it's recommended to use the
        // applyDefaultNavigationOptions and applyLanguageAndVoiceUnitOptions
        // that make sure the route request is optimized
        // to allow for support of all of the Navigation SDK features
        mapboxNavigation?.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
                .coordinatesList(listOf(originPoint, destination))
                // provide the bearing for the origin of the request to ensure
                // that the returned route faces in the direction of the current user movement
                .bearingsList(
                    listOf(
                        Bearing.builder()
                            .angle(originLocation.bearing.toDouble())
                            .degrees(45.0)
                            .build(),
                        null
                    )
                )
                .layersList(listOf(mapboxNavigation?.getZLevel(), null))
                .build(),
            object : NavigationRouterCallback {
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    // no impl
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    // no impl
                }

                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    setRouteAndStartNavigation(routes)
                }
            }
        )
    }

    private fun setRouteAndStartNavigation(routes: List<NavigationRoute>) {
        Logger.w("RCTMGLMapNavigationView", "setRouteAndStartNavigation")
        // set routes, where the first route in the list is the primary route that
        // will be used for active guidance
        mapboxNavigation?.setNavigationRoutes(routes)

        // move the camera to overview when new route is available
        navigationCamera.requestNavigationCameraToFollowing()
    }

    private fun clearRouteAndStopNavigation() {
        // clear
        mapboxNavigation?.setNavigationRoutes(listOf())

        // stop simulation
        mapboxReplayer.stop()
    }

    fun sendResponse(callbackID: String?, buildPayload: (map: WritableMap) -> Unit) {
        val payload: WritableMap = WritableNativeMap()
        buildPayload(payload)
        var event = AndroidCallbackEvent(this, callbackID, payload)
        mManager.handleEvent(event)
    }

    fun setVoiceMuted(callbackID: String?, voiceMuted: Boolean) {
        isVoiceInstructionsMuted = voiceMuted
    }

    fun recenter(callbackID: String?) {
        navigationCamera.requestNavigationCameraToFollowing()
    }

    fun isVoiceMuted(callbackID: String?) {
        sendResponse(callbackID, {
            it.putBoolean("isVoiceMuted", isVoiceInstructionsMuted)
        })
    }

//    fun setSourceVisibility(
//        visible: Boolean,
//        sourceId: String,
//        sourceLayerId: String?
//    ) {
//        if (mMap == null) {
//            Logger.e("MapView", "setSourceVisibility, map is null")
//            return
//        }
//        val style = mMap!!.getStyle();
//        style!!.styleLayers.forEach {
//            val layer = style.getLayer(it.id)
//            if ((layer != null) && match(layer, sourceId, sourceLayerId)) {
//                layer.visibility(
//                    if (visible) Visibility.VISIBLE else Visibility.NONE
//                )
//            }
//        }
//    }

    var mFromLatitude : Double = 0.0
    var mFromLongitude : Double = 0.0
    var mToLatitude : Double = 0.0
    var mToLongitude : Double = 0.0

    fun setReactFromLatitude(value: Double) {
        mFromLatitude = value
    }

    fun setReactFromLongitude(value: Double) {
        mFromLongitude = value
    }

    fun setReactToLatitude(value: Double) {
        mToLatitude = value
    }

    fun setReactToLongitude(value: Double) {
        mToLongitude = value
    }

}
