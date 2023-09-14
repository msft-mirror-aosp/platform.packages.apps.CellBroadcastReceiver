package com.google.android.clockwork.common.wearable.wearmaterial.rsbcontrol;

import android.content.Context;

/** Render an RSB control for showing a ranged value */
public class RSBSliderRenderer extends RSBRenderer {

  private float increment = 0f;

  protected RSBSliderRenderer(Context context) {
    super(context);
  }

  @Override
  protected float getThumbBottomDegrees(float currentValue) {
    return (sweepDegrees / 2);
  }

  @Override
  protected float getThumbSweepDegrees(float currentValue) {
    return currentValue * increment;
  }

  @Override
  protected void onMaxValueChanged() {
    increment = sweepDegrees / maxValue;
  }
}
