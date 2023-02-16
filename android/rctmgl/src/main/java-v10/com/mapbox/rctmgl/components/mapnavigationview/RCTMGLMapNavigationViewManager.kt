package com.mapbox.rctmgl.components.mapnavigationview

import android.util.Log
import android.view.View
import com.facebook.react.bridge.*

import com.mapbox.rctmgl.components.AbstractEventEmitter
import com.facebook.react.uimanager.LayoutShadowNode
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.mapbox.rctmgl.events.constants.EventKeys
import com.mapbox.maps.MapboxMap
import com.facebook.react.common.MapBuilder
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.rctmgl.utils.ConvertUtils
import com.mapbox.rctmgl.utils.ExpressionParser
import com.mapbox.rctmgl.utils.GeoJSONUtils
import com.mapbox.rctmgl.utils.extensions.toCoordinate
import com.mapbox.rctmgl.utils.extensions.toScreenCoordinate
import java.lang.Exception
import java.util.HashMap
import com.mapbox.rctmgl.modules.RCTMGLModule
import com.mapbox.rctmgl.utils.Logger

open class RCTMGLMapNavigationViewManager(private val mReactContext: ReactApplicationContext?) :
    AbstractEventEmitter<RCTMGLMapNavigationView?>(mReactContext) {
    private val mViews: MutableMap<Int, RCTMGLMapNavigationView>
    override fun getName(): String {
        return REACT_CLASS
    }

    override fun createShadowNodeInstance(): LayoutShadowNode {
        return MapShadowNode(this)
    }

    override fun getShadowNodeClass(): Class<out LayoutShadowNode> {
        return MapShadowNode::class.java
    }

    override fun onAfterUpdateTransaction(mapView: RCTMGLMapNavigationView) {
        super.onAfterUpdateTransaction(mapView)
        Logger.w("RCTMGLMapNavigationView", "onAfterUpdateTransaction")
        if (mapView.getMapboxMap() == null) {
            mViews[mapView.id] = mapView
            mapView.init()
        }

    }

    override fun addView(mapView: RCTMGLMapNavigationView?, childView: View?, childPosition: Int) {
        mapView!!.addView(childView, childPosition)
    }

    override fun getChildCount(mapView: RCTMGLMapNavigationView?): Int {
        return mapView!!.getChildCount()
    }

    override fun getChildAt(mapView: RCTMGLMapNavigationView?, index: Int): View? {
        return mapView!!.getChildAt(index)
    }

    override fun removeViewAt(mapView: RCTMGLMapNavigationView?, index: Int) {
        mapView!!.removeViewAt(index)
    }

    override fun createViewInstance(themedReactContext: ThemedReactContext): RCTMGLMapNavigationView {
        Logger.w("RCTMGLMapNavigationView", "createViewInstance")
        return RCTMGLMapNavigationView(themedReactContext, this, RCTMGLModule.getAccessToken(mReactContext))
    }

    override fun onDropViewInstance(mapView: RCTMGLMapNavigationView) {
        Logger.w("RCTMGLMapNavigationView", "onDropViewInstance")
        val reactTag = mapView.id
        if (mViews.containsKey(reactTag)) {
            mViews.remove(reactTag)
        }
        mapView.onDropViewInstance()
        super.onDropViewInstance(mapView)
    }

    fun getByReactTag(reactTag: Int): RCTMGLMapNavigationView? {
        return mViews[reactTag]
    }

    // region React Props

    @ReactProp(name = "fromLatitude")
    fun setFromLatitude(mapView: RCTMGLMapNavigationView, fromLatitude: Double) {
        Logger.w("RCTMGLMapNavigationView", "setFromLatitude")
        mapView.setReactFromLatitude(fromLatitude)
    }

    @ReactProp(name = "fromLongitude")
    fun setFromLongitude(mapView: RCTMGLMapNavigationView, fromLongitude: Double) {
        Logger.w("RCTMGLMapNavigationView", "setFromLongitude")
        mapView.setReactFromLongitude(fromLongitude)
    }

    @ReactProp(name = "toLatitude")
    fun setToLatitude(mapView: RCTMGLMapNavigationView, toLatitude: Double) {
        Logger.w("RCTMGLMapNavigationView", "setToLatitude")
        mapView.setReactToLatitude(toLatitude)
    }

    @ReactProp(name = "toLongitude")
    fun setToLongitude(mapView: RCTMGLMapNavigationView, toLongitude: Double) {
        Logger.w("RCTMGLMapNavigationView", "setToLongitude")
        mapView.setReactToLongitude(toLongitude)
    }

    //endregion
    //region Custom Events
    override fun customEvents(): Map<String, String>? {
        return MapBuilder.builder<String, String>()
            .put(EventKeys.MAP_ON_DID_ARRIVE, "onDidArrive")
            .put(EventKeys.MAP_ON_SHOW_RESUME_BUTTON, "onShowResumeButton")
            .put(EventKeys.MAP_ON_UPDATE_NAVIGATION_INFO, "onUpdateNavigationInfo")
            .put(EventKeys.MAP_ANDROID_CALLBACK, "onAndroidCallback")
            .build()
    }

    override fun getCommandsMap(): Map<String, Int>? {
        return MapBuilder.builder<String, Int>()
            .put("setVoiceMuted", METHOD_SET_VOICE_MUTED)
            .put("isVoiceMuted", METHOD_GET_VOICE_MUTED)
            .put("recenter", METHOD_RECENTER)
            .build()
    }

    override fun receiveCommand(mapView: RCTMGLMapNavigationView, commandID: Int, args: ReadableArray?) {
        // allows method calls to work with componentDidMount
        val mapboxMap = mapView.getMapboxMap()
            ?: //            mapView.enqueuePreRenderMapMethod(commandID, args);
            return
        when (commandID) {
            METHOD_SET_VOICE_MUTED -> {
                mapView.setVoiceMuted(args!!.getString(0), args!!.getBoolean(1));
            }
            METHOD_GET_VOICE_MUTED -> {
                mapView.isVoiceMuted(args!!.getString(0));
            }
            METHOD_RECENTER -> {
                mapView.recenter(args!!.getString(0));
            }
        }
    }
    //endregion

    private class MapShadowNode(private val mViewManager: RCTMGLMapNavigationViewManager) :
        LayoutShadowNode() {
        override fun dispose() {
            super.dispose()
            diposeNativeMapView()
        }

        /**
         * We need this mapview to dispose (calls into nativeMap.destroy) before ReactNative starts tearing down the views in
         * onDropViewInstance.
         */
        private fun diposeNativeMapView() {
            val mapView = mViewManager.getByReactTag(reactTag)
            if (mapView != null) {
                UiThreadUtil.runOnUiThread {
                    try {
//                            mapView.dispose();
                    } catch (ex: Exception) {
                        Log.e(LOG_TAG, " disposeNativeMapView() exception destroying map view", ex)
                    }
                }
            }
        }
    }

    companion object {
        const val LOG_TAG = "RCTMGLMapNavigationViewManager"
        const val REACT_CLASS = "RCTMGLMapNavigationView"

        //endregion
        //region React Methods
        const val METHOD_SET_VOICE_MUTED = 2
        const val METHOD_GET_VOICE_MUTED = 3
        const val METHOD_RECENTER = 4
    }

    init {
        mViews = HashMap()
    }
}
