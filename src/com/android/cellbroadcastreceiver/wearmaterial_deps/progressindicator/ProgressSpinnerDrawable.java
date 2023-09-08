package com.google.android.clockwork.common.wearable.wearmaterial.progressindicator;

import static androidx.core.math.MathUtils.clamp;
import static com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.R.styleable.ProgressSpinnerDrawable;
import static com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.R.styleable.ProgressSpinnerDrawable_android_gravity;
import static com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.R.styleable.ProgressSpinnerDrawable_android_level;
import static com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.R.styleable.ProgressSpinnerDrawable_showEmptySweepAngle;
import static com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.R.styleable.ProgressSpinnerDrawable_spinnerDirection;
import static com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.R.styleable.ProgressSpinnerDrawable_spinnerProgressColor;
import static com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.R.styleable.ProgressSpinnerDrawable_spinnerRotation;
import static com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.R.styleable.ProgressSpinnerDrawable_spinnerStartAngle;
import static com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.R.styleable.ProgressSpinnerDrawable_spinnerSweepAngle;
import static com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.R.styleable.ProgressSpinnerDrawable_spinnerTrackColor;
import static com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.R.styleable.ProgressSpinnerDrawable_spinnerTrackEndAngle;
import static com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.R.styleable.ProgressSpinnerDrawable_spinnerTrackStartAngle;
import static com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.R.styleable.ProgressSpinnerDrawable_spinnerTrackWidth;
import static com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils.lerp;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.annotation.TargetApi;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import androidx.annotation.Keep;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * This {@link Drawable} is a progress-spinner. It draws a circular progress-bar on top of a
 * circular track. It can be used in controls and views that indicate a progression of a process or
 * an indeterminate waiting state.
 */
@Keep
public final class ProgressSpinnerDrawable extends Drawable {

  /**
   * Determines the direction of the angles provided to {@link #setStartAngle(float)} and {@link
   * #getSweepAngle()}
   */
  public enum Direction {
    CLOCKWISE(0),
    COUNTER_CLOCKWISE(1);

    private static Direction fromValue(int value) {
      return (value == 1) ? COUNTER_CLOCKWISE : CLOCKWISE;
    }

    private final int value;

    Direction(int value) {
      this.value = value;
    }
  }

  /** Number of degrees in a full circle. */
  private static final float MAX_DEGREES = 360;

  /** 0 degrees is on top of the circle, not on the left. Adjust for this */
  private static final float START_OFFSET = 270;

  /** The maximum value that {@link #setLevel(int)} accepts. */
  private static final int LEVEL_RANGE = 10_000;

  /**
   * A very small sweep-angle that is almost 0. When a sweep-angle is 0, no arc is drawn. When a
   * sweep-angle is just a bit larger, the arc is drawn with the round caps.
   */
  private static final float TINY_SWEEP_ANGLE_SIZE = 0.01f;

  private static final float DEGREES_PER_RADIAN = MAX_DEGREES / (2 * (float) Math.PI);
  private static final float DEFAULT_TRACK_WIDTH_DP = 3;
  private static final int DEFAULT_PROGRESS_COLOR = 0xFFFFFFFF;
  private static final int DEFAULT_TRACK_COLOR = 0x1AFFFFFF;
  private static final int TRACK_ALPHA = 26;

  private final ThemeState themeState = new ThemeState();

  private ColorStateList progressColor = ColorStateList.valueOf(DEFAULT_PROGRESS_COLOR);
  private @Nullable ColorStateList trackColor;

  private int gravity = Gravity.CENTER;

  private Direction direction = Direction.CLOCKWISE;
  private boolean showEmptySweepAngle;
  private float trackWidth;
  private float trackStartAngle;
  private float trackEndAngle = MAX_DEGREES;
  private float startAngle;
  private float sweepAngle;
  private float rotation;
  private float capRadiusInDegrees;

  private final Rect destSquare = new Rect();
  private final Paint paintProgress = new Paint();
  private final Paint paintTrack = new Paint();

  private final TypedArrayHelper typedArrayHelper = new TypedArrayHelper(ProgressSpinnerDrawable);

  public ProgressSpinnerDrawable() {
    trackWidth = DEFAULT_TRACK_WIDTH_DP * Resources.getSystem().getDisplayMetrics().density;

    paintProgress.setAntiAlias(true);
    paintProgress.setStyle(Style.STROKE);
    paintProgress.setStrokeCap(Cap.ROUND);
    paintProgress.setStrokeWidth(trackWidth);
    paintProgress.setColor(progressColor.getDefaultColor());

    paintTrack.setAntiAlias(true);
    paintTrack.setStyle(Style.STROKE);
    paintTrack.setStrokeWidth(trackWidth);
    paintTrack.setColor(progressColor.getDefaultColor());
  }

  @Override
  public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, @Nullable Theme theme)
      throws IOException, XmlPullParserException {
    super.inflate(r, parser, attrs, theme);

    TypedArray a = typedArrayHelper.inflate(theme, attrs, r);
    obtainAttributes(a);
    a.recycle();

    if (!typedArrayHelper.canApplyTheme()) {
      themeState.apply();
    }
  }

  private void obtainAttributes(TypedArray a) {
    obtainThemedAttributes(a);

    setTrackWidth(
        a.getDimension(ProgressSpinnerDrawable_spinnerTrackWidth, paintTrack.getStrokeWidth()));
    setTrackStartAngle(a.getFloat(ProgressSpinnerDrawable_spinnerTrackStartAngle, trackStartAngle));
    setTrackEndAngle(a.getFloat(ProgressSpinnerDrawable_spinnerTrackEndAngle, trackEndAngle));
    setDirection(
        Direction.fromValue(a.getInt(ProgressSpinnerDrawable_spinnerDirection, direction.value)));
    showEmptySweepAngle(
        a.getBoolean(ProgressSpinnerDrawable_showEmptySweepAngle, showEmptySweepAngle));
    setStartAngle(a.getFloat(ProgressSpinnerDrawable_spinnerStartAngle, startAngle));
    setSweepAngle(a.getFloat(ProgressSpinnerDrawable_spinnerSweepAngle, sweepAngle));
    setLevel(a.getInt(ProgressSpinnerDrawable_android_level, getLevel()));
    setRotation(a.getFloat(ProgressSpinnerDrawable_spinnerRotation, rotation));
    setGravity(a.getInt(ProgressSpinnerDrawable_android_gravity, gravity));
  }

  /** Obtains the possibly themed resource-values from TypedArray {@code a}. */
  private void obtainThemedAttributes(TypedArray a) {
    if (typedArrayHelper.hasResolvedValue(a, ProgressSpinnerDrawable_spinnerProgressColor)) {
      themeState.progressColor = a.getColorStateList(ProgressSpinnerDrawable_spinnerProgressColor);
    }

    if (typedArrayHelper.hasResolvedValue(a, ProgressSpinnerDrawable_spinnerTrackColor)) {
      themeState.trackColor = a.getColorStateList(ProgressSpinnerDrawable_spinnerTrackColor);
    }
  }

  @Override
  public boolean canApplyTheme() {
    return typedArrayHelper.canApplyTheme();
  }

  @Override
  public void applyTheme(Theme t) {
    TypedArray a = typedArrayHelper.applyTheme(t);
    obtainThemedAttributes(a);
    a.recycle();

    themeState.apply();
  }

  @Override
  public void draw(Canvas canvas) {
    if (!isVisible() || destSquare.isEmpty()) {
      return;
    }

    float start;
    float sweep;
    if (sweepAngle <= capRadiusInDegrees && showEmptySweepAngle) {
      sweep = TINY_SWEEP_ANGLE_SIZE;
      start = trackStartAngle + startAngle - sweep / 2;
    } else {
      // The calculation of 'sweep' takes the size of the two 'Cap.ROUND's into account.
      // Since the start and end of the arc are in the center of each cap-circle, the progress-bar
      // effectively extends beyond the start and end by an amount that is equal to the radius of
      // a round cap. The size of that radius, in degrees on the arc's circle, is defined by
      // 'capRadiusInDegrees'.
      if (sweepAngle == 0) {
        sweep = 0;
      } else if (sweepAngle <= capRadiusInDegrees) {
        sweep = TINY_SWEEP_ANGLE_SIZE;
      } else {
        float sweepTarget = sweepAngle - capRadiusInDegrees;
        float factor = min(sweepTarget / (MAX_DEGREES - capRadiusInDegrees), 1);
        sweep = lerp(sweepTarget, sweepAngle, factor);
      }
      start =
          (direction == Direction.CLOCKWISE)
              ? trackStartAngle + startAngle
              : (trackEndAngle - startAngle - sweep);
    }

    draw(canvas, start + START_OFFSET, sweep);
  }

  private void draw(Canvas canvas, float start, float sweep) {
    int savePoint = canvas.getSaveCount();

    canvas.clipRect(destSquare);
    canvas.translate(destSquare.left, destSquare.top);

    float pivot = destSquare.width() / 2f;
    canvas.rotate(rotation, pivot, pivot);

    float arcStart = (float) ceil(paintTrack.getStrokeWidth() / 2);
    float arcEnd = destSquare.right - arcStart;

    canvas.drawArc(
        arcStart,
        arcStart,
        arcEnd,
        arcEnd,
        trackStartAngle + START_OFFSET,
        getMaximumSweepAngle(),
        false,
        paintTrack);
    canvas.drawArc(arcStart, arcStart, arcEnd, arcEnd, start, sweep, false, paintProgress);

    canvas.restoreToCount(savePoint);
  }

  @Keep
  @Override
  public int getAlpha() {
    return paintProgress.getAlpha();
  }

  @Keep
  @Override
  public void setAlpha(int alpha) {
    if (paintTrack.getAlpha() != alpha) {
      paintProgress.setAlpha(alpha);
      paintTrack.setAlpha(alpha);
      invalidateSelf();
    }
  }

  @Override
  public @Nullable ColorFilter getColorFilter() {
    return paintProgress.getColorFilter();
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    paintProgress.setColorFilter(colorFilter);
    paintTrack.setColorFilter(colorFilter);
    invalidateSelf();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  @Override
  public boolean isStateful() {
    return progressColor.isStateful() || (trackColor != null && trackColor.isStateful());
  }

  @TargetApi(29)
  @Override
  public Insets getOpticalInsets() {
    Rect bounds = getBounds();
    int left = destSquare.left - bounds.left;
    int top = destSquare.top - bounds.top;
    int right = bounds.right - destSquare.right;
    int bottom = bounds.bottom - destSquare.bottom;
    return Insets.of(left, top, right, bottom);
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    updateBounds(bounds);
  }

  private void updateBounds(Rect bounds) {
    applyGravity(bounds);
    updateCapRadius();
    updateProgressPaint();
  }

  private void applyGravity(Rect bounds) {
    int size = min(bounds.width(), bounds.height());
    Gravity.apply(gravity, size, size, bounds, destSquare);
  }

  private void updateCapRadius() {
    capRadiusInDegrees = (trackWidth / getArcRadius()) * DEGREES_PER_RADIAN;
  }

  private void updateProgressPaint() {
    float fractionShown =
        showEmptySweepAngle
            ? 1
            : min(1, (sweepAngle * getArcRadius()) / DEGREES_PER_RADIAN / trackWidth);

    // Setting the stroke width to less than 'trackWidth' and making the progress-bar translucent
    // when the 'sweepAngle' is smaller than 'capRadiusInArcDegrees', enables to have a smooth
    // progress-bar transition from nothing into something (and vice-versa).
    paintProgress.setStrokeWidth(fractionShown * trackWidth);
  }

  private float getArcRadius() {
    return max(destSquare.width() - trackWidth, trackWidth) / 2;
  }

  @Override
  protected boolean onStateChange(int[] state) {
    return updateColors(state);
  }

  private boolean updateColors(int[] state) {
    int newProgressColor = progressColor.getColorForState(state, DEFAULT_PROGRESS_COLOR);
    boolean progressColorChanged = newProgressColor != paintProgress.getColor();
    if (progressColorChanged) {
      paintProgress.setColor(newProgressColor);
    }

    ColorStateList trackColor =
        (this.trackColor == null) ? progressColor.withAlpha(TRACK_ALPHA) : this.trackColor;

    int newTrackColor = trackColor.getColorForState(state, DEFAULT_TRACK_COLOR);
    boolean trackColorChanged = newTrackColor != paintTrack.getColor();
    if (trackColorChanged) {
      paintTrack.setColor(newTrackColor);
    }

    return progressColorChanged || trackColorChanged;
  }

  /**
   * The callback for {@link #setLevel(int)} when a level change has occurred.
   *
   * <p>Directly manages the sweepAngle by converting provided {@link Drawable} level to the
   * sweepAngle.
   */
  @Override
  protected boolean onLevelChange(int level) {
    level = clamp(abs(level), 0, LEVEL_RANGE);
    float maxAngle = getMaximumSweepAngle();
    float angle = level * maxAngle / (float) LEVEL_RANGE;

    if (sweepAngle == angle) {
      // Same level's sweepAngle. Ignore updating.
      return false;
    }

    sweepAngle = angle;

    if (!showEmptySweepAngle) {
      updateProgressPaint();
    }

    invalidateSelf();
    return true;
  }

  /** Sets the color of the progress-bar to the provided {@code progressColor}. */
  public void setProgressColor(ColorStateList progressColor) {
    this.progressColor = progressColor;
    if (updateColors(getState())) {
      invalidateSelf();
    }
  }

  /**
   * Sets the color of the circular track to the provided {@code trackColor}.
   *
   * <p>If {@code trackColor} is {@code null}, the track will have the same color as the {@code
   * progressColor}, but with the alpha channel set to 10%.
   */
  public void setTrackColor(@Nullable ColorStateList trackColor) {
    this.trackColor = trackColor;
    if (updateColors(getState())) {
      invalidateSelf();
    }
  }

  /** Sets the width of the progress-bar and track to the provided {@code trackWidth}. */
  public void setTrackWidth(float trackWidth) {
    trackWidth = max(0, trackWidth);
    if (this.trackWidth == trackWidth) {
      return;
    }

    this.trackWidth = trackWidth;
    paintTrack.setStrokeWidth(trackWidth);

    updateCapRadius();
    updateProgressPaint();
    invalidateSelf();
  }

  /**
   * Sets the start-angle (in degrees) of the track.
   *
   * <p>If {@code trackStartAngle} is {@code 0}, the track starts at the top of the circle.
   */
  @Keep
  public void setTrackStartAngle(float trackStartAngle) {
    if (this.trackStartAngle != trackStartAngle) {
      this.trackStartAngle = trackStartAngle;
      updateTrackCap();
      invalidateSelf();
    }
  }

  /** Sets the end-angle (in degrees) of the track. */
  @Keep
  public void setTrackEndAngle(float trackEndAngle) {
    if (this.trackEndAngle != trackEndAngle) {
      this.trackEndAngle = trackEndAngle;
      updateTrackCap();
      invalidateSelf();
    }
  }

  private void updateTrackCap() {
    paintTrack.setStrokeCap(getMaximumSweepAngle() == MAX_DEGREES ? Cap.BUTT : Cap.ROUND);
  }

  /** Returns the maximum sweep-angle, based on the track's start-angle and the end-angle. */
  public float getMaximumSweepAngle() {
    return abs(trackEndAngle - trackStartAngle);
  }

  /**
   * Sets the direction of the start-angle and the sweep-angle. When you change the direction, the
   * progress-bar will be effectively mirrored along the vertical y-axis.
   */
  public void setDirection(Direction direction) {
    if (this.direction != direction) {
      this.direction = direction;
      invalidateSelf();
    }
  }

  /**
   * Determines whether an 0-sized, empty, sweep-angle should be shown as a small filled circle or
   * not shown at all.
   */
  public void showEmptySweepAngle(boolean showEmptySweepAngle) {
    if (this.showEmptySweepAngle != showEmptySweepAngle) {
      this.showEmptySweepAngle = showEmptySweepAngle;
      updateProgressPaint();
      invalidateSelf();
    }
  }

  /**
   * Sets the gravity of the track and progress-bar to the provided {@code gravity}.
   *
   * <p>Any integer mask value from the {@link Gravity} is valid, but only a combination of these
   * are effective, because the track and progress-bar are circular and its width and height are
   * equal:
   *
   * <ul>
   *   <li>{@link Gravity#LEFT} or {@link Gravity#START}
   *   <li>{@link Gravity#RIGHT} or {@link Gravity#END}
   *   <li>{@link Gravity#TOP}
   *   <li>{@link Gravity#BOTTOM}
   *   <li>{@link Gravity#CENTER}
   * </ul>
   *
   * <p>The track and progress-bar will be placed within this {@code Drawable}'s bounds according to
   * the provided {@code gravity}.
   */
  public void setGravity(int gravity) {
    if (this.gravity != gravity) {
      this.gravity = gravity;
      updateBounds(getBounds());
      invalidateSelf();
    }
  }

  /** Returns the current start-angle. The value is module 360. */
  @Keep
  public float getStartAngle() {
    return startAngle;
  }

  /**
   * Sets the current start-angle to the provided {@code startAngle}. It determines the start of the
   * progress-bar.
   *
   * <p>If {@code startAngle} is {@code 0}, the progress-bar starts at the track's start-angle.
   *
   * <p>The current {@link Direction} determines the direction at which the start-angle is located.
   */
  @Keep
  public void setStartAngle(float startAngle) {
    float angle = startAngle % getMaximumSweepAngle();
    if (this.startAngle != angle) {
      this.startAngle = angle;
      invalidateSelf();
    }
  }

  /** Returns the current sweep-angle. The value is non-negative and clamped to 360. */
  @Keep
  public float getSweepAngle() {
    return sweepAngle;
  }

  /**
   * Sets the current sweep-angle to the provided {@code sweepAngle}. It determines the end of the
   * progress-bar and it is relative to the start-angle.
   *
   * <p>The current {@link Direction} determines the direction in which the sweep-angle moves from
   * the start-angle.
   *
   * <p>NOTE: The sweepAngle and level can be used interchangeably. Therefore, sweepAngle is
   * funneled through {@link #setLevel(int)} to ensure the level and sweepAngle match. This also
   * means that the passed in sweepAngle may be set slightly different since it's based on the
   * conversion of the level to sweepAngle rather than directly referring to the passed in
   * parameter.
   */
  @Keep
  public void setSweepAngle(float sweepAngle) {
    float angle = min(abs(sweepAngle), getMaximumSweepAngle());
    int level = Math.round(angle / getMaximumSweepAngle() * (float) LEVEL_RANGE);
    setLevel(level);
  }

  /** Returns the overall rotation of the progress-bar. */
  @Keep
  public float getRotation() {
    return rotation;
  }

  /**
   * Sets the overall rotation of the progress-bar to the provided {@code rotation}. The rotation is
   * not depending on the current {@link Direction} of this drawable. It is always rotated clockwise
   * if the provided {@code rotation} is positive.
   */
  @Keep
  public void setRotation(float rotation) {
    float angle = rotation % MAX_DEGREES;
    if (this.rotation != angle) {
      this.rotation = angle;
      invalidateSelf();
    }
  }

  /** The inner (non-static) class that manages theme related attribute state. */
  private class ThemeState {
    @Nullable ColorStateList progressColor;
    @Nullable ColorStateList trackColor;

    /** Applies the gathered state to the outer drawable. */
    void apply() {
      if (progressColor != null) {
        setProgressColor(progressColor);
      }

      setTrackColor(trackColor);
    }
  }
}
