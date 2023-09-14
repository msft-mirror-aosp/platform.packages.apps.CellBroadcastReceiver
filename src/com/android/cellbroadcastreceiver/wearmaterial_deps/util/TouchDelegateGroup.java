package com.google.android.clockwork.common.wearable.wearmaterial.util;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build.VERSION_CODES;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo.TouchDelegateInfo;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link TouchDelegate} implementation that combines multiple touch delegates. Touch events are
 * dispatched to the first delegate that consumes the event.
 */
public final class TouchDelegateGroup extends TouchDelegate {
  private final Map<TouchDelegate, View> touchDelegates;

  public TouchDelegateGroup(Map<TouchDelegate, View> touchDelegates, View parent) {
    super(new Rect(), parent);

    this.touchDelegates = touchDelegates;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    for (TouchDelegate touchDelegate : touchDelegates.keySet()) {
      if (touchDelegate.onTouchEvent(event)) {
        return true;
      }
    }
    return false;
  }

  @TargetApi(VERSION_CODES.Q)
  @Override
  public boolean onTouchExplorationHoverEvent(MotionEvent event) {
    for (TouchDelegate touchDelegate : touchDelegates.keySet()) {
      if (touchDelegate.onTouchExplorationHoverEvent(event)) {
        return true;
      }
    }
    return false;
  }

  @TargetApi(VERSION_CODES.Q)
  @Override
  public TouchDelegateInfo getTouchDelegateInfo() {
    Map<Region, View> targetMap = new HashMap<>();
    for (TouchDelegate touchDelegate : touchDelegates.keySet()) {
      TouchDelegateInfo touchDelegateInfo = touchDelegate.getTouchDelegateInfo();
      for (int i = 0; i < touchDelegateInfo.getRegionCount(); i++) {
        targetMap.put(touchDelegateInfo.getRegionAt(i), touchDelegates.get(touchDelegate));
      }
    }
    return new TouchDelegateInfo(targetMap);
  }
}
