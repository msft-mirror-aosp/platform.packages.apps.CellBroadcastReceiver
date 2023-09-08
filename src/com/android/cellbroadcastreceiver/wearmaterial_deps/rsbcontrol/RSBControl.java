package com.google.android.clockwork.common.wearable.wearmaterial.rsbcontrol;

import static com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils.clamp;
import static java.lang.Math.max;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.ColorInt;
import androidx.annotation.VisibleForTesting;
import com.google.android.clockwork.common.wearable.wearmaterial.animations.Durations;
import org.checkerframework.checker.nullness.qual.Nullable;

/** View element representing the RSB control */
public class RSBControl extends View {

  public static final int DEFAULT_MAX_VALUE = 10;

  private static final int SCROLL_BAR_FADE_IN_MS = ViewConfiguration.getScrollBarFadeDuration();
  private static final int SCROLL_BAR_FADE_OUT_MS = ViewConfiguration.getScrollBarFadeDuration();
  private static final long VALUE_TRANSITION_DURATION_MS = Durations.SLOW;
  private static final int SCROLL_DELAY_MULTIPLIER = 4;
  private static final TimeInterpolator OUT_CUBIC = new DecelerateInterpolator(1.5f);
  private static final float SCALE_ANIMATION_LOWER_BOUND = 0.65f;

  private RSBRenderer renderer;
  private int maxValue = 10;

  // actual value for the indicator, between min (0) and maxValue
  // may be different from the one shown if animating between values
  private int value = 0;

  private float displayedValue = 0;

  private final AnimatorSet fadeAndScaleAnimatorSet = new AnimatorSet();
  private final ValueAnimator displayedValueAnimator = new ValueAnimator();
  private final ObjectAnimator scaleYAnimator = new ObjectAnimator();
  private final ObjectAnimator fadeAnimator = new ObjectAnimator();
  private Animator.AnimatorListener fadeAnimatorListener;

  private enum ControlType {
    SLIDER,
    SELECTOR
  }

  public RSBControl(Context context) {
    this(context, null);
  }

  public RSBControl(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.rsbControlStyle);
  }

  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  public RSBControl(final Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initFromAttributes(context, attrs, defStyleAttr);
    initAnimator();
    setAlpha(0);
  }

  private void initFromAttributes(
      final Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    TypedArray a =
        context.getTheme().obtainStyledAttributes(attrs, R.styleable.RSBControl, defStyleAttr, 0);

    try {
      ControlType controlType =
          ControlType.values()[
              a.getInt(R.styleable.RSBControl_rsbControlType, ControlType.SLIDER.ordinal())];
      switch (controlType) {
        case SLIDER:
          renderer = new RSBSliderRenderer(context);
          break;
        case SELECTOR:
          renderer = new RSBSelectorRenderer(context);
          break;
      }

      renderer.sweepDegrees =
          a.getFloat(R.styleable.RSBControl_sweepDegrees, RSBRenderer.DEFAULT_RSB_SWEEP_DEGREES);

      int color =
          a.getColor(
              R.styleable.RSBControl_color,
              context.getResources().getColor(R.color.rsb_default_color));

      maxValue = a.getInt(R.styleable.RSBControl_maxValue, DEFAULT_MAX_VALUE);

      if (renderer != null) {
        renderer.setThumbColor(color);
        renderer.setMaxValue(maxValue);
      }
    } finally {
      a.recycle();
    }
  }

  private void initAnimator() {
    displayedValueAnimator.setInterpolator(OUT_CUBIC);
    displayedValueAnimator.addUpdateListener(
        animation -> {
          displayedValue = (float) animation.getAnimatedValue();
          invalidate();
        });

    scaleYAnimator.setProperty(View.SCALE_Y);
    scaleYAnimator.setTarget(this);

    fadeAnimator.setProperty(View.ALPHA);
    fadeAnimator.setTarget(this);
    fadeAnimatorListener =
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            fadeOutRSB();
          }
        };

    fadeAndScaleAnimatorSet.setInterpolator(OUT_CUBIC);
  }

  /**
   * Set the maximum value for the RSB control.
   *
   * @param maxValue the maximum value for the control.
   */
  public void setMaxValue(int maxValue) {
    if (this.maxValue != maxValue) {
      this.maxValue = maxValue;
      renderer.setMaxValue(maxValue);
    }
  }

  /** Returns the max value of the RSB control. */
  public int getMaxValue() {
    return maxValue;
  }

  /**
   * Set the thumb color on the RSB control.
   *
   * @param color the thumb color.
   */
  public void setThumbColor(@ColorInt int color) {
    renderer.setThumbColor(color);
  }

  /** Returns the thumb color of the RSB control. */
  @ColorInt
  public int getThumbColor() {
    return renderer.thumbColor;
  }

  /**
   * Set the sweep angle on the RSB control in degrees.
   *
   * @param sweepDegrees the thumb color.
   */
  public void setSweepDegrees(float sweepDegrees) {
    if (sweepDegrees != renderer.sweepDegrees) {
      renderer.sweepDegrees = sweepDegrees;
      invalidate();
    }
  }

  /** Returns the sweep angle of the RSB control in degrees. */
  public float getSweepDegrees() {
    return renderer.sweepDegrees;
  }

  /**
   * Sets the value shown on RSB control, animating if changed.
   *
   * @param value value to set RSBControl to.
   * @param animate whether to animate fade in / fade out if value has changed.
   */
  public void setValue(int value, boolean animate) {
    updateValueAndAnimateIfChanged(value, animate);
  }

  /** Returns the value of the RSB control. */
  public int getValue() {
    return value;
  }

  /** Fade in RSBControl temporarily. */
  public void fadeInRSB() {
    if (fadeAndScaleAnimatorSet.isRunning()) {
      fadeAndScaleAnimatorSet.cancel();
    }

    long animationDurationMs = (long) ((1f - getAlpha()) * SCROLL_BAR_FADE_IN_MS);

    fadeAnimator.setFloatValues(getAlpha(), 1f);

    scaleYAnimator.setFloatValues(max(SCALE_ANIMATION_LOWER_BOUND, getScaleY()), 1f);

    fadeAndScaleAnimatorSet.addListener(fadeAnimatorListener);
    fadeAndScaleAnimatorSet.setStartDelay(0);
    fadeAndScaleAnimatorSet.setDuration(animationDurationMs);
    fadeAndScaleAnimatorSet.playTogether(fadeAnimator, scaleYAnimator);
    fadeAndScaleAnimatorSet.start();
  }

  private void updateValueAndAnimateIfChanged(int value, boolean animateChanges) {
    int newValue = clamp(value, 0, maxValue);
    if (newValue != this.value) {
      this.value = newValue;
      onValueChanged(animateChanges);
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (renderer != null) {
      renderer.render(canvas, displayedValue);
    }
  }

  private void fadeOutRSB() {
    if (fadeAndScaleAnimatorSet.isRunning()) {
      fadeAndScaleAnimatorSet.cancel();
    }

    long animationDurationMs = (long) (getAlpha() * SCROLL_BAR_FADE_OUT_MS);
    // Delay aligned with scrollbars in View {@see android.view.View#initialAwakenScrollBars}
    long delay = (long) ViewConfiguration.getScrollDefaultDelay() * SCROLL_DELAY_MULTIPLIER;

    fadeAnimator.setFloatValues(getAlpha(), 0f);

    scaleYAnimator.setFloatValues(getScaleY(), SCALE_ANIMATION_LOWER_BOUND);

    fadeAndScaleAnimatorSet.removeListener(fadeAnimatorListener);
    fadeAndScaleAnimatorSet.setStartDelay(delay);
    fadeAndScaleAnimatorSet.setDuration(animationDurationMs);
    fadeAndScaleAnimatorSet.playTogether(fadeAnimator, scaleYAnimator);
    fadeAndScaleAnimatorSet.start();
  }

  private void onValueChanged(boolean animateChanges) {
    if (animateChanges) {
      fadeInRSB();
      animateIndicatorDisplayedValue();
    } else {
      displayedValue = value;
    }
    invalidate();
  }

  private void animateIndicatorDisplayedValue() {
    displayedValueAnimator.setFloatValues(displayedValue, value);
    displayedValueAnimator.setDuration(VALUE_TRANSITION_DURATION_MS);
    displayedValueAnimator.start();
  }

  @VisibleForTesting
  float getDisplayedValue() {
    return displayedValue;
  }
}
