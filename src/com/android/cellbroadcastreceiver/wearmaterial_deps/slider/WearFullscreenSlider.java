package com.google.android.clockwork.common.wearable.wearmaterial.slider;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.TextView;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearCircularButton;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Full screen widget to provide increment / decrement button and callback to modify some value. */
public class WearFullscreenSlider extends ConstraintLayout {

  @VisibleForTesting final WearCircularButton decrementButton;
  @VisibleForTesting final WearCircularButton incrementButton;

  private final TextView textView;
  private final ValueUpdater valueUpdater;
  private final OnGlobalLayoutListener onGlobalLayoutListener;

  private @Nullable Listener listener;
  private Rect incrementButtonBounds = new Rect();
  private Rect decrementButtonBounds = new Rect();

  public WearFullscreenSlider(@NonNull Context context) {
    this(context, null);
  }

  public WearFullscreenSlider(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.fullscreenSliderStyle);
  }

  public WearFullscreenSlider(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, R.style.Widget_FullscreenSlider_Default);
  }

  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  public WearFullscreenSlider(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    LayoutInflater.from(context).inflate(R.layout.wear_fullscreen_slider, this);
    incrementButton = findViewById(R.id.wear_fullscreen_slider_increment);
    decrementButton = findViewById(R.id.wear_fullscreen_slider_decrement);
    textView = findViewById(R.id.wear_fullscreen_slider_text);
    valueUpdater = new ValueUpdater(incrementButton, decrementButton);
    valueUpdater.setListener(
        new ValueUpdater.Listener() {
          @Override
          public boolean onIncrementValue() {
            return listener != null && listener.incrementValue();
          }

          @Override
          public boolean onDecrementValue() {
            return listener != null && listener.decrementValue();
          }
        });

    applyAttributes(context, attrs, defStyleAttr, defStyleRes);
    onGlobalLayoutListener =
        () ->
            post(
                () -> {
                  // Resetting touch delegates will cutoff any tracked states within individual
                  // touch delegates during long presses.  Only do this if layout changes impact
                  // bounds.
                  Rect newIncrementBounds = getIncrementTouchBounds();
                  Rect newDecrementBounds = getDecrementTouchBounds();
                  if (!incrementButtonBounds.equals(newIncrementBounds)
                      || !decrementButtonBounds.equals(newDecrementBounds)) {
                    setTouchDelegate(new FullscreenSliderTouchDelegate(/* delegateView= */ this));
                    incrementButtonBounds = newIncrementBounds;
                    decrementButtonBounds = newDecrementBounds;
                  }
                });
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
  }

  @Override
  protected void onDetachedFromWindow() {
    getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
    super.onDetachedFromWindow();
  }

  private void applyAttributes(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    TypedArray a =
        context.obtainStyledAttributes(
            attrs, R.styleable.WearFullscreenSlider, defStyleAttr, defStyleRes);
    valueUpdater.setAllowLongPress(
        a.getBoolean(R.styleable.WearFullscreenSlider_allowLongPress, false));
    setIncrementIconDrawable(a.getDrawable(R.styleable.WearFullscreenSlider_incrementDrawable));
    setDecrementIconDrawable(a.getDrawable(R.styleable.WearFullscreenSlider_decrementDrawable));
    setDecrementContentDescription(
        a.getString(R.styleable.WearFullscreenSlider_decrementContentDescription));
    setIncrementContentDescription(
        a.getString(R.styleable.WearFullscreenSlider_incrementContentDescription));

    ColorStateList iconTint = a.getColorStateList(R.styleable.WearFullscreenSlider_buttonIconTint);
    if (iconTint != null) {
      incrementButton.setIconTintColor(iconTint);
      decrementButton.setIconTintColor(iconTint);
    }
    a.recycle();
  }

  private Rect getIncrementTouchBounds() {
    return new Rect(0, 0, getMeasuredWidth(), textView.getTop());
  }

  private Rect getDecrementTouchBounds() {
    return new Rect(0, textView.getBottom(), getMeasuredWidth(), getMeasuredHeight());
  }

  /**
   * Set the text shown between increment/decrement buttons.
   *
   * @param text the text to show between increment/decrement buttons.
   */
  public void setText(CharSequence text) {
    textView.setText(text);
  }

  /**
   * Set icon image shown on increment button.
   *
   * @param drawable the image drawable to set.
   */
  public void setIncrementIconDrawable(@Nullable Drawable drawable) {
    incrementButton.setIcon(drawable);
  }

  /**
   * Set icon image shown on decrement button.
   *
   * @param drawable the image drawable to set.
   */
  public void setDecrementIconDrawable(@Nullable Drawable drawable) {
    decrementButton.setIcon(drawable);
  }

  /**
   * Sets the increment button content description.
   *
   * @param description the content description to set.
   */
  public void setDecrementContentDescription(@Nullable CharSequence description) {
    decrementButton.setContentDescription(description);
  }

  /**
   * Sets the increment button content description.
   *
   * @param description the content description to set.
   */
  public void setIncrementContentDescription(@Nullable CharSequence description) {
    incrementButton.setContentDescription(description);
  }

  /**
   * Set the listener to receive value changes.
   *
   * @param listener the listener to be set.
   */
  public void setListener(@Nullable Listener listener) {
    this.listener = listener;
  }

  /** Listeners for notifying changes to the value represented. */
  public interface Listener {

    /**
     * Attempt to increment the value.
     *
     * @return {@code true} if value was incremented.
     */
    
    boolean incrementValue();

    /**
     * Attempt to decrement the value.
     *
     * @return {@code true} if value was decremented.
     */
    
    boolean decrementValue();
  }

  /** TouchDelegate implementation to expand touch areas for increment and decremtn buttons. */
  private class FullscreenSliderTouchDelegate extends TouchDelegate {

    private final TouchDelegate incrementTouchDelegate;
    private final TouchDelegate decrementTouchDelegate;

    private FullscreenSliderTouchDelegate(View delegateView) {
      super(new Rect(), delegateView);

      incrementTouchDelegate = new TouchDelegate(getIncrementTouchBounds(), incrementButton);
      decrementTouchDelegate = new TouchDelegate(getDecrementTouchBounds(), decrementButton);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
      float eventX = event.getX();
      float eventY = event.getY();
      boolean handled = incrementTouchDelegate.onTouchEvent(event);
      if (!handled) {
        event.setLocation(eventX, eventY);
        return decrementTouchDelegate.onTouchEvent(event);
      }
      return true;
    }

    @Override
    @TargetApi(VERSION_CODES.R)
    public boolean onTouchExplorationHoverEvent(MotionEvent event) {
      float eventX = event.getX();
      float eventY = event.getY();
      boolean handled = incrementTouchDelegate.onTouchExplorationHoverEvent(event);
      if (!handled) {
        event.setLocation(eventX, eventY);
        return decrementTouchDelegate.onTouchExplorationHoverEvent(event);
      }
      return true;
    }
  }
}
