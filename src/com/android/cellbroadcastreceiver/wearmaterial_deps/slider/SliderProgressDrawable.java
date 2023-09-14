package com.google.android.clockwork.common.wearable.wearmaterial.slider;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.Keep;
import javax.annotation.Nullable;

/**
 * Drawable for showing a slider value, increment separators can be specified and displayed as even
 * divisions on the slider progress bar.
 */
class SliderProgressDrawable extends Drawable {

  private final Paint progressPaint = new Paint();
  private final RectF progressRect = new RectF();
  private final Paint separatorPaint = new Paint();
  private final Paint trackPaint = new Paint();

  private float fillAmount;
  private float incrementSeparators;

  SliderProgressDrawable() {
    separatorPaint.setStrokeWidth(1);
    progressPaint.setAntiAlias(true);
    separatorPaint.setAntiAlias(true);
    trackPaint.setAntiAlias(true);
  }

  @Override
  public void draw(Canvas canvas) {
    canvas.save();

    Rect bounds = getBounds();
    float width = bounds.width();
    float height = bounds.height();
    if (width == 0 || height == 0) {
      canvas.restore();
      return;
    }

    if (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
      canvas.scale(-1, 1, width / 2, height / 2);
    }

    canvas.drawPaint(trackPaint);

    progressRect.right = width * fillAmount;
    progressRect.bottom = height;
    canvas.drawRect(progressRect, progressPaint);

    if (incrementSeparators > 0) {
      float widthIncrement = width / incrementSeparators;
      float dividerX = widthIncrement;
      for (int i = 0; i < incrementSeparators; i++) {
        canvas.drawLine(dividerX, 0, dividerX, height, separatorPaint);
        dividerX += widthIncrement;
      }
    }
    canvas.restore();
  }

  @Override
  public void setAlpha(int alpha) {}

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {}

  void setBaseBarColor(int color) {
    trackPaint.setColor(color);
  }

  void setFilledBarColor(int color) {
    progressPaint.setColor(color);
  }

  void setSeparatorColor(int color) {
    separatorPaint.setColor(color);
  }

  @Keep
  void setFillAmount(float fillAmount) {
    this.fillAmount = max(0.0f, min(1.0f, fillAmount));
    invalidateSelf();
  }

  @Keep
  float getFillAmount() {
    return fillAmount;
  }

  void setIncrementSeparators(int incrementSeparators) {
    this.incrementSeparators = incrementSeparators;
    invalidateSelf();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.OPAQUE;
  }
}
