package com.google.android.clockwork.common.wearable.wearmaterial.slider;

import android.animation.TimeAnimator;
import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.VisibleForTesting;


/**
 * Class to hook up press state / long press states with increment / decrement functions and
 * maintain auto increment / decrement states.
 */
class ValueUpdater {

  @VisibleForTesting static final int LONG_PRESS_UPDATE_INTERVAL_MS = 60;

  private final TimeAnimator longClickValueChangeAnimator = new TimeAnimator();

  private boolean allowLongPress;
  private Listener listener;
  private boolean incrementLongPress;
  private boolean decrementLongPress;
  private long lastValueUpdateTime;

  @SuppressLint("ClickableViewAccessibility")
  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  ValueUpdater(View incrementButton, View decrementButton) {
    longClickValueChangeAnimator.setTimeListener(
        (timeAnimator, totalTime, deltaTime) -> {
          if (totalTime - lastValueUpdateTime > LONG_PRESS_UPDATE_INTERVAL_MS) {
            lastValueUpdateTime = totalTime;
            autoUpdateValue();
          }
        });
    incrementButton.setOnClickListener(v -> incrementValue());
    incrementButton.setOnLongClickListener(
        v -> {
          if (allowLongPress) {
            incrementLongPress = true;
            updateLongClickValueAnimator();
          }
          return true;
        });
    incrementButton.setOnTouchListener(
        (v, event) -> {
          if (event.getAction() == MotionEvent.ACTION_CANCEL
              || event.getAction() == MotionEvent.ACTION_UP) {
            incrementLongPress = false;
            updateLongClickValueAnimator();
          }
          return false;
        });

    decrementButton.setOnClickListener(v -> decrementValue());
    decrementButton.setOnLongClickListener(
        v -> {
          if (allowLongPress) {
            decrementLongPress = true;
            updateLongClickValueAnimator();
          }
          return true;
        });
    decrementButton.setOnTouchListener(
        (v, event) -> {
          if (event.getAction() == MotionEvent.ACTION_CANCEL
              || event.getAction() == MotionEvent.ACTION_UP) {
            decrementLongPress = false;
            updateLongClickValueAnimator();
          }
          return false;
        });
  }

  void setListener(Listener listener) {
    this.listener = listener;
  }

  void setAllowLongPress(boolean allowLongPress) {
    this.allowLongPress = allowLongPress;
    cancelUpdates();
  }

  void cancelUpdates() {
    incrementLongPress = false;
    decrementLongPress = false;
    if (longClickValueChangeAnimator.isStarted()) {
      longClickValueChangeAnimator.cancel();
    }
  }

  private void updateLongClickValueAnimator() {
    if (!allowLongPress) {
      return;
    }

    if (incrementLongPress || decrementLongPress) {
      lastValueUpdateTime = 0;
      longClickValueChangeAnimator.start();
    } else {
      longClickValueChangeAnimator.cancel();
    }
  }

  private void autoUpdateValue() {
    if (listener == null || !(incrementLongPress || decrementLongPress)) {
      return;
    }

    boolean success;
    if (incrementLongPress) {
      success = listener.onIncrementValue();
    } else {
      success = listener.onDecrementValue();
    }

    if (!success) {
      cancelUpdates();
    }
  }

  private void decrementValue() {
    if (listener == null) {
      return;
    }

    listener.onDecrementValue();
  }

  private void incrementValue() {
    if (listener == null) {
      return;
    }

    listener.onIncrementValue();
  }

  interface Listener {

    
    boolean onIncrementValue();

    
    boolean onDecrementValue();
  }
}
