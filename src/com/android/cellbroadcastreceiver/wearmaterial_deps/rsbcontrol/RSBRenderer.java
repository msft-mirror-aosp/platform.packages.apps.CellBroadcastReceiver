package com.google.android.clockwork.common.wearable.wearmaterial.rsbcontrol;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.RectF;

/** Base class for rendering RSB control views */
public abstract class RSBRenderer {

  static final float DEFAULT_RSB_SWEEP_DEGREES = 51f;

  private final float outsidePadding;

  protected final Paint thumbPaint = new Paint();
  protected final Paint trackPaint = new Paint();
  protected final RectF rect = new RectF();

  protected int thumbColor;
  protected int trackColor;
  protected float sweepDegrees = DEFAULT_RSB_SWEEP_DEGREES;

  protected int maxValue = 10;

  protected RSBRenderer(Context context) {
    thumbColor = context.getColor(R.color.rsb_default_color);
    trackColor = context.getColor(R.color.rsb_track_color);

    float stroke = context.getResources().getDimension(R.dimen.rsb_stroke);
    thumbPaint.setStrokeWidth(stroke);
    trackPaint.setStrokeWidth(stroke);
    outsidePadding =
        context.getResources().getDimension(R.dimen.rsb_outside_padding) + (stroke / 2);

    trackPaint.setStrokeCap(Cap.ROUND);
    trackPaint.setStyle(Style.STROKE);
    trackPaint.setAntiAlias(true);
    thumbPaint.setStrokeCap(Cap.ROUND);
    thumbPaint.setStyle(Style.STROKE);
    thumbPaint.setAntiAlias(true);

    thumbPaint.setColor(thumbColor);
    trackPaint.setColor(trackColor);
  }

  protected abstract float getThumbBottomDegrees(float currentValue);

  protected abstract float getThumbSweepDegrees(float currentValue);

  protected abstract void onMaxValueChanged();

  public void render(Canvas canvas, float currentValue) {
    rect.set(
        outsidePadding,
        outsidePadding,
        canvas.getWidth() - outsidePadding,
        canvas.getHeight() - outsidePadding);

    canvas.drawArc(rect, -(sweepDegrees / 2), sweepDegrees, false, trackPaint);
    canvas.drawArc(
        rect,
        getThumbBottomDegrees(currentValue),
        -getThumbSweepDegrees(currentValue),
        false,
        thumbPaint);
  }

  public void setMaxValue(int value) {
    maxValue = value;
    onMaxValueChanged();
  }

  public void setThumbColor(int color) {
    if (thumbColor != color) {
      thumbColor = color;
      thumbPaint.setColor(color);
    }
  }

  public void setTrackColor(int color) {
    if (trackColor != color) {
      trackColor = color;
      trackPaint.setColor(color);
    }
  }
}
