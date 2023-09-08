package com.google.android.clockwork.common.wearable.wearmaterial.progressindicator;

import static com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils.clamp;
import static com.google.android.clockwork.common.wearable.wearmaterial.util.ThemeUtils.applyThemeOverlay;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * View containing a Progress spinner with options for a text label on the top, bottom, start, or
 * end. This animated indicator may be used to show progress in determinant or indeterminant cases
 */
public final class WearProgressSpinnerIndicator extends LinearLayout {

  @VisibleForTesting TextView label;

  @VisibleForTesting View progressIndicator;

  private ProgressSpinnerDrawableAnimCoordinator drawableHelper;
  private int gravity;
  private int labelVerticalMargin;
  private int labelHorizontalMargin;
  private float storedProgress = 0;

  public WearProgressSpinnerIndicator(Context context) {
    this(context, null);
  }

  public WearProgressSpinnerIndicator(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  // Suppress these warnings, because at some point super-class methods are called
  // which cannot be annotated (with '@UnknownInitialization' for example).
  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  public WearProgressSpinnerIndicator(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(
        applyThemeOverlay(context, defStyleAttr, R.style.WearProgressSpinnerIndicatorDefault),
        attrs,
        defStyleAttr);

    initialize(attrs, defStyleAttr);
  }

  private void initialize(@Nullable AttributeSet attrs, int defStyleAttr) {
    Context context = getContext();

    LayoutInflater.from(context).inflate(R.layout.wear_progress_spinner_indicator, this, true);
    initializeViews();
    initializeAttributes(context, attrs, defStyleAttr);
  }

  private void initializeViews() {
    Resources resources = getResources();
    setPadding(
        (int) resources.getDimension(R.dimen.wear_progress_indicator_horizontal_padding),
        (int) resources.getDimension(R.dimen.wear_progress_indicator_vertical_padding),
        (int) resources.getDimension(R.dimen.wear_progress_indicator_horizontal_padding),
        (int) resources.getDimension(R.dimen.wear_progress_indicator_vertical_padding));

    progressIndicator = findViewById(R.id.wear_progress_indicator_spinner);
    ProgressSpinnerDrawable drawable = new ProgressSpinnerDrawable();
    progressIndicator.setForeground(drawable);
    drawableHelper = new ProgressSpinnerDrawableAnimCoordinator(drawable);

    label = findViewById(R.id.wear_progress_indicator_label);
    labelVerticalMargin =
        (int) resources.getDimension(R.dimen.wear_progress_indicator_text_view_vertical_margin);
    labelHorizontalMargin =
        (int) resources.getDimension(R.dimen.wear_progress_indicator_text_view_horizontal_margin);
  }

  private void initializeAttributes(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    TypedArray a =
        context.obtainStyledAttributes(
            attrs, R.styleable.WearProgressSpinnerIndicator, defStyleAttr, 0);
    try {
      setViewGravity(
          a.getInt(R.styleable.WearProgressSpinnerIndicator_android_gravity, Gravity.NO_GRAVITY));
      Drawable drawable = a.getDrawable(R.styleable.WearProgressSpinnerIndicator_spinner);
      drawable =
          drawable != null ? drawable : context.getDrawable(R.drawable.wear_progress_indicator);
      if (drawable instanceof ProgressSpinnerDrawable) {
        setSpinnerDrawable((ProgressSpinnerDrawable) drawable);
      }
      setSpinnerProgress(a.getFloat(R.styleable.WearProgressSpinnerIndicator_progress, 0));
      setText(a.getText(R.styleable.WearProgressSpinnerIndicator_android_text));
      setIndeterminateMode(
          a.getBoolean(R.styleable.WearProgressSpinnerIndicator_android_indeterminate, false));
    } finally {
      a.recycle();
    }
  }

  // dereference of possibly-null reference layoutParams
  @SuppressWarnings("nullness:dereference.of.nullable")
  @VisibleForTesting
  void setViewGravity(int gravity) {
    if (this.gravity == gravity || label == null) {
      return;
    }
    this.gravity = gravity;
    removeAllViews();
    LayoutParams layoutParams = (LayoutParams) label.getLayoutParams();
    setGravity(Gravity.CENTER);
    // Gravity refers to where the spinner is placed in relation to the text
    switch (gravity) {
      case Gravity.BOTTOM:
        label.setVisibility(VISIBLE);
        layoutParams.setMargins(0, 0, 0, labelVerticalMargin);
        label.setLayoutParams(layoutParams);
        setOrientation(LinearLayout.VERTICAL);
        addView(label);
        addView(progressIndicator);
        break;
      case Gravity.TOP:
        label.setVisibility(VISIBLE);
        layoutParams.setMargins(0, labelVerticalMargin, 0, 0);
        label.setLayoutParams(layoutParams);
        setOrientation(LinearLayout.VERTICAL);
        addView(progressIndicator);
        addView(label);
        break;
      case Gravity.START:
        label.setVisibility(VISIBLE);
        layoutParams.setMargins(labelHorizontalMargin, 0, 0, 0);
        label.setLayoutParams(layoutParams);
        setOrientation(LinearLayout.HORIZONTAL);
        addView(progressIndicator);
        addView(label);
        break;
      case Gravity.END:
        label.setVisibility(VISIBLE);
        layoutParams.setMargins(0, 0, labelHorizontalMargin, 0);
        label.setLayoutParams(layoutParams);
        setOrientation(LinearLayout.HORIZONTAL);
        addView(label);
        addView(progressIndicator);
        break;
      default:
        label.setVisibility(GONE);
        break;
    }
  }

  /**
   * Changes the Indicators {@link ProgressSpinnerDrawable} to indeterminant mode if set to true and
   * sets the last known progress if false
   */
  public void setIndeterminateMode(boolean indeterminate) {
    if (indeterminate) {
      drawableHelper.startIndeterminateAnimation(getContext());
    } else {
      drawableHelper.setProgress(storedProgress, getContext());
    }
  }

  public void setCountDown(long countDownDurationMs, boolean reverse, Runnable action) {
    drawableHelper.setCountDown(countDownDurationMs, reverse, action);
  }

  /**
   * Changes the Indicators {@link ProgressSpinnerDrawable} to show the provided {@code drawable}.
   *
   * <p>If the {@code drawable} is {@code null}, then the default will continue to be used.
   */
  public void setSpinnerDrawable(ProgressSpinnerDrawable drawable) {
    if (drawable != null) {
      progressIndicator.setForeground(drawable);
      if (drawableHelper != null) {
        drawableHelper.stopCurrentAnimation();
      }
      drawableHelper = new ProgressSpinnerDrawableAnimCoordinator(drawable);
    }
  }

  /**
   * Changes the Indicators {@link ProgressSpinnerDrawable} to show the Drawable from the provided
   * {@code resourceId}.
   *
   * <p>If the drawable is not found or an instance of {@link ProgressSpinnerDrawable}, then the
   * last set spinner will continue to be used.
   */
  public void setSpinnerResource(@DrawableRes int resourceId) {
    if (resourceId != 0) {
      Drawable drawable = getResources().getDrawable(resourceId, getContext().getTheme());
      if (drawable instanceof ProgressSpinnerDrawable) {
        setSpinnerDrawable((ProgressSpinnerDrawable) drawable);
      }
    }
  }

  /**
   * Sets the progress of the {@link ProgressSpinnerDrawable} when not in indeterminate mode. If the
   * indicator is in indeterminant mode then the progress will be saved but not displayed
   *
   * @param progress progress to display in the {@link ProgressSpinnerDrawable}, the value will be
   *     clamped between 0 and 1
   */
  public void setSpinnerProgress(float progress) {
    storedProgress = clamp(progress, 0, 1);
    drawableHelper.setProgress(progress, getContext());
  }

  /**
   * Returns the current progress value of the inner {@link ProgressSpinnerDrawable}, -1 will be
   * returned in the event that the indicator is in indeterminant mode
   */
  public float getSpinnerProgress() {
    return drawableHelper.getProgress();
  }

  /** Returns the text set in the indicator label or an empty string if there is no label shown */
  public CharSequence getText() {
    if (label != null && label.getVisibility() == VISIBLE) {
      return label.getText();
    }
    return "";
  }

  /** Sets the text of the visible label, if no label is visible no text will be set */
  public void setText(CharSequence text) {
    if (label != null && label.getVisibility() == VISIBLE) {
      label.setText(text);
    }
  }

  public boolean isIndeterminate() {
    return drawableHelper.isIndeterminate();
  }
}
