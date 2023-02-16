package com.mapbox.rctmgl.events;

import android.view.View;

import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.mapbox.rctmgl.events.constants.EventKeys;

public class OnDidArriveEvent extends AbstractEvent {

    public OnDidArriveEvent() {
        super(EventKeys.MAP_ON_DID_ARRIVE);
    }

    @Override
    public String getKey() {
        return EventKeys.MAP_ON_DID_ARRIVE;
    }

    @Override
    public WritableMap getPayload() {
        WritableMap properties = new WritableNativeMap();
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
