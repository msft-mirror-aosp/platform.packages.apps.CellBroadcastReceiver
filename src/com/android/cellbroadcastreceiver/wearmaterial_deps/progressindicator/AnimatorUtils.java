package com.google.android.clockwork.common.wearable.wearmaterial.progressindicator;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import androidx.annotation.Keep;
import androidx.annotation.VisibleForTesting;
import com.google.android.clockwork.common.wearable.wearmaterial.animations.Durations;
import com.google.android.clockwork.common.wearable.wearmaterial.util.RepeatableAnimator;
import com.google.android.clockwork.common.wearable.wearmaterial.util.RepeatableAnimatorTarget;
import com.google.android.clockwork.common.wearable.wearmaterial.util.SimpleAnimatorListener;

/** A set of utility functions related to animating a {@link ProgressSpinnerDrawable}. */
public final class AnimatorUtils {

  private static final String SWEEP_ANGLE = "sweepAngle";
  private static final String START_ANGLE = "startAngle";
  private static final Interpolator linearInterpolator = new LinearInterpolator();
  private static final PathInterpolator easingIncoming80 = new PathInterpolator(0, 0, 0.2f, 1);

  private static final float MAX_DEGREES = 360;
  private static final float FAST_FORWARD_CUTOFF = 0.05f;

  private AnimatorUtils() {}

  /**
   * Returns an {@link Animator} that will show the 'indeterminate progress' animation on the
   * provided {@code target}.
   */
  public static Animator loopIndeterminate(Context context, ProgressSpinnerDrawable target) {
    return new RepeatableAnimator(
        context, new StartEndSpinnerDrawableWrapper(target), R.animator.progress_indicator_loop);
  }

  /**
   * Returns an {@link Animator} that will show the progress animation on the provided {@code
   * target}.
   */
  public static Animator animateSweepAngle(
      Context context, ProgressSpinnerDrawable target, float newProgress) {
    Animator animator =
        AnimatorInflater.loadAnimator(context, R.animator.progress_indicator_incremental);
    animator.setTarget(target);
    ((ObjectAnimator) animator).setFloatValues(newProgress);
    return animator;
  }

  /**
   * Returns an {@link Animator} that shows a count-down animation over a period of {@code
   * countDownDurationMs} milliseconds. The animation animates the {@code target}'s "sweepAngle"
   * from 0 to 360 if {@code reverse} is false, from 360 to 0 if {@code reverse} is true.
   *
   * <p>When the animation ends, the {@code action} is called, which gives the caller a chance to
   * execute an action after the count-down has ended.
   *
   * <p>If the animation is cancelled, the {@code action} will not be called.
   */
  public static Animator countDown(
      ProgressSpinnerDrawable target, long countDownDurationMs, boolean reverse, Runnable action) {
    float maxAngle = target.getMaximumSweepAngle();
    float start = reverse ? maxAngle : 0;
    Animator animator = ObjectAnimator.ofFloat(target, SWEEP_ANGLE, start, maxAngle - start);
    animator.setInterpolator(linearInterpolator);
    animator.setDuration(countDownDurationMs);
    AnimatorListener listener =
        new SimpleAnimatorListener() {

          @Override
          public void onAnimationComplete(Animator animation) {
            action.run();
          }
        };
    animator.addListener(listener);
    return animator;
  }

  /**
   * Returns an "fast-forward" type of animator that animates the "sweepAngle" of the {@code target}
   * back to 0.
   */
  public static Animator fastForward(ProgressSpinnerDrawable target) {
    float maxAngle = target.getMaximumSweepAngle();
    float sweepAngle = target.getSweepAngle();
    Animator sweepAngleAnimator = ObjectAnimator.ofFloat(target, SWEEP_ANGLE, sweepAngle, 0);

    float progress = sweepAngle / maxAngle;
    if (progress <= FAST_FORWARD_CUTOFF) {
      sweepAngleAnimator.setInterpolator(easingIncoming80);
      sweepAngleAnimator.setDuration(Durations.RAPID);
      return sweepAngleAnimator;
    } else {
      float startAngle = target.getStartAngle();
      Animator startAnimator =
          ObjectAnimator.ofFloat(target, START_ANGLE, startAngle, startAngle + maxAngle);
      AnimatorSet animator = new AnimatorSet();
      animator.playTogether(sweepAngleAnimator, startAnimator);
      animator.setInterpolator(easingIncoming80);
      animator.setDuration(Durations.STANDARD);
      return animator;
    }
  }

  /**
   * Returns an "rewind" type of animator that animates the "sweepAngle" of the {@code target} back
   * to 0.
   */
  public static Animator rewind(ProgressSpinnerDrawable target) {
    float sweepAngle = target.getSweepAngle();
    Animator sweepAngleAnimator = ObjectAnimator.ofFloat(target, SWEEP_ANGLE, sweepAngle, 0);
    sweepAngleAnimator.setInterpolator(easingIncoming80);
    sweepAngleAnimator.setDuration(Durations.STANDARD);
    return sweepAngleAnimator;
  }

  /**
   * Wraps a {@link ProgressSpinnerDrawable} so that it can be animated using the {@code startAngle}
   * and <em>{@code endAngle}</em> properties (instead of the {@code startAngle} and {@code
   * sweepAngle} properties).
   */
  @VisibleForTesting
  public static final class StartEndSpinnerDrawableWrapper implements RepeatableAnimatorTarget {

    private final ProgressSpinnerDrawable delegate;

    private float startAngle;
    private float endAngle;

    StartEndSpinnerDrawableWrapper(ProgressSpinnerDrawable delegate) {
      this.delegate = delegate;
    }

    @Override
    public void initialize() {
      startAngle = delegate.getStartAngle() % MAX_DEGREES;
      endAngle = delegate.getStartAngle() + delegate.getSweepAngle();
    }

    @Keep
    public float getStartAngle() {
      return startAngle;
    }

    @Keep
    public void setStartAngle(float startAngle) {
      this.startAngle = startAngle;
      float fraction = delegate.getMaximumSweepAngle() / MAX_DEGREES;
      delegate.setStartAngle(fraction * startAngle);
      delegate.setSweepAngle(fraction * (endAngle - startAngle));
    }

    @Keep
    public float getEndAngle() {
      return endAngle;
    }

    @Keep
    public void setEndAngle(float endAngle) {
      this.endAngle = endAngle;
      float fraction = delegate.getMaximumSweepAngle() / MAX_DEGREES;
      delegate.setSweepAngle(fraction * (endAngle - startAngle));
    }

    @Keep
    public float getRotation() {
      return delegate.getRotation();
    }

    @Keep
    public void setRotation(float rotation) {
      delegate.setRotation(rotation);
    }
  }
}
