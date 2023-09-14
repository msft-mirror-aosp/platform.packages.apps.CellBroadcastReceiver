package com.google.android.clockwork.common.wearable.wearmaterial.pageindicator;

import static com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils.clamp;
import static com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils.lerp;
import static java.lang.Math.abs;
import static java.lang.Math.min;

import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Keep;
import androidx.core.graphics.ColorUtils;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Drawable to render a page indicator consists of dots laid out either in an arc or linearly.
 *
 * <p>Determine indicator positions using {@link IndicatorState} before drawing to canvas.
 *
 * <p>Translate positions expressed as page positions to actual canvas coordinates using {@link
 * CanvasTransformer} implementations which can either lay out the dots in a line or in an arc.
 * Based on overflow and transition percentages these dots are scaled down and hidden if too far
 * from center point.
 */
@Keep
public class WearPageIndicatorDrawable extends Drawable {

  // max visible indicators when page position is not in transition.
  private static final int MAX_VISIBLE_INDICATORS = 6;

  // maximum distance the page position can be from the center of the page indicator, in pages.
  private static final float MAX_PAGE_POS_TO_CENTER_DISTANCE = 0.5f;

  // page distance away from center when fade starts to be applied when overflow is needed.
  private static final float OVERFLOW_FADE_DISTANCE_TO_PAGE_POS = 1f;
  private static final float OVERFLOW_FADEOUT_LENGTH = MAX_VISIBLE_INDICATORS;

  private final Paint indicatorPaint = new Paint();
  private final IndicatorState state =
      new IndicatorState(MAX_VISIBLE_INDICATORS, MAX_PAGE_POS_TO_CENTER_DISTANCE);

  private CanvasTransformer canvasTransformer;
  private int dotRadius;
  private float selectedAlpha;
  private float unselectedAlpha;

  public WearPageIndicatorDrawable() {
    indicatorPaint.setAntiAlias(true);
  }

  public void setPageCount(int pageCount) {
    state.setPageCount(pageCount);
    invalidateSelf();
  }

  public void setPagePosition(float pagePosition) {
    state.setPagePosition(pagePosition);
    invalidateSelf();
  }

  @Override
  public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, @Nullable Theme theme)
      throws IOException, XmlPullParserException {
    super.inflate(r, parser, attrs, theme);
    final TypedArray a;
    if (theme != null) {
      a = theme.obtainStyledAttributes(attrs, R.styleable.WearPageIndicator, 0, 0);
    } else {
      a = r.obtainAttributes(attrs, R.styleable.WearPageIndicator);
    }
    dotRadius = a.getDimensionPixelSize(R.styleable.WearPageIndicator_radius, 0);
    selectedAlpha = a.getInteger(R.styleable.WearPageIndicator_selectedAlpha, 0);
    unselectedAlpha = a.getInteger(R.styleable.WearPageIndicator_unselectedAlpha, 0);

    int margin = a.getDimensionPixelSize(R.styleable.WearPageIndicator_margin, 0);
    if (r.getConfiguration().isScreenRound()) {
      canvasTransformer = new RoundCanvasTransformer(dotRadius, margin);
    } else {
      float dotDistance =
          r.getDimensionPixelOffset(R.dimen.wear_page_indicator_rectangular_dot_distance);
      canvasTransformer = new RectangularCanvasTransformer(dotRadius, dotDistance, margin);
    }

    a.recycle();
  }

  @Override
  public void draw(Canvas canvas) {
    if (canvasTransformer == null || !state.shouldShowIndicator()) {
      return;
    }

    state.updatePagePosition();
    int pageStart = state.getFirstVisibleIndicator();
    int pageEnd = state.getLastVisibleIndicator();
    float center = state.getCenterPagePosition();
    float pagePosition =
        needsMirroring() ? pageEnd - state.getPagePosition() : state.getPagePosition();

    canvas.save();
    canvasTransformer.moveToFirstVisibleIndicator(getBounds(), canvas, pageStart, center);
    for (int pos = pageStart; pos <= pageEnd; pos++) {
      float percentSelected = 1 - min(1, abs(pagePosition - pos));
      int alpha = (int) lerp(unselectedAlpha, selectedAlpha, percentSelected);
      int color = ColorUtils.setAlphaComponent(Color.WHITE, alpha);
      indicatorPaint.setColor(color);
      canvas.drawCircle(0, 0, computeIndicatorDotRadius(pos), indicatorPaint);
      canvasTransformer.moveToNextIndicator(getBounds(), canvas);
    }
    canvas.restore();
  }

  @Override
  public boolean isAutoMirrored() {
    return true;
  }

  private boolean needsMirroring() {
    return isAutoMirrored() && getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
  }

  private float computeIndicatorDotRadius(int position) {
    if (state.isFullPageRangeShown()) {
      return dotRadius;
    }

    float pagePosition = state.getPagePosition();
    float distToCenter = abs(position - state.getCenterPagePosition());
    float distToPagePosition = abs(position - pagePosition) - OVERFLOW_FADE_DISTANCE_TO_PAGE_POS;
    float distToFadeEdge = distToCenter - MAX_VISIBLE_INDICATORS / 2f;
    float overflowScale = clamp(1f - distToPagePosition / OVERFLOW_FADEOUT_LENGTH, 0f, 1f);
    float transitionScale = clamp(1f - distToFadeEdge * 2, 0f, 1f);
    return (float) dotRadius * transitionScale * overflowScale;
  }

  @Override
  public void setAlpha(int alpha) {}

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {}

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  /** Interface to provide transformation on canvas to move to each indicator dot position. */
  interface CanvasTransformer {

    /**
     * Apply transformation to {@link Canvas} so that it is centered on first indicator position.
     *
     * @param bounds bounds of the {@link WearPageIndicatorDrawable}.
     * @param canvas the {@link Canvas} to apply transformation to.
     * @param firstPosition the first indicator position that will be drawn.
     * @param center the center point page number to align the series of indicator dots.
     */
    void moveToFirstVisibleIndicator(Rect bounds, Canvas canvas, int firstPosition, float center);

    /**
     * Apply transformation to move origin from current indicator to the next.
     *
     * @param bounds the {@link WearPageIndicatorDrawable} bounding rect.
     * @param canvas the canvas for the current draw operation.
     */
    void moveToNextIndicator(Rect bounds, Canvas canvas);
  }
}
