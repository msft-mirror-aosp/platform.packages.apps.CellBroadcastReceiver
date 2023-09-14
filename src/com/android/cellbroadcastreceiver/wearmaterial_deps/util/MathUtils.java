package com.google.android.clockwork.common.wearable.wearmaterial.util;

import static java.lang.Math.max;
import static java.lang.Math.min;

/** Shared math utilities for page indicator. */
public final class MathUtils {

  /**
   * Clamp value within range of [min, max].
   *
   * @param value the value to clamp.
   * @param min the minimum value that the result can be.
   * @param max the maximum value that the result can be.
   * @return the clamped value.
   */
  public static float clamp(float value, float min, float max) {
    return max(min, min(max, value));
  }

  /**
   * Clamp int value within range of [min, max].
   *
   * @param value the value to clamp.
   * @param min the minimum value that the result can be.
   * @param max the maximum value that the result can be.
   * @return the clamped value.
   */
  public static int clamp(int value, int min, int max) {
    return max(min, min(max, value));
  }

  /**
   * Linear interpolation between [start, end] using value as fraction.
   *
   * @param min the starting point of the interpolation range.
   * @param max the ending point of the interpolation range.
   * @param value the proportion of the range to linearly interpolate for.
   * @return the interpolated value.
   */
  public static float lerp(float min, float max, float value) {
    return min + (max - min) * value;
  }

  /**
   * Returns the interpolation value that satisfies the equation: {@code value = }{@link
   * #lerp}{@code (min, max, value)}
   *
   * <p>If {@code min == max}, then this function will return 0.
   */
  public static float lerpInv(float min, float max, float value) {
    return min != max ? ((value - min) / (max - min)) : 0.0f;
  }

  private MathUtils() {}
}
