package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils.lerp;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.graphics.Point;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.RecyclerView.SmoothScroller;
import android.util.DisplayMetrics;
import android.view.View;
import androidx.core.math.MathUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This is a {@link LinearSnapHelper} that keeps items centered within the {@link
 * CenteredRecyclerView}'s area.
 *
 * <p>It adjusts the items' positions based on their {@code ItemDecoration}s so that they remain
 * properly centered and it works around an {@code LinearSnapHelper} issue where the STATE_IDLE
 * scroll-state is sent twice when the user is not flinging the items in a {@code RecyclerView}.
 */
final class CenteredLinearSnapHelper extends LinearSnapHelper {

  /** The maximum number of items in a small list. */
  private static final int MAX_ITEMS_IN_SMALL_LIST = 10;

  /** The minimum number of items in a large list. */
  private static final int MIN_ITEMS_IN_LARGE_LIST = 100;

  /** Multiples of a screen-size used to determine the max flinging distance for small lists. */
  private static final int SMALL_LIST_MULTIPLIER = 4;

  /** Multiples of a screen-size used to determine the max flinging distance for large lists. */
  private static final int LARGE_LIST_MULTIPLIER = 8;

  /** For slowing down or speeding up the {@link SmoothScroller}'s scrolling speed. */
  private final float scrollSpeedFactor;

  /** Determines the amount of the {@link SmoothScroller}'s scrolling deceleration. */
  private final float scrollDecelerationFactor;

  /**
   * Determines if the {@code SnapHelper} sends a <em>second</em> STATE_IDLE event after it has
   * settled. This is always true except in the case of a fling or a programmatic smooth-scroll.
   */
  boolean mustWaitForSecondStateIdleEvent = true;

  private @Nullable RecyclerView recyclerView;

  /** Communicates the scroll-distance to the {@link SmoothScroller} */
  private int scrollDistance = 0;

  public CenteredLinearSnapHelper(float scrollSpeedFactor, float scrollDecelerationFactor) {
    this.scrollSpeedFactor = scrollSpeedFactor;
    this.scrollDecelerationFactor = scrollDecelerationFactor;
  }

  @Override
  public int[] calculateDistanceToFinalSnap(LayoutManager layoutManager, View view) {
    int[] distances = super.calculateDistanceToFinalSnap(layoutManager, view);

    if (distances != null) {
      Point offset = RecyclerViewUtils.getOffsetToKeepChildCentered(layoutManager, view);
      distances[0] += offset.x;
      distances[1] += offset.y;
    }

    return distances;
  }

  @Override
  public boolean onFling(int velocityX, int velocityY) {
    // When flung, the SnapHelper does *not* send a second STATE_IDLE scroll-event.
    mustWaitForSecondStateIdleEvent = false;
    return super.onFling(velocityX, velocityY);
  }

  @Override
  public void attachToRecyclerView(@Nullable RecyclerView recyclerView) {
    super.attachToRecyclerView(recyclerView);

    this.recyclerView = recyclerView;
  }

  @Override
  public int[] calculateScrollDistance(int velocityX, int velocityY) {
    int[] distances = super.calculateScrollDistance(velocityX, velocityY);

    RecyclerView recyclerView = this.recyclerView;
    if (recyclerView != null) {
      // Clamp the return distances to the maximum scroll-range of the Recycler View based on the
      // screen-size and the total number of items in the recycler-view.
      int maxDistance = getMaxScrollingDistance(recyclerView);
      int maxRangeX = min(maxDistance, recyclerView.computeHorizontalScrollRange());
      int maxRangeY = min(maxDistance, recyclerView.computeVerticalScrollRange());

      distances[0] = MathUtils.clamp(distances[0], -maxRangeX, maxRangeX);
      distances[1] = MathUtils.clamp(distances[1], -maxRangeY, maxRangeY);
    }

    // First estimate at the scroll-distance that will be provided to the SmoothScroller.
    scrollDistance = max(abs(distances[0]), abs(distances[1]));

    return distances;
  }

  @Override
  public int findTargetSnapPosition(LayoutManager layoutManager, int velocityX, int velocityY) {
    int newPosition = super.findTargetSnapPosition(layoutManager, velocityX, velocityY);
    RecyclerView recyclerView = this.recyclerView;
    int itemCount = layoutManager.getItemCount();
    if (recyclerView == null || itemCount == 0) {
      return newPosition;
    }

    View currentView = findSnapView(layoutManager);
    if (currentView == null) {
      return newPosition;
    }

    int currentPosition = layoutManager.getPosition(currentView);
    if (currentPosition == RecyclerView.NO_POSITION) {
      return newPosition;
    }

    int listSize;
    if (layoutManager.canScrollVertically()) {
      listSize = recyclerView.computeVerticalScrollRange();
    } else if (layoutManager.canScrollHorizontally()) {
      listSize = recyclerView.computeHorizontalScrollRange();
    } else {
      listSize = 0;
    }

    // Improved estimate at the scroll-distance that will be provided to the SmoothScroller.
    scrollDistance = (listSize / itemCount) * abs(newPosition - currentPosition);

    return newPosition;
  }

  @Override
  protected @Nullable SmoothScroller createScroller(LayoutManager layoutManager) {
    if (recyclerView == null) {
      return null;
    }

    return new CenteredSnapScroller(
        recyclerView.getContext(),
        scrollSpeedFactor,
        scrollDecelerationFactor,
        view -> calculateDistanceToFinalSnap(layoutManager, view),
        () -> scrollDistance);
  }

  private static int getMaxScrollingDistance(RecyclerView recyclerView) {
    Adapter<?> adapter = recyclerView.getAdapter();
    if (adapter == null) {
      return 0;
    }

    DisplayMetrics displayMetrics = recyclerView.getResources().getDisplayMetrics();
    int screenSize = (displayMetrics.widthPixels + displayMetrics.heightPixels) / 2;
    return screenSize * getScreenSizeMultiplier(adapter.getItemCount());
  }

  private static int getScreenSizeMultiplier(int numberOfItems) {
    if (numberOfItems <= MAX_ITEMS_IN_SMALL_LIST) {
      return SMALL_LIST_MULTIPLIER;
    } else if (numberOfItems >= MIN_ITEMS_IN_LARGE_LIST) {
      return LARGE_LIST_MULTIPLIER;
    } else {
      float range = MIN_ITEMS_IN_LARGE_LIST - MAX_ITEMS_IN_SMALL_LIST;
      float fraction = (float) (numberOfItems - MAX_ITEMS_IN_SMALL_LIST) / range;
      return (int) lerp(SMALL_LIST_MULTIPLIER, LARGE_LIST_MULTIPLIER, fraction);
    }
  }
}
