package com.mapbox.rctmgl.events;

import android.view.View;

import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.mapbox.rctmgl.events.constants.EventKeys;
import com.mapbox.rctmgl.events.constants.EventTypes;

public class OnDidArriveEvent extends AbstractEvent {

    public OnDidArriveEvent(View view) {
        super(view, EventTypes.MAP_ON_DID_ARRIVE);
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
}
