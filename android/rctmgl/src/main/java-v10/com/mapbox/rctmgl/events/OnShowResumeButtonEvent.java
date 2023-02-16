package com.mapbox.rctmgl.events;

import android.view.View;

import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.mapbox.rctmgl.events.constants.EventKeys;
import com.mapbox.rctmgl.events.constants.EventTypes;

public class OnShowResumeButtonEvent extends AbstractEvent {

    private final boolean mValue;

    public OnShowResumeButtonEvent(View view, boolean value) {
        super(view, EventTypes.MAP_ON_SHOW_RESUME_BUTTON);
        mValue = value;
    }

    @Override
    public String getKey() {
        return EventKeys.MAP_ON_SHOW_RESUME_BUTTON;
    }

    @Override
    public WritableMap getPayload() {
        WritableMap properties = new WritableNativeMap();
        properties.putBoolean("showResumeButton", mValue);
        return properties;
    }
}
