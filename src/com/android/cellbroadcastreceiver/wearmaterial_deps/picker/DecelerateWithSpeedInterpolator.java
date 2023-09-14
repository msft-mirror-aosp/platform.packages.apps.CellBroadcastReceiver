package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static java.lang.Math.pow;

import android.view.animation.DecelerateInterpolator;

/** A {@link DecelerateInterpolator} whose current speed can be queried. */
final class DecelerateWithSpeedInterpolator extends DecelerateInterpolator {

  private static final float SPEED_TO_FACTOR_RATIO = 0.5f;

  private final float initialSpeed;

  private float lastInput = 0;

  /**
   * Creates a {@link DecelerateInterpolator} with the given {@code initialSpeed}.
   *
   * <p>The {@code initialSpeed} is equal to twice the {@code DecelerateInterpolator}'s "factor".
   *
   * <p>The {@code initialSpeed} is the value of this interpolator's derivative at {@code input =
   * 0}.
   */
  public DecelerateWithSpeedInterpolator(float initialSpeed) {
    super(initialSpeed * SPEED_TO_FACTOR_RATIO);
    this.initialSpeed = initialSpeed;
  }

  @Override
  public float getInterpolation(float input) {
    lastInput = input;
    return super.getInterpolation(input);
  }

  /** Returns the speed of the interpolator at its last interpolation input. */
  float getCurrentSpeed() {
    // The derivative of the Decelerator's interpolation is used as the function to return
    // the current speed.
    return SPEED_TO_FACTOR_RATIO * (float) (initialSpeed * pow(1 - lastInput, initialSpeed - 1));
  }
}
