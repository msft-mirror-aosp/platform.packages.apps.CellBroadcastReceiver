package com.google.android.clockwork.common.wearable.wearmaterial.rotaryinput;

import static android.view.InputDevice.SOURCE_ROTARY_ENCODER;
import static android.view.MotionEvent.AXIS_SCROLL;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Reads {@link android.view.MotionEvent} to determine whether they are rotary input events and to
 * obtain axis value.
 *
 * <p>This is extracted to simplify testing since there are no way to obtain events with specified
 * {@link MotionEvent#AXIS_SCROLL} values. Class is non-final to support mocking in unit tests.
 */
@TargetApi(VERSION_CODES.R)
class RotaryInputEventReader {
  private final float scaledScrollFactor;

  RotaryInputEventReader(Context context) {
    scaledScrollFactor = ViewConfiguration.get(context).getScaledVerticalScrollFactor();
  }

  boolean isRotaryScrollEvent(MotionEvent ev) {
    return ev.getSource() == SOURCE_ROTARY_ENCODER && ev.getAction() == MotionEvent.ACTION_SCROLL;
  }

  float getScrollDistance(MotionEvent ev) {
    return -ev.getAxisValue(AXIS_SCROLL) * scaledScrollFactor;
  }
}
