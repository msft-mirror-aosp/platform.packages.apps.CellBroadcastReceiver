package com.google.android.clockwork.common.wearable.wearmaterial.slider;

import static com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils.clamp;
import static java.lang.Math.max;
import static java.lang.Math.round;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.VisibleForTesting;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * InlineSlider containing increment, decrement buttons. Progress is shown on the drawable in the
 * middle of the layout, with optional increment separators.
 */
public final class WearInlineSlider extends LinearLayout {

  private static final String TAG = "WearInlineSlider";

  private static final int DEF_STYLE_RES = R.style.Widget_InlineSlider_Default;
  private static final int MAX_INCREMENT_SEPARATORS = 8;

  private final ImageView increment;
  private final ImageView decrement;
  private final ImageView progress;
  private final SliderProgressDrawable sliderProgressDrawable = new SliderProgressDrawable();
  private final ValueUpdater valueUpdater;
  private final ObjectAnimator valueAnimator;

  @SuppressWarnings({"nullness:method.invocation", "nullness:methodref.receiver.bound"})
  private final Runnable updatePropertiesRunnable = this::updateProperties;

  private @Nullable Listener listener;
  private boolean showIncrementDividers;
  private boolean showMinimumIncrement;
  private float stepSize;
  private float valueFrom;
  private float valueTo;
  private float value;

  public WearInlineSlider(Context context) {
    this(context, null);
  }

  public WearInlineSlider(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.inlineSliderStyle);
  }

  public WearInlineSlider(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, DEF_STYLE_RES);
  }

  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  public WearInlineSlider(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    setOrientation(HORIZONTAL);

    // clip to outline so that children ripples do not get drawn outside slider boundary
    setClipToOutline(true);

    // In case a theme-overlay was applied, 'getContext()' may be different than 'context'.
    context = getContext();

    LayoutInflater.from(context).inflate(R.layout.wear_inline_slider, this, true);
    increment = findViewById(R.id.inline_slider_increment);
    decrement = findViewById(R.id.inline_slider_decrement);
    progress = findViewById(R.id.inline_slider_progress);
    int cornerRadius =
        context.getResources().getDimensionPixelSize(R.dimen.inline_slider_progress_corner_radius);
    progress.setOutlineProvider(
        new ViewOutlineProvider() {
          @Override
          public void getOutline(View view, Outline outline) {
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
          }
        });
    progress.setClipToOutline(true);
    progress.setImageDrawable(sliderProgressDrawable);
    valueUpdater = new ValueUpdater(increment, decrement);
    valueUpdater.setListener(
        new ValueUpdater.Listener() {

          @Override
          public boolean onIncrementValue() {
            if (value == valueTo) {
              if (listener != null) {
                listener.onInlineSliderMaxValueRepeated();
              }
              return false;
            }

            updateValue(clamp(value + stepSize, valueFrom, valueTo), /* forceRedraw= */ false);
            return true;
          }

          @Override
          public boolean onDecrementValue() {
            if (value == valueFrom) {
              if (listener != null) {
                listener.onInlineSliderMinValueRepeated();
              }
              return false;
            }

            updateValue(clamp(value - stepSize, valueFrom, valueTo), /* forceRedraw= */ false);
            return true;
          }
        });

    valueAnimator =
        (ObjectAnimator)
            AnimatorInflater.loadAnimator(context, R.animator.wear_slider_value_transition);
    valueAnimator.setTarget(sliderProgressDrawable);

    applyAttributes(context, attrs, defStyleAttr, defStyleRes);
  }

  private void applyAttributes(
      final Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    TypedArray a =
        context.obtainStyledAttributes(
            attrs, R.styleable.WearInlineSlider, defStyleAttr, defStyleRes);
    setAllowLongPress(a.getBoolean(R.styleable.WearInlineSlider_allowLongPress, false));
    setValueFrom(a.getFloat(R.styleable.WearInlineSlider_android_valueFrom, 0));
    setValueTo(a.getFloat(R.styleable.WearInlineSlider_android_valueTo, 1));
    setStepSize(a.getFloat(R.styleable.WearInlineSlider_android_stepSize, 0));
    setValue(a.getFloat(R.styleable.WearInlineSlider_android_value, 0));
    setFilledBarColor(a.getColor(R.styleable.WearInlineSlider_filledBarColor, 0));
    setSeparatorColor(a.getColor(R.styleable.WearInlineSlider_separatorColor, 0));
    setShowIncrementSeparators(
        a.getBoolean(R.styleable.WearInlineSlider_showIncrementSeparators, false));
    setShowMinimumIncrement(a.getBoolean(R.styleable.WearInlineSlider_showMinimumIncrement, false));
    setIncrementIconDrawable(a.getDrawable(R.styleable.WearInlineSlider_incrementDrawable));
    setDecrementIconDrawable(a.getDrawable(R.styleable.WearInlineSlider_decrementDrawable));
    sliderProgressDrawable.setBaseBarColor(
        a.getColor(R.styleable.WearInlineSlider_baseBarColor, 0));
    setDecrementContentDescription(
        a.getString(R.styleable.WearInlineSlider_decrementContentDescription));
    setIncrementContentDescription(
        a.getString(R.styleable.WearInlineSlider_incrementContentDescription));
    setProgressContentDescription(
        a.getString(R.styleable.WearInlineSlider_progressContentDescription));
    a.recycle();

    updateProperties();
  }

  /**
   * Registers a callback to be invoked when value changes.
   *
   * @param listener The callback to run when value changes.
   */
  public void setListener(@Nullable Listener listener) {
    this.listener = listener;
  }

  /**
   * Sets the value step size of the slider.
   *
   * @param stepSize the amount each increment / decrement can change the value by.
   */
  public void setStepSize(float stepSize) {
    if (this.stepSize != stepSize) {
      this.stepSize = stepSize;
      updatePropertiesWhenUiIsIdle();
    }
  }

  /**
   * Sets the min value of the slider.
   *
   * @param valueFrom the min value of the slider.
   */
  public void setValueFrom(float valueFrom) {
    if (this.valueFrom != valueFrom) {
      this.valueFrom = valueFrom;
      updatePropertiesWhenUiIsIdle();
    }
  }

  /**
   * Sets the max value of the slider.
   *
   * @param valueTo the max value of the slider.
   */
  public void setValueTo(float valueTo) {
    if (this.valueTo != valueTo) {
      this.valueTo = valueTo;
      updatePropertiesWhenUiIsIdle();
    }
  }

  /**
   * Sets the value of the slider.
   *
   * @param value the new value of the slider.
   */
  public void setValue(float value) {
    if (this.value != value) {
      updateValue(value, /* forceRedraw= */ false);
    }
  }

  /**
   * Set whether to show increment separators when number of separators required to divide value
   * range by stepSize is no more than {@code MAX_INCREMENT_SEPARATORS}.
   *
   * @param showIncrementDividers {@code true} when increment dividers should be shown.
   */
  public void setShowIncrementSeparators(boolean showIncrementDividers) {
    if (this.showIncrementDividers != showIncrementDividers) {
      this.showIncrementDividers = showIncrementDividers;
      updatePropertiesWhenUiIsIdle();
    }
  }

  /**
   * Set whether to show minimum value as 1 increment. When this is set, number of dividers will be
   * one more than case when minimum value is drawn as an empty progress bar.
   *
   * @param showMinimumIncrement {@code true} when minimum value should be represented by 1 step.
   */
  public void setShowMinimumIncrement(boolean showMinimumIncrement) {
    if (this.showMinimumIncrement != showMinimumIncrement) {
      this.showMinimumIncrement = showMinimumIncrement;
      updatePropertiesWhenUiIsIdle();
    }
  }

  /**
   * Set icon image shown on increment button.
   *
   * @param drawable the image drawable to set.
   */
  public void setIncrementIconDrawable(@Nullable Drawable drawable) {
    increment.setImageDrawable(drawable);
  }

  /**
   * Set icon image shown on decrement button.
   *
   * @param drawable the image drawable to set.
   */
  public void setDecrementIconDrawable(@Nullable Drawable drawable) {
    decrement.setImageDrawable(drawable);
  }

  /**
   * Sets the filled bar's color.
   *
   * @param color Color to be used on the filled portion of the slider value bar.
   */
  public void setFilledBarColor(int color) {
    sliderProgressDrawable.setFilledBarColor(color);
  }

  /**
   * Sets the separator's color.
   *
   * @param color Color to be used on the increment separators when shown.
   */
  public void setSeparatorColor(int color) {
    sliderProgressDrawable.setSeparatorColor(color);
  }

  /**
   * Sets whether long clicks on increment and decrement buttons will repeatedly change the value on
   * this slider.
   *
   * @param allowLongPress whether long clicks will affect the value.
   */
  public void setAllowLongPress(boolean allowLongPress) {
    valueUpdater.setAllowLongPress(allowLongPress);
  }

  /**
   * Sets the increment button content description.
   *
   * @param description the content description to set.
   */
  public void setDecrementContentDescription(@Nullable CharSequence description) {
    decrement.setContentDescription(description);
  }

  /**
   * Sets the increment button content description.
   *
   * @param description the content description to set.
   */
  public void setIncrementContentDescription(@Nullable CharSequence description) {
    increment.setContentDescription(description);
  }

  /**
   * Sets the increment button content description.
   *
   * @param description the content description to set.
   */
  public void setProgressContentDescription(@Nullable CharSequence description) {
    progress.setContentDescription(description);
  }

  /** Returns the step size of the slider. */
  public float getStepSize() {
    return stepSize;
  }

  /** Returns the min value of the slider. */
  public float getValueFrom() {
    return valueFrom;
  }

  /** Returns the max value of the slider. */
  public float getValueTo() {
    return valueTo;
  }

  /** Returns the value of the slider. */
  public float getValue() {
    return value;
  }

  @VisibleForTesting
  public CharSequence getIncrementContentDescription() {
    return increment.getContentDescription();
  }

  @VisibleForTesting
  public CharSequence getDecrementContentDescription() {
    return decrement.getContentDescription();
  }

  @VisibleForTesting
  public CharSequence getProgressContentDescription() {
    return progress.getContentDescription();
  }

  private void updatePropertiesWhenUiIsIdle() {
    removeCallbacks(updatePropertiesRunnable);
    post(updatePropertiesRunnable);
  }

  private void updateValue(float updateTo, boolean forceRedraw) {
    float previousValue = value;
    if (stepSize > 0) {
      // Number of full steps between 'valueFrom' (min) and 'updateTo' (new value).
      int stepMultiple = round((updateTo - valueFrom) / stepSize);

      // New value is minimum plus full steps to reach 'updateTo'.
      value = clamp(valueFrom + (stepMultiple * stepSize), valueFrom, valueTo);
    } else {
      value = clamp(updateTo, valueFrom, valueTo);
    }
    boolean valueChanged = value != previousValue;

    if (listener != null && valueChanged) {
      listener.onInlineSliderValueChange(this, value);
    }

    if (forceRedraw || valueChanged) {
      animateProgressFillAmount();
    }

    invalidate();
  }

  private void animateProgressFillAmount() {
    float currentFillAmount = sliderProgressDrawable.getFillAmount();
    float minValue = showMinimumIncrement ? valueFrom - stepSize : valueFrom;
    float currentValueDelta = value - minValue;
    float maximumValueDelta = valueTo - minValue;
    float newFillAmount;
    if (maximumValueDelta == 0) {
      // Fall back to empty fill if the state is invalid.
      newFillAmount = 0;
    } else {
      newFillAmount = currentValueDelta / maximumValueDelta;
    }

    if (currentFillAmount == newFillAmount) {
      return;
    }

    if (isLaidOut()) {
      valueAnimator.cancel();
      valueAnimator.setFloatValues(sliderProgressDrawable.getFillAmount(), newFillAmount);
      valueAnimator.start();
    } else {
      sliderProgressDrawable.setFillAmount(newFillAmount);
    }
  }

  private void updateProperties() {
    valueTo = max(valueFrom, valueTo);
    if (stepSize <= 0 || (valueTo - valueFrom) % stepSize > 0) {
      stepSize = valueTo - valueFrom;
    }

    int separators = 0;
    if (showIncrementDividers
        && (valueTo - valueFrom) / stepSize <= MAX_INCREMENT_SEPARATORS
        && (valueTo - valueFrom) % stepSize == 0) {
      separators = (int) ((valueTo - valueFrom) / stepSize);
      separators = showMinimumIncrement ? separators + 1 : separators;
    }

    updateValue(value, /* forceRedraw= */ true);
    sliderProgressDrawable.setIncrementSeparators(separators);
    valueUpdater.cancelUpdates();
  }

  @VisibleForTesting
  SliderProgressDrawable getSliderProgressDrawable() {
    return sliderProgressDrawable;
  }

  /**
   * Callback for value changes. This also includes callbacks for when the value is attempted to be
   * incremented or decremented while already at the maximum and minimum values, respectively.
   */
  public interface Listener {

    /**
     * Called when value is changed for a slider.
     *
     * @param slider the slider instance where value has changed.
     * @param value the value of the slider.
     */
    void onInlineSliderValueChange(WearInlineSlider slider, float value);

    /** Called when the slider is attempted to be decreased when already at min value. */
    default void onInlineSliderMinValueRepeated() {}

    /** Called when the slider is attempted to be increased when already at max value. */
    default void onInlineSliderMaxValueRepeated() {}
  }
}
