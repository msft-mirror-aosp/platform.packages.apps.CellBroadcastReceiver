package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static java.lang.Math.abs;
import static java.lang.Math.max;

import android.content.Context;
import android.graphics.PointF;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link CenteredSmoothScroller} that snaps back to the center on a fling and, if the first or
 * last items were targeted, snaps back to the first or last item with a bounce.
 */
final class CenteredSnapScroller extends CenteredSmoothScroller {

  private static final int TARGET_SEEK_SCROLL_DISTANCE_PX = 10000;
  private static final float DECELERATION_STRENGTH = 2;

  private final DecelerateWithSpeedInterpolator interimDecelerateInterpolator;
  private final Interpolator targetDecelerateInterpolator;

  @SuppressWarnings("Guava")
  private final Function<View, int[]> getSnapDistances;

  @SuppressWarnings("Guava")
  private final Supplier<Integer> getScrollDistance;

  @SuppressWarnings("Guava")
  public CenteredSnapScroller(
      Context context,
      float scrollSpeedFactor,
      float scrollDecelerationFactor,
      Function<View, int[]> getSnapDistances,
      Supplier<Integer> getScrollDistance) {
    super(context, scrollSpeedFactor);

    float decelerationFactor = max(scrollDecelerationFactor, 0.5f);
    interimDecelerateInterpolator =
        new DecelerateWithSpeedInterpolator(DECELERATION_STRENGTH * decelerationFactor);
    targetDecelerateInterpolator = new DecelerateInterpolator();

    this.getSnapDistances = getSnapDistances;
    this.getScrollDistance = getScrollDistance;
  }

  @Override
  protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
    PointF targetVector = getTargetVector();
    if (targetVector == null || (targetVector.x == 0 && targetVector.y == 0)) {
      return;
    }

    int[] snapDistances = getSnapDistances.apply(targetView);
    if (snapDistances == null) {
      return;
    }

    int dx = snapDistances[0];
    int dy = snapDistances[1];

    int distance = max(abs(dx), abs(dy));
    int totalTime = calculateTimeForDeceleration(distance);
    if (totalTime > 0) {
      action.update(dx, dy, totalTime, targetDecelerateInterpolator);
    }
  }

  @Override
  protected int calculateTimeForDeceleration(int dx) {
    return (int) (dx / interimDecelerateInterpolator.getCurrentSpeed());
  }

  @SuppressWarnings("nullness:assignment") // mTargetVector is nullable.
  @Override
  protected void updateActionForInterimTarget(Action action) {
    // This implementation is copied from the LinearSmoothScroller and modified to use a
    // smaller scroll-distance than TARGET_SEEK_SCROLL_DISTANCE_PX. This improves the deceleration
    // of the scrolling towards the end.
    mTargetVector = null;
    PointF targetVector = getTargetVector();
    if (targetVector == null) {
      final int target = getTargetPosition();
      action.jumpTo(target);
      stop();
      return;
    }

    int scrollDistance = getScrollDistance.get();
    if (scrollDistance == 0) {
      scrollDistance = TARGET_SEEK_SCROLL_DISTANCE_PX;
    }

    mInterimTargetDx = (int) (scrollDistance * targetVector.x);
    mInterimTargetDy = (int) (scrollDistance * targetVector.y);
    int scrollTime = calculateTimeForScrolling(max(abs(mInterimTargetDx), abs(mInterimTargetDy)));

    // To avoid UI hiccups, trigger a smooth scroll to a distance little further than the
    // interim target. Since we track the distance travelled in onSeekTargetStep callback, it
    // won't actually scroll more than what we need.
    action.update(mInterimTargetDx, mInterimTargetDy, scrollTime, interimDecelerateInterpolator);
  }

  private @Nullable PointF getTargetVector() {
    if (mTargetVector == null) {
      PointF scrollVector = computeScrollVectorForPosition(getTargetPosition());
      if (scrollVector != null && (scrollVector.x != 0 || scrollVector.y != 0)) {
        normalize(scrollVector);
        mTargetVector = scrollVector;
      }
    }

    return mTargetVector;
  }
}
