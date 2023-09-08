package com.google.android.clockwork.common.wearable.wearmaterial.pageindicator;

import static java.lang.Math.min;

import android.graphics.Canvas;
import android.graphics.Rect;

/** {@link WearPageIndicatorDrawable.CanvasTransformer} implementation for round screens. */
final class RoundCanvasTransformer implements WearPageIndicatorDrawable.CanvasTransformer {

  private static final float SEPARATION_ANGLE = 6.6f;

  private final float dotRadius;
  private final float padding;

  RoundCanvasTransformer(float dotRadius, float padding) {
    this.dotRadius = dotRadius;
    this.padding = padding;
  }

  @Override
  public void moveToFirstVisibleIndicator(Rect bounds, Canvas canvas, int firstPos, float center) {
    float radius = getArcRadius(bounds);
    float startAngleOffset = (firstPos - center) * SEPARATION_ANGLE;
    canvas.translate(bounds.width() / 2f, bounds.height() / 2f);
    canvas.rotate(-1 * startAngleOffset);
    canvas.translate(0, radius);
  }

  @Override
  public void moveToNextIndicator(Rect bounds, Canvas canvas) {
    float radius = getArcRadius(bounds);
    canvas.translate(0, -1 * radius);
    canvas.rotate(-1 * SEPARATION_ANGLE);
    canvas.translate(0, radius);
  }

  private float getArcRadius(Rect bounds) {
    return min(bounds.width() / 2, bounds.height() / 2) - padding - dotRadius / 2;
  }
}
