package com.mapbox.rctmgl.events;

import android.view.View;

import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.mapbox.rctmgl.events.constants.EventKeys;
import com.mapbox.rctmgl.events.constants.EventTypes;

public class OnUpdateNavigationInfoEvent extends AbstractEvent {

    private final double mDistance;
    private final double mDuration;

    public OnUpdateNavigationInfoEvent(View view, double distance, double duration) {
        super(view, EventTypes.MAP_ON_UPDATE_NAVIGATION_INFO);
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
}
