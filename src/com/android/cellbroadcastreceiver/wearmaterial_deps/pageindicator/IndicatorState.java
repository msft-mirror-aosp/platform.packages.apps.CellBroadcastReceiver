package com.google.android.clockwork.common.wearable.wearmaterial.pageindicator;

import static com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils.clamp;
import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.StrictMath.ceil;

/**
 * Calculates and stores state of the page indicator in terms of page positions. Shift center point
 * depending on current and previous state, attempt to keep current page position at most
 * maxPageToCenterDistance away from center point when possible.
 *
 * <p>Indicator dot positions are defined using:
 *
 * <ol>
 *   <li>Range of page numbers represented by visible indicator dots, including transition dots that
 *       are potentially fully faded out.
 *   <li>Center page position (can be fractional) to align the rendered dots to, since the indicator
 *       dots are not always centered at the page position depending on the previous center
 *       position.
 * </ol>
 */
final class IndicatorState {

  private enum SnapDirection {
    LEFT,
    RIGHT
  };

  private final int maxVisibleIndicators;
  private final float maxPageToCenterDistance;

  private boolean fullPageRangeShown;
  private int pageCount;
  private int pageStart;
  private int pageEnd;
  private float pagePosition;
  private float center;
  private float maxCenter;
  private float minCenter;
  private float nearestSnap;
  private float nearestLeftSnap;
  private float nearestRightSnap;
  private SnapDirection snapDirection = SnapDirection.RIGHT;

  /**
   * @param maxVisibleIndicators the maximum number of visible indicators when not mid-transition.
   * @param maxPageToCenterDistance the maximum distance the page position can be from center point
   *     for cases where maxVisibleIndicators is less than the total page count.
   */
  IndicatorState(int maxVisibleIndicators, float maxPageToCenterDistance) {
    this.maxVisibleIndicators = maxVisibleIndicators;
    this.maxPageToCenterDistance = maxPageToCenterDistance;
  }

  void setPagePosition(float pagePosition) {
    this.pagePosition = pagePosition;
  }

  void setPageCount(int pageCount) {
    this.pageCount = pageCount;
    fullPageRangeShown = this.pageCount <= maxVisibleIndicators;
    minCenter = (maxVisibleIndicators - 1) / 2f;
    maxCenter = (this.pageCount - 1) - minCenter;
  }

  void updatePagePosition() {
    pagePosition = clamp(pagePosition, 0, pageCount - 1);
    updateCenterPosition();
    updateSnapPoints();
    updateVisiblePageRange();
  }

  float getPagePosition() {
    return pagePosition;
  }

  float getCenterPagePosition() {
    return center;
  }

  int getFirstVisibleIndicator() {
    return pageStart;
  }

  int getLastVisibleIndicator() {
    return pageEnd;
  }

  boolean isFullPageRangeShown() {
    return fullPageRangeShown;
  }

  boolean shouldShowIndicator() {
    return pageCount > 1;
  }

  /**
   * Calculates center position which is the page position equivalent of where the indicator dots
   * are aligned in the center. This covers 3 cases:
   *
   * <ol>
   *   <li>If page position is too far left/right, center adjusts so that page position is at the
   *       maxPageToCenterDistance from center, effectively pulling the center indicator window to
   *       one side.
   *   <li>If page position is within maxPageToCenterDistance, that means at some point the page was
   *       backtracked. Attempt to snap towards a valid snap point based on the snap direction
   *       stored while covering case 1.
   *   <li>If page position is within maxPageToCenterDistance, and center is at a snap point, no
   *       change is needed.
   * </ol>
   */
  private void updateCenterPosition() {
    if (fullPageRangeShown) {
      center = (pageCount - 1) / 2f;
      return;
    }

    float boxLeft = center - maxPageToCenterDistance;
    float boxRight = center + maxPageToCenterDistance;
    boolean centerSnapped = center == nearestSnap;
    if (pagePosition < boxLeft) {
      center = pagePosition + maxPageToCenterDistance;
      snapDirection = SnapDirection.RIGHT;
    } else if (pagePosition > boxRight) {
      center = pagePosition - maxPageToCenterDistance;
      snapDirection = SnapDirection.LEFT;
    } else if (!centerSnapped && snapDirection == SnapDirection.LEFT) {
      center = max(pagePosition - maxPageToCenterDistance, nearestLeftSnap);
    } else if (!centerSnapped && snapDirection == SnapDirection.RIGHT) {
      center = min(pagePosition + maxPageToCenterDistance, nearestRightSnap);
    }

    center = clamp(center, minCenter, maxCenter);
  }

  /**
   * Calculate snap points for use in case of page position moving back towards center and center
   * point needs to move towards a stable position.
   */
  private void updateSnapPoints() {
    if (maxVisibleIndicators % 2 == 0) {
      int floor = (int) floor(center);
      // Snap points are between pages (x.5) in case of even indicator count.  If decimal portion of
      // center is less than X.5, then the nearest left is located at X - 0.5.  Otherwise X.5.
      nearestLeftSnap = (center - floor > 0.5f) ? floor + 0.5f : floor - 0.5f;
    } else {
      nearestLeftSnap = (float) floor(center - 0.5f);
    }
    nearestRightSnap = nearestLeftSnap + 1;
    nearestSnap =
        (abs(center - nearestLeftSnap) < abs(center - nearestRightSnap))
            ? nearestLeftSnap
            : nearestRightSnap;
  }

  private void updateVisiblePageRange() {
    if (fullPageRangeShown) {
      pageStart = 0;
      pageEnd = pageCount - 1;
      return;
    }

    // floor / ceil to include transitional dots.
    pageStart = (int) clamp((float) floor(center - maxVisibleIndicators / 2f), 0, pageCount - 1);
    pageEnd = (int) clamp((float) ceil(center + maxVisibleIndicators / 2f), 0, pageCount - 1);
  }
}
