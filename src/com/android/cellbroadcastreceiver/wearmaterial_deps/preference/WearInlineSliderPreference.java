package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;
import com.google.android.clockwork.common.wearable.wearmaterial.slider.WearInlineSlider;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Wear-specific preference using {@link WearInlineSlider}. Note this is similar to {@code
 * SeekBarPreference}, but it is not feasible to use that due it being written around having a
 * {@link android.widget.SeekBar} in the inflated layout. As such, this is not intended to be a
 * drop-in replacement for {@link androidx.preference.SeekBarPreference}.
 */
public class WearInlineSliderPreference extends Preference implements WearInlineSlider.Listener {

  private float minValue;
  private float maxValue;
  private float sliderIncrement;
  private float value;
  private boolean showIncrementSeparators;
  private boolean showMinimumIncrement;
  private @Nullable CharSequence decrementContentDescription;
  private @Nullable CharSequence incrementContentDescription;
  private @Nullable CharSequence progressContentDescription;
  private @Nullable LimitReachedListener limitReachedListener;

  public WearInlineSliderPreference(Context context) {
    this(context, null);
  }

  // AttributeSet can actually be null
  @SuppressWarnings("nullness:argument")
  public WearInlineSliderPreference(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.inlineSliderPreferenceStyle);
  }

  @SuppressWarnings("nullness:argument")
  public WearInlineSliderPreference(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  @SuppressWarnings("nullness:argument")
  public WearInlineSliderPreference(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    TypedArray array =
        context.obtainStyledAttributes(
            attrs, R.styleable.WearInlineSliderPreference, defStyleAttr, defStyleRes);
    minValue = array.getFloat(R.styleable.WearInlineSliderPreference_preferenceSliderMinValue, 0f);
    maxValue = array.getFloat(R.styleable.WearInlineSliderPreference_preferenceSliderMaxValue, 1f);
    showMinimumIncrement =
        array.getBoolean(
            R.styleable.WearInlineSliderPreference_preferenceSliderShowMinimumIncrement, false);
    sliderIncrement =
        array.getFloat(R.styleable.WearInlineSliderPreference_preferenceSliderIncrement, 1f);
    showIncrementSeparators =
        array.getBoolean(
            R.styleable.WearInlineSliderPreference_preferenceSliderIncrementSeparators, false);

    if (array.hasValue(R.styleable.WearInlineSlider_decrementContentDescription)) {
      decrementContentDescription =
          array.getString(R.styleable.WearInlineSlider_decrementContentDescription);
    }

    if (array.hasValue(R.styleable.WearInlineSlider_incrementContentDescription)) {
      incrementContentDescription =
          array.getString(R.styleable.WearInlineSlider_incrementContentDescription);
    }

    if (array.hasValue(R.styleable.WearInlineSlider_progressContentDescription)) {
      progressContentDescription =
          array.getString(R.styleable.WearInlineSlider_progressContentDescription);
    }

    array.recycle();
  }

  public void setIncrementContentDescription(CharSequence description) {
    if (!TextUtils.equals(incrementContentDescription, description)) {
      incrementContentDescription = description;
      notifyChanged();
    }
  }

  public void setDecrementContentDescription(CharSequence description) {
    if (!TextUtils.equals(decrementContentDescription, description)) {
      decrementContentDescription = description;
      notifyChanged();
    }
  }

  public void setProgressContentDescription(CharSequence description) {
    if (!TextUtils.equals(progressContentDescription, description)) {
      progressContentDescription = description;
      notifyChanged();
    }
  }

  /** Gets the lower bound set on the {@link WearInlineSlider}. */
  public float getMinValue() {
    return minValue;
  }

  /** Gets the upper bound set on the {@link WearInlineSlider}. */
  public float getMaxValue() {
    return maxValue;
  }

  /**
   * Returns the current value of the slider. This does not necessarily match the current value of
   * the {@link WearInlineSlider} if it called when inside {@link
   * Preference.OnPreferenceChangeListener}, or before the view is bound.
   */
  public float getValue() {
    return value;
  }

  /** Returns the increment set on the {@link WearInlineSlider}. */
  public float getSliderIncrement() {
    return sliderIncrement;
  }

  /** Returns true if separators should be used between increments on the slider. */
  public boolean isShowIncrementSeparators() {
    return showIncrementSeparators;
  }

  /** Sets the increment amount on the {@link WearInlineSlider}. */
  public final void setSliderIncrement(float sliderIncrement) {
    if (this.sliderIncrement != sliderIncrement) {
      this.sliderIncrement = sliderIncrement;
      notifyChanged();
    }
  }

  /** Sets the lower bound on the {@link WearInlineSlider}. */
  public void setMinValue(float minValue) {
    if (this.minValue != minValue) {
      this.minValue = minValue;
      notifyChanged();
    }
  }

  /** Sets the upper bound on the {@link WearInlineSlider}. */
  public void setMaxValue(float maxValue) {
    if (this.maxValue != maxValue) {
      this.maxValue = maxValue;
      notifyChanged();
    }
  }

  public void setValue(float value) {
    setValueInternal(value, true);
  }

  /** Sets if increment separators should be shown on the {@link WearInlineSlider}. */
  public void setShowIncrementSeparators(boolean showIncrementSeparators) {
    if (this.showIncrementSeparators != showIncrementSeparators) {
      this.showIncrementSeparators = showIncrementSeparators;
      notifyChanged();
    }
  }

  /** Sets if the minimum increment should be shown on the {@link WearInlineSlider}. */
  public void setShowMinimumIncrement(boolean showMinimumIncrement) {
    if (this.showMinimumIncrement != showMinimumIncrement) {
      this.showMinimumIncrement = showMinimumIncrement;
      notifyChanged();
    }
  }

  public void setLimitReachedListener(@Nullable LimitReachedListener limitReachedListener) {
    this.limitReachedListener = limitReachedListener;
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return a.getFloat(index, 0f);
  }

  @Override
  protected void onSetInitialValue(@Nullable Object defaultValue) {
    if (defaultValue == null) {
      defaultValue = minValue;
    }
    setValueInternal(getPersistedFloat((Float) defaultValue), true);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {
    super.onBindViewHolder(holder);

    WearInlineSlider wearInlineSlider = (WearInlineSlider) holder.itemView;
    wearInlineSlider.setListener(this);
    wearInlineSlider.setValueFrom(minValue);
    wearInlineSlider.setValueTo(maxValue);
    wearInlineSlider.setValue(value);
    wearInlineSlider.setStepSize(sliderIncrement);
    wearInlineSlider.setShowIncrementSeparators(showIncrementSeparators);
    wearInlineSlider.setShowMinimumIncrement(showMinimumIncrement);

    if (incrementContentDescription != null) {
      wearInlineSlider.setIncrementContentDescription(incrementContentDescription);
    }

    if (decrementContentDescription != null) {
      wearInlineSlider.setDecrementContentDescription(decrementContentDescription);
    }

    if (progressContentDescription != null) {
      wearInlineSlider.setProgressContentDescription(progressContentDescription);
    }
  }

  @Override
  public void onInlineSliderValueChange(WearInlineSlider slider, float newValue) {
    if (value == newValue) {
      return;
    }

    if (callChangeListener(newValue)) {
      setValueInternal(newValue, false);
    } else {
      // Restore slider value if the listener ignores the change.
      slider.setValue(value);
    }

    // Treat clicks on the slider increment/decrement buttons as clicks on the preference.
    OnPreferenceClickListener onClickListener = getOnPreferenceClickListener();
    if (onClickListener != null && onClickListener.onPreferenceClick(this)) {
      return;
    }

    PreferenceManager preferenceManager = getPreferenceManager();
    if (preferenceManager != null) {
      PreferenceManager.OnPreferenceTreeClickListener listener =
          preferenceManager.getOnPreferenceTreeClickListener();
      if (listener != null) {
        // Ignore the return value when the preference is clicked even if it's handled, we
        // don't have any special handling for the click action.
        listener.onPreferenceTreeClick(this);
      }
    }
  }

  @Override
  public void onInlineSliderMinValueRepeated() {
    if (limitReachedListener != null) {
      limitReachedListener.onInlineSliderMinValueRepeated();
    }
  }

  @Override
  public void onInlineSliderMaxValueRepeated() {
    if (limitReachedListener != null) {
      limitReachedListener.onInlineSliderMaxValueRepeated();
    }
  }

  private void setValueInternal(float value, boolean notifyChanged) {
    if (this.value != value) {
      this.value = value;
      persistFloat(value);
      if (notifyChanged) {
        notifyChanged();
      }
    }
  }

  /** Callback for user attempts to change their values past their limits. */
  public interface LimitReachedListener {

    /** Called when the slider is attempted to be decreased when already at min value. */
    void onInlineSliderMinValueRepeated();

    /** Called when the slider is attempted to be increased when already at max value. */
    void onInlineSliderMaxValueRepeated();
  }
}
