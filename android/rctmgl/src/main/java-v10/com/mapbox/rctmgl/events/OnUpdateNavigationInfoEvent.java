package com.mapbox.rctmgl.events;

import android.view.View;

import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.mapbox.rctmgl.events.constants.EventKeys;

public class OnUpdateNavigationInfoEvent extends AbstractEvent {

    private final double mDistance;
    private final double mDuration;

    public OnUpdateNavigationInfoEvent(double distance, double duration) {
        super(EventKeys.MAP_ON_UPDATE_NAVIGATION_INFO);
        mDistance = distance;
        mDuration = duration;
    }

    @Override
    public String getKey() {
        return EventKeys.MAP_ON_UPDATE_NAVIGATION_INFO;
    }

    @Override
    public WritableMap getPayload() {
        WritableMap properties = new WritableNativeMap();
        properties.putDouble("distanceRemaining", mDistance);
        properties.putDouble("durationRemaining", mDuration);
        return properties;
    }

    @Override
    public boolean canCoalesce() {
        // Make sure EventDispatcher never merges EventKeys.MAP_ANDROID_CALLBACK events.
        // These events are couples to unique callbacks references (promises) on the JS side which
        // each expect response with their corresponding callbackID
        return false;
    }
}
