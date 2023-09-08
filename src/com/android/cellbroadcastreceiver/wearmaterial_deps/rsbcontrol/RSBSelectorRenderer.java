package com.google.android.clockwork.common.wearable.wearmaterial.rsbcontrol;

import static java.lang.Math.max;

import android.content.Context;

/** Render an RSB control for selecting elements form a list */
public class RSBSelectorRenderer extends RSBRenderer {

  // Minimum thumb size in degrees, 10% of the total sweep
  private static final float MIN_THUMB_SIZE_RATIO = 0.1f;

  private float thumbSweep = 0;

  protected RSBSelectorRenderer(Context context) {
    super(context);
  }

  @Override
  protected float getThumbBottomDegrees(float currentValue) {
    float innerSweep = (sweepDegrees - thumbSweep) / maxValue;
    return (sweepDegrees / 2) - (innerSweep * currentValue);
  }

  @Override
  protected float getThumbSweepDegrees(float currentValue) {
    return thumbSweep;
  }

  @Override
  protected void onMaxValueChanged() {
    thumbSweep = max(sweepDegrees / (maxValue + 1), MIN_THUMB_SIZE_RATIO * sweepDegrees);
  }
}
