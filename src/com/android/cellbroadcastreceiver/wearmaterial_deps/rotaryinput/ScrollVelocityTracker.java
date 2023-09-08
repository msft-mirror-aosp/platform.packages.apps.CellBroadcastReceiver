package com.google.android.clockwork.common.wearable.wearmaterial.rotaryinput;

import android.view.MotionEvent;

/** An interface for calculating scroll velocity. */
public interface ScrollVelocityTracker {

  /** Works similarly to {@link VelocityTracker#addMovement(MotionEvent)}. */
  public default void addMovement(MotionEvent event) {}

  /** Works similarly to {@link VelocityTracker#computeCurrentVelocity(int)}. */
  public default void computeCurrentVelocity(int units) {}

  /** Works similarly to {@link VelocityTracker#computeCurrentVelocity(int, float)}. */
  public default void computeCurrentVelocity(int units, float maxVelocity) {}

  /** Equivalent to calling {@link #getScrollVelocity(int)} for the active pointer ID. */
  public default float getScrollVelocity() {
    return 0;
  }

  /**
   * Retrieve the last computed {@link MotionEvent#AXIS_SCROLL} velocity. You must first call {@link
   * #computeCurrentVelocity(int)} before calling this function.
   *
   * @param id Which pointer's velocity to return.
   * @return The previously computed scroll velocity.
   */
  public default float getScrollVelocity(int id) {
    return 0;
  }

  /** Works similarly to {@link VelocityTracker#clear()}. */
  public default void clear() {}

  /** Works similarly to {@link VelocityTracker#recycle()}. */
  public default void recycle() {}
}
