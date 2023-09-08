package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP;
import static android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT;
import static android.graphics.drawable.GradientDrawable.Orientation.RIGHT_LEFT;
import static android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM;
import static androidx.core.math.MathUtils.clamp;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import androidx.annotation.IntRange;
import androidx.annotation.Keep;
import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.ColorUtils;

import java.io.IOException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A {@code Drawable} that draws a vignette like effect.
 *
 * <p>It does so by drawing two horizontal ({@link #getOrientation()} returns {@link #HORIZONTAL})
 * or two vertical ({@link #getOrientation()} returns {@link #VERTICAL}) linear gradients.
 *
 * <p>The first gradient starts with the provided {@link #vignetteColor} and ends with a transparent
 * color.
 *
 * <p>The second gradient starts with a transparent color and ends with the provided {@link
 * #vignetteColor}.
 *
 * <p>Between the gradients, the user of this {@code VignetteDrawable} can set a clear-area,
 * defining where the first gradient ends and where the second gradient starts. Between the first
 * and second gradient, no vignetting will be applied (see {@link #setClearArea(int, int)}).
 */
@Keep
public final class PickerVignetteDrawable extends DrawableWrapper {

  public static final int HORIZONTAL = 0;
  public static final int VERTICAL = 1;

  private static final int DEFAULT_VIGNETTE_COLOR = 0xFF000000;

  /**
   * Configures and returns a new {@code VignetteDrawable} with the given {@code orientation} (can
   * be {@link #HORIZONTAL} or {@link #VERTICAL}).
   */
  public static PickerVignetteDrawable create(int orientation) {
    PickerVignetteDrawable drawable = new PickerVignetteDrawable();
    drawable.initialize(orientation);
    return drawable;
  }

  /**
   * The vignette-gradient, starting with the current {@link #vignetteColor} and ending with the
   * same color but with its alpha-channel set to 0.
   */
  private final int[] gradientColors = new int[2];

  /** The {@link GradientDrawable} that does the actual drawing of the {@link #gradientColors}. */
  private final GradientDrawable gradientDrawable;

  /** The area to the left or above the clear-area. */
  private final Rect startGradientRect = new Rect();

  /** The area to the right or below the clear-area. */
  private final Rect endGradientRect = new Rect();

  /** Start x or y coordinate of the clear-area. */
  private int startOfClearArea = -1;

  /** End x or y coordinate of the clear-area. */
  private int endOfClearArea = -1;

  private boolean isVertical = true;

  private ColorStateList vignetteColor;

  private int vignetteAlpha;

  /**
   * Creates a new vertical {@code VignetteDrawable} with a black vignette color that does not use
   * alpha-blending.
   */
  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  public PickerVignetteDrawable() {
    super(null);

    gradientDrawable = new GradientDrawable();
    gradientDrawable.setColors(gradientColors);
    setDrawable(gradientDrawable);
  }

  private void initialize(int orientation) {
    isVertical = (orientation == VERTICAL);
    setVignetteColor(ColorStateList.valueOf(DEFAULT_VIGNETTE_COLOR));
  }

  @Override
  public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, @Nullable Theme theme)
      throws IOException, XmlPullParserException {
    super.inflate(r, parser, attrs, theme);

    TypedArray a;
    if (theme != null) {
      a = theme.obtainStyledAttributes(attrs, R.styleable.VignetteDrawable, 0, 0);
    } else {
      a = r.obtainAttributes(attrs, R.styleable.VignetteDrawable);
    }

    isVertical = a.getInt(R.styleable.VignetteDrawable_android_orientation, VERTICAL) == VERTICAL;

    ColorStateList color = a.getColorStateList(R.styleable.VignetteDrawable_vignetteColor);
    if (color != null) {
      setVignetteColor(color);
    }

    a.recycle();
  }

  @Override
  public void draw(Canvas canvas) {
    if (startGradientRect.isEmpty() && endGradientRect.isEmpty()) {
      return;
    }
    if (!startGradientRect.isEmpty()) {
      GradientDrawable.Orientation startOrientation = isVertical ? TOP_BOTTOM : LEFT_RIGHT;
      gradientDrawable.setOrientation(startOrientation);
      gradientDrawable.setBounds(startGradientRect);
      gradientDrawable.draw(canvas);
    }

    if (!endGradientRect.isEmpty()) {
      GradientDrawable.Orientation endOrientation = isVertical ? BOTTOM_TOP : RIGHT_LEFT;
      gradientDrawable.setOrientation(endOrientation);
      gradientDrawable.setBounds(endGradientRect);
      gradientDrawable.draw(canvas);
    }
  }

  @Override
  public void invalidateDrawable(Drawable who) {
    // The internal 'gradientDrawable' modifies itself in the 'draw(Canvas)' method.
    // To avoid a forever invalidation-loop, don't call 'super.invalidateDrawable'
    // if 'who' is 'gradientDrawable' (modelled after the 'CrossfadeDrawable' class).
    if (who != gradientDrawable) {
      Callback callback = getCallback();
      if (callback != null) {
        callback.invalidateDrawable(who);
      }
    }
  }

  @Override
  public boolean isStateful() {
    return getVignetteColor().isStateful();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  @Override
  protected boolean onStateChange(int[] state) {
    boolean appearanceChanged = super.onStateChange(state);
    boolean colorChanged = handleColorChange(state);
    return appearanceChanged || colorChanged;
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    updateGradientRectsForClearArea(bounds);
  }

  /** Returns the orientation. It can either be {@link #HORIZONTAL} or {@link #VERTICAL}. */
  public int getOrientation() {
    return isVertical ? VERTICAL : HORIZONTAL;
  }

  public ColorStateList getVignetteColor() {
    if (vignetteColor == null) {
      vignetteColor = ColorStateList.valueOf(DEFAULT_VIGNETTE_COLOR);
    }
    return vignetteColor;
  }

  public void setVignetteColor(ColorStateList vignetteColor) {
    this.vignetteColor = vignetteColor;

    handleColorChange(getState());
    invalidateSelf();
  }

  /**
   * Sets the vignette-alpha.
   *
   * <p>The vignette is shown as a gradient with the vignette-color, ranging from full opaque to the
   * provided {@code vignetteAlpha}. Changing the vignette-alpha will change the gradient
   * accordingly.
   *
   * <p>Providing a value less than {@code 255} will cause the gradient to partially and gradually
   * hide the underlying content. Setting the value to {@code 255} will hide the underlying content
   * entirely with the blended vignette-color.
   *
   * <p>The default value of the vignette-alpha is 0.
   */
  public void setVignetteAlpha(@IntRange(from = 0, to = 255) int vignetteAlpha) {
    this.vignetteAlpha = vignetteAlpha;
    if (gradientColors[0] != 0) {
      determineEndGradientColor();
    }
    setColors();
    invalidateSelf();
  }

  public int getVignetteAlpha() {
    return vignetteAlpha;
  }

  /**
   * Designates a clear area that starts at the coordinate {@code start} and ends at the coordinate
   * {@code end}. No vignetting will be drawn inside the clear area.
   *
   * <p>To remove the clear area, provide {@code -1} as the value for both {@code start} and {@code
   * end}.
   */
  public void setClearArea(int start, int end) {
    if (startOfClearArea == start && endOfClearArea == end) {
      return;
    }

    startOfClearArea = start;
    endOfClearArea = end;

    updateGradientRectsForClearArea(getBounds());
    invalidateSelf();
  }

  
  private boolean handleColorChange(int[] state) {
    int color = getVignetteColor().getColorForState(state, DEFAULT_VIGNETTE_COLOR);
    if (gradientColors[0] == 0 || gradientColors[0] != color) {
      gradientColors[0] = color;
      determineEndGradientColor();
      setColors();
      return true;
    }
    return false;
  }

  private void determineEndGradientColor() {
    gradientColors[1] = ColorUtils.setAlphaComponent(gradientColors[0], vignetteAlpha);
  }

  /**
   * Explicitly sets colors. {@code GradientDrawable.setColors} must be called when changing {@code
   * gradientColors} even though it's a shared reference. Internally the drawable gets marked as
   * dirty. Without that call the colors won't update.
   */
  private void setColors() {
    gradientDrawable.mutate();
    gradientDrawable.setColors(gradientColors);
  }

  private void updateGradientRectsForClearArea(Rect bounds) {
    if (bounds.isEmpty()) {
      return;
    }

    int left = bounds.left;
    int top = bounds.top;
    int right = bounds.right;
    int bottom = bounds.bottom;

    if (isVertical) {
      int center = (top + bottom) / 2;
      ensureClearArea(center);

      startGradientRect.set(left, top, right, clamp(startOfClearArea, top, bottom));
      endGradientRect.set(left, clamp(endOfClearArea, top, bottom), right, bottom);
    } else {
      int center = (left + right) / 2;
      ensureClearArea(center);

      startGradientRect.set(left, top, clamp(startOfClearArea, left, right), bottom);
      endGradientRect.set(clamp(endOfClearArea, left, right), top, right, bottom);
    }
  }

  private void ensureClearArea(int center) {
    if (startOfClearArea < 0) {
      startOfClearArea = center;
    }

    if (endOfClearArea < 0) {
      endOfClearArea = center;
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  Rect getStartGradientRect() {
    return startGradientRect;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  Rect getEndGradientRect() {
    return endGradientRect;
  }
}
