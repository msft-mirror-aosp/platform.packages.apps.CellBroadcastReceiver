package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

/**
 * Measures the necessary padding to make sure the first and last items can be centered in the
 * {@code CenteredRecyclerView}'s area.
 */
interface PaddingMeasurements {

  /** Returns either the translationX or translationY property of the {@code view}. */
  float getTranslation(View view);

  /** Returns either the height or width of a given {@code view}. */
  int getSize(View view);

  /** Returns the coordinate of the center of the given {@code view}. */
  int getCenter(View view);

  /** Returns amount of padding currently applied to the given {@code view}. */
  int getPaddingSize(View view);

  /** Changes the amount of padding of the given {@code view} to a new {@code paddingSize}. */
  void setPaddingSize(View view, int paddingSize);

  /**
   * Offsets all child views attached to the {@code view} by the given {@code offset}-number of
   * pixels.
   */
  void offsetChildren(RecyclerView view, int offset);

  /** Returns true if the given {@code view} needs its items to be centered, false otherwise. */
  boolean needsCenteredPadding(RecyclerView view);

  /**
   * Returns true if the view's {@link LayoutParams} is {@link LayoutParams#MATCH_PARENT} or not.
   */
  boolean isMatchingParent(View view);

  /**
   * Returns the desired padding needed to be able to center the list-items in the center of the
   * {@code view}.
   *
   * @param paddingMeasurements The padding measurements
   * @param view The RecyclerView
   * @param isCentered If true, always return the center of the {@code view}, otherwise the distance
   *     of the children's bounding-box from the {@code view}'s left- or top-edge.
   * @return The padding needed on each side of the RecyclerView
   */
  static int getDesiredPadding(
      PaddingMeasurements paddingMeasurements, RecyclerView view, boolean isCentered) {
    int childCount = view.getChildCount();

    // If the list-items have a layout that needs to match the parent, i.e. they need to be
    // as high/wide as the recycler-view itself, no padding is needed.
    if (childCount > 0 && paddingMeasurements.isMatchingParent(view.getChildAt(0))) {
      return 0;
    }

    if (isCentered) {
      return paddingMeasurements.getSize(view) / 2;
    }

    if (childCount == 0) {
      return 0;
    }

    int minLeft = Integer.MAX_VALUE;
    int maxRight = -Integer.MAX_VALUE;
    for (int i = 0; i < childCount; i++) {
      View child = view.getChildAt(i);
      int center = paddingMeasurements.getCenter(child);
      int halfSize = paddingMeasurements.getSize(child) / 2;

      minLeft = min(minLeft, center - halfSize);
      maxRight = max(maxRight, center + halfSize);
    }

    int boundingSize = maxRight - minLeft;
    return (paddingMeasurements.getSize(view) - boundingSize) / 2;
  }

  /**
   * Returns true if the provided {@code view} provides scrolling behavior. This is provided when
   * its content does not fit in its allocated area.
   */
  static boolean canScroll(PaddingMeasurements paddingMeasurements, RecyclerView view) {
    return (view.getChildCount() >= 1 && paddingMeasurements.needsCenteredPadding(view));
  }

  /**
   * If needed, this method scrolls all the children of the {@code view} so that {@code
   * childInCenter} is centered. It returns {@code true} if some scrolling was needed, {@code false}
   * otherwise.
   */
  static boolean offsetChildrenToEnsureCentering(
      PaddingMeasurements paddingMeasurements, RecyclerView view, View childInCenter) {
    int viewCenter = paddingMeasurements.getSize(view) / 2;
    int childCenter = paddingMeasurements.getCenter(childInCenter);
    int offset = viewCenter - childCenter;
    if (offset != 0) {
      paddingMeasurements.offsetChildren(view, offset);
      return true;
    }
    return false;
  }

  /**
   * Prepares the given RecyclerView to handle either vertical or horizontal scrolling and returns
   * the correct {@link PaddingMeasurements} implementation to handle these measurements.
   *
   * @param view The RecyclerView that will be prepared
   * @param direction The {@link RecyclerView.Orientation} for which to prepare the RecyclerView.
   * @return A {@link VerticalPaddingMeasurements} or {@link HorizontalPaddingMeasurements}
   *     depending on the provided direction
   */
  static PaddingMeasurements getAndPreparePaddingMeasurements(RecyclerView view, int direction) {
    Context context = view.getContext();
    view.setLayoutManager(new CenteredLinearLayoutManager(context, direction));

    if (direction == RecyclerView.HORIZONTAL) {
      return new HorizontalPaddingMeasurements();
    } else {
      return new VerticalPaddingMeasurements();
    }
  }
}
