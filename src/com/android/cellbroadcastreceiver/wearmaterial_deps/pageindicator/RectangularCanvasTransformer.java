package com.google.android.clockwork.common.wearable.wearmaterial.pageindicator;

import android.graphics.Canvas;
import android.graphics.Rect;

/** {@link WearPageIndicatorDrawable.CanvasTransformer} implementation for rectangular screens. */
final class RectangularCanvasTransformer implements WearPageIndicatorDrawable.CanvasTransformer {

  private final float dotDistance;
  private final float dotRadius;
  private final float padding;

  RectangularCanvasTransformer(float dotRadius, float dotDistance, float padding) {
    this.dotDistance = dotDistance;
    this.dotRadius = dotRadius;
    this.padding = padding;
  }

  @Override
  public void moveToFirstVisibleIndicator(Rect bounds, Canvas canvas, int firstPos, float center) {
    float x = bounds.width() / 2f - (center - firstPos) * dotDistance;
    float y = bounds.height() - padding - dotRadius / 2f;
    canvas.translate(x, y);
  }

  @Override
  public void moveToNextIndicator(Rect bounds, Canvas canvas) {
    canvas.translate(dotDistance, 0);
  }
}
