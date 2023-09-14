package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static java.lang.Math.max;

import android.content.Context;
import androidx.recyclerview.widget.LinearSmoothScroller;
import android.util.DisplayMetrics;
import android.view.View;

/**
 * This is a slightly modified {@link LinearSmoothScroller}, forcing horizontal and vertical
 * snapping to happen at {@link #SNAP_TO_ANY} and to snap scrolling around the middle of the {@code
 * RecyclerView}'s area.
 */
class CenteredSmoothScroller extends LinearSmoothScroller {

  /**
   * The default time it will take, in milliseconds, to scroll content one inch.
   *
   * @see #milliSecondsPerInch
   */
  private static final float DEFAULT_MILLISECONDS_PER_INCH = 160;

  private static final float MIN_SCROLL_SPEED_FACTOR = 0.01f;

  /** The time it will take, in milliseconds, for this Scroller to scroll content one inch. */
  private final float milliSecondsPerInch;

  public CenteredSmoothScroller(Context context, float scrollSpeedFactor) {
    super(context);

    scrollSpeedFactor = max(MIN_SCROLL_SPEED_FACTOR, scrollSpeedFactor);
    this.milliSecondsPerInch = DEFAULT_MILLISECONDS_PER_INCH / scrollSpeedFactor;
  }

  @Override
  protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
    return milliSecondsPerInch / displayMetrics.densityDpi;
  }

  @Override
  protected int getHorizontalSnapPreference() {
    return SNAP_TO_ANY;
  }

  @Override
  protected int getVerticalSnapPreference() {
    return SNAP_TO_ANY;
  }

  @Override
  public int calculateDxToMakeVisible(View view, int snapPreference) {
    int dx = super.calculateDxToMakeVisible(view, snapPreference);
    if (snapPreference == SNAP_TO_ANY) {
      dx -= RecyclerViewUtils.getOffsetToKeepChildCentered(getLayoutManager(), view).x;
    }
    return dx;
  }

  @Override
  public int calculateDyToMakeVisible(View view, int snapPreference) {
    int dy = super.calculateDyToMakeVisible(view, snapPreference);
    if (snapPreference == SNAP_TO_ANY) {
      dy -= RecyclerViewUtils.getOffsetToKeepChildCentered(getLayoutManager(), view).y;
    }
    return dy;
  }

  @Override
  public int calculateDtToFit(
      int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {

    if (snapPreference == SNAP_TO_ANY) {
      // The view-target is the center of the box.
      return ((boxStart + boxEnd) - (viewStart + viewEnd)) / 2;
    }

    return super.calculateDtToFit(viewStart, viewEnd, boxStart, boxEnd, snapPreference);
  }
}
