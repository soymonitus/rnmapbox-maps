package com.mapbox.rctmgl.events;

import android.view.View;

import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.mapbox.rctmgl.events.constants.EventKeys;

public class OnShowResumeButtonEvent extends AbstractEvent {

    private final boolean mValue;

    public OnShowResumeButtonEvent(boolean value) {
        super(EventKeys.MAP_ON_SHOW_RESUME_BUTTON);
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

    @Override
    public boolean canCoalesce() {
        // Make sure EventDispatcher never merges EventKeys.MAP_ANDROID_CALLBACK events.
        // These events are couples to unique callbacks references (promises) on the JS side which
        // each expect response with their corresponding callbackID
        return false;
    }
}
