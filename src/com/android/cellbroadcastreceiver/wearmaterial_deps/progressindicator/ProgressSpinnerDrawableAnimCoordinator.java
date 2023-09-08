package com.google.android.clockwork.common.wearable.wearmaterial.progressindicator;

import static com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils.clamp;

import android.animation.Animator;
import android.content.Context;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A class to coordinate the setting of progress or indeterminate mode and accompanying
 * animators/animations on a {@link ProgressSpinnerDrawable}
 */
final class ProgressSpinnerDrawableAnimCoordinator {

  private static final float MAX_SWEEP_ANGLE = 360;
  public static final int INDETERMINANT_MODE_PROGRESS = -1;
  private final ProgressSpinnerDrawable drawable;
  private @Nullable Animator animator;
  private boolean isIndeterminate;
  private float startAngle;
  private float rotation;

  ProgressSpinnerDrawableAnimCoordinator(ProgressSpinnerDrawable drawable) {
    this.drawable = drawable;
    startAngle = this.drawable.getStartAngle();
  }

  /** Starts an indeterminant animation to denote unknown progress */
  void startIndeterminateAnimation(Context context) {
    stopCurrentAnimation();
    isIndeterminate = true;
    startAngle = drawable.getStartAngle();
    rotation = drawable.getRotation();
    animator = AnimatorUtils.loopIndeterminate(context, drawable);
    animator.start();
  }

  /** Stops the current animation and leaves the {@code drawable} in its most recent state */
  void stopCurrentAnimation() {
    if (animator != null) {
      animator.cancel();
      animator = null;
    }
    isIndeterminate = false;
  }

  /** Stops the current animation and resets the {@code drawable}'s progress to 0 */
  public void resetState() {
    if (animator != null) {
      animator.cancel();
      animator = null;
    }
    isIndeterminate = false;
    drawable.setStartAngle(startAngle);
    drawable.setSweepAngle(0);
  }

  /**
   * Starts an animation to move the {@code drawable} to the specified progress point, if the
   * drawable is in indeterminant mode it switches to determinant mode
   */
  void setProgress(float progress, Context context) {
    float clampedProgress = clamp(progress, 0, 1);
    if (isIndeterminate) {
      isIndeterminate = false;
      drawable.setStartAngle(startAngle);
      drawable.setRotation(rotation);
    }
    stopCurrentAnimation();
    float sweepAngle = MAX_SWEEP_ANGLE * clampedProgress;
    animator = AnimatorUtils.animateSweepAngle(context, drawable, sweepAngle);
    animator.start();
  }

  /** Returns the current progress of the drawable or -1 if it is in indeterminant mode */
  float getProgress() {
    if (isIndeterminate) {
      return INDETERMINANT_MODE_PROGRESS;
    }
    return drawable.getSweepAngle() / MAX_SWEEP_ANGLE;
  }

  /**
   * Starts an animation that completes the spinner circle after the specified duration
   *
   * @param countDownDurationMs the duration of the circular animation
   * @param reverse denotes whether the circle completes or empties
   * @param action the action to be performed after the animation is complete
   */
  public void setCountDown(long countDownDurationMs, boolean reverse, Runnable action) {
    stopCurrentAnimation();
    animator = AnimatorUtils.countDown(drawable, countDownDurationMs, reverse, action);
    animator.start();
  }

  /** Returns whether the drawable is in indeterminant mode or not */
  public boolean isIndeterminate() {
    return isIndeterminate;
  }
}
