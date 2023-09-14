package com.google.android.clockwork.common.wearable.wearmaterial.util;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.TimeInterpolator;
import android.content.Context;
import androidx.annotation.AnimatorRes;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * This is an {@link Animator} that provides the method {@link #setRepeatCount(int), which is
 * missing on {@code Animator}s that are not {@code ValueAnimator}s.
 */
public final class RepeatableAnimator extends Animator {

  /**
   * This value used used with the {@link #setRepeatCount(int)} property to repeat the animation
   * indefinitely.
   */
  public static final int INFINITE = -1;

  private final Animator animator;
  private final @MonotonicNonNull RepeatableAnimatorTarget target;

  private int repeatCount = INFINITE;
  private int startCount = 0;

  /**
   * Creates an {@code RepeatableAnimator} that can repeatedly run the animation with the provided
   * {@code animatorId} on the provided {@code target}.
   *
   * <p>How often the animation is run, depends on this animator's repeat-count.
   *
   * @see #setRepeatCount(int)
   */
  // TODO(b/217967930): incompatible types in conditional expression.
  @SuppressWarnings("nullness:conditional")
  public RepeatableAnimator(Context context, Object target, @AnimatorRes int animatorId) {
    this.target =
        target instanceof RepeatableAnimatorTarget ? (RepeatableAnimatorTarget) target : null;
    animator = AnimatorInflater.loadAnimator(context, animatorId);
    animator.setTarget(target);
    animator.addListener(new RestartAnimatorListener());
  }

  /**
   * Sets how many times the animation should be repeated. If the {@code value} is 0, the animation
   * is never repeated. If it is greater than 0 or {@link #INFINITE}, it will be repeated that many
   * times after the initial animation. The repeat count is {@code INFINITE} by default.
   */
  public void setRepeatCount(int value) {
    repeatCount = value;
  }

  @Override
  public long getStartDelay() {
    return animator.getStartDelay();
  }

  /** Does nothing; the start delay cannot be changed. */
  @Override
  public void setStartDelay(long startDelay) {
    animator.setStartDelay(startDelay);
  }

  /** Does nothing; the inner (child) animations have their own durations. */
  @Override
  public Animator setDuration(long duration) {
    return this;
  }

  /**
   * Returns a negative value, indicating an that the inner (child) animations have their own
   * durations.
   */
  @Override
  public long getDuration() {
    return -1;
  }

  /** Does nothing, the inner (child) animations have their own interpolators. */
  @Override
  public void setInterpolator(TimeInterpolator value) {}

  @Override
  public boolean isRunning() {
    return startCount > 0;
  }

  @Override
  public void start() {
    if (!isStarted()) {
      startAgain();
    }
  }

  private void startAgain() {
    startCount++;
    if (target != null) {
      target.initialize();
    }
    animator.start();
  }

  @Override
  public void cancel() {
    if (isStarted()) {
      startCount = 0;
      animator.cancel();
    }
  }

  @Override
  public void end() {
    if (isStarted()) {
      startCount = 0;
      animator.end();
    }
  }

  private void restart() {
    if (repeatCount < 0 || repeatCount >= startCount) {
      startAgain();
    }
  }

  /** An {@code AnimatorListener} that restarts the animation when it ends. */
  private final class RestartAnimatorListener extends SimpleAnimatorListener {

    @Override
    public void onAnimationComplete(Animator animation) {
      restart();
    }
  }
}
