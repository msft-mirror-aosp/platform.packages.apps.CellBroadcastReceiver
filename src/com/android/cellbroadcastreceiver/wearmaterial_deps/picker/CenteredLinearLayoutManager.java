package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.Context;
import android.os.Handler;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Recycler;
import androidx.recyclerview.widget.RecyclerView.State;
import android.view.View;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class is a {@link LinearLayoutManager} specifically targeted for the {@link
 * CenteredRecyclerView}.
 *
 * <p>It adds this behavior to the {@code LinearLayoutManager}:
 *
 * <ul>
 *   <li>After a successful layout, it makes sure the highlighted item view is properly centered.
 *   <li>A client can specify an amount of extra space that should be allocated so that off-screen
 *       list-item views are laid-out. This can improve visual quality at the expense of some
 *       performance.
 *   <li>A client can listen for when the {@code LinearLayoutManager} lays-out and moves list-item
 *       views. This will give the client a chance to transform the list-item views.
 * </ul>
 */
final class CenteredLinearLayoutManager extends LinearLayoutManager {

  /**
   * Interface for a callback to be invoked when a list-item view is repositioned (laid-out or
   * scrolled).
   *
   * <p>Use this interface for transforming list-item views during scrolling of the recycler-view.
   */
  public interface OnMovedItemViewListener {

    /**
     * Handles the event when the {@code layoutManager} lays-out or moves the {@code itemView}.
     *
     * <p>The {@code itemView} can be transformed/animated in this callback.
     */
    void onMoved(LinearLayoutManager layoutManager, View itemView);
  }

  private @Nullable OnMovedItemViewListener itemMovedListener;

  private @Nullable CenteredRecyclerView recyclerView;

  private @Nullable Handler uiHandler;

  @SuppressWarnings({"nullness:method.invocation", "nullness:methodref.receiver.bound"})
  private final Runnable ensureLayoutIsCentered = this::ensureLayoutIsCentered;

  private int extraLayoutSpace;

  public CenteredLinearLayoutManager(Context context, int direction) {
    super(context, direction, false);
  }

  @Override
  public void onLayoutCompleted(State state) {
    super.onLayoutCompleted(state);

    Handler handler = this.uiHandler;
    if (handler != null) {
      handler.removeCallbacks(ensureLayoutIsCentered);
      handler.postAtFrontOfQueue(ensureLayoutIsCentered);
    }
  }

  private void ensureLayoutIsCentered() {
    if (recyclerView != null) {
      recyclerView.ensureLayoutIsCentered();
      notifyListener();
    }
  }

  @Override
  public void onAttachedToWindow(RecyclerView recyclerView) {
    super.onAttachedToWindow(recyclerView);
    if (recyclerView instanceof CenteredRecyclerView) {
      this.recyclerView = (CenteredRecyclerView) recyclerView;
      this.uiHandler = new Handler();
    }
  }

  @Override
  public int computeVerticalScrollExtent(State state) {
    if (getChildCount() == 0 || state.getItemCount() == 0) {
      return 0;
    }

    View firstChild = getChildAt(0);
    View lastChild = getChildAt(getChildCount() - 1);

    if (firstChild == null || lastChild == null) {
      return 0;
    }

    int laidOutArea = getDecoratedBottom(lastChild) - getDecoratedTop(firstChild);
    return min(getHeight(), laidOutArea);
  }

  @Override
  public void onDetachedFromWindow(RecyclerView recyclerView, Recycler recycler) {
    super.onDetachedFromWindow(recyclerView, recycler);
    this.recyclerView = null;
    this.uiHandler = null;
  }

  @Override
  protected void calculateExtraLayoutSpace(State state, int[] extraLayoutSpace) {
    int extraLayoutSpacePx = this.extraLayoutSpace;

    // If we don't specify any extra layout amount or when we're smoothly scrolling,
    // let the default (super) implementation handle it.
    if (state.hasTargetScrollPosition()) {
      super.calculateExtraLayoutSpace(state, extraLayoutSpace);
    } else {
      // Due to possible scaling and centering, to ensure the start and end item-views are properly
      // shown, a minimum of extra-space is needed, which would be the size of the
      // centering-padding.
      int extraStart = max(extraLayoutSpacePx, max(getPaddingLeft(), getPaddingTop()));
      int extraEnd = max(extraLayoutSpacePx, max(getPaddingRight(), getPaddingBottom()));
      extraLayoutSpace[0] = extraStart;
      extraLayoutSpace[1] = extraEnd;
    }
  }

  @Override
  public int scrollHorizontallyBy(int dx, Recycler recycler, State state) {
    // Prevents over-scrolling by the default LinearLayoutManager implementation.
    // This allows the MotionEdgeEffect to handle it instead.
    dx = getAdjustedScrollDelta(dx, state);

    int scrolled = super.scrollHorizontallyBy(dx, recycler, state);
    if (getOrientation() == RecyclerView.HORIZONTAL) {
      notifyListener();
    }
    return scrolled;
  }

  @Override
  public int scrollVerticallyBy(int dy, Recycler recycler, State state) {
    // Prevents over-scrolling by the default LinearLayoutManager implementation.
    // This allows the MotionEdgeEffect to handle it instead.
    dy = getAdjustedScrollDelta(dy, state);

    int scrolled = super.scrollVerticallyBy(dy, recycler, state);
    if (getOrientation() == RecyclerView.VERTICAL) {
      notifyListener();
    }
    return scrolled;
  }

  private void notifyListener() {
    OnMovedItemViewListener listener = this.itemMovedListener;
    if (listener == null) {
      return;
    }

    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if (child != null) {
        listener.onMoved(this, child);
      }
    }
  }

  /**
   * If the caller would like to allocate extra space for layout of off-screen item views, this
   * method will instruct the RecyclerView to do so.
   *
   * <p>The {@code extraLayoutSpace} is the amount of space, in pixels, to allocate before and after
   * the visible area of the RecyclerView, so that the RecyclerView lays out off-screen item views.
   * Setting this value to {@code 0} will disable off-screen item layout.
   */
  public void setExtraLayoutSpace(int extraLayoutSpace) {
    if (this.extraLayoutSpace != extraLayoutSpace) {
      this.extraLayoutSpace = extraLayoutSpace;
      requestLayout();
    }
  }

  public void setItemMovedListener(@Nullable OnMovedItemViewListener listener) {
    itemMovedListener = listener;
  }

  /**
   * Returns the number of pixels with which a scrolling amount, provided by the {@code scrollDelta}
   * value, should be adjusted to ensure that the first item-view will scroll slower if pulled below
   * the RecyclerView's center and that the last item-view will scroll slower if pulled above the
   * RecyclerView's center.
   */
  private int getAdjustedScrollDelta(int scrollDelta, State state) {
    if (scrollDelta == 0
        || state.getItemCount() == 0
        || state.getRemainingScrollHorizontal() != 0
        || state.getRemainingScrollVertical() != 0) {
      return scrollDelta;
    }

    CenteredRecyclerView recyclerView = this.recyclerView;
    if (recyclerView == null) {
      return scrollDelta;
    }

    if (state.getItemCount() == 1) {
      return scrollDelta / 2;
    } else {
      View firstItemView = findViewByPosition(0);
      int firstItemDistance = recyclerView.getDistanceFromCenter(firstItemView) - scrollDelta;

      View lastItemView = findViewByPosition(state.getItemCount() - 1);
      int lastItemDistance = recyclerView.getDistanceFromCenter(lastItemView) - scrollDelta;

      if (firstItemView != null && firstItemDistance >= 0) {
        return scrollDelta / 2;
      } else if (lastItemView != null && lastItemDistance <= 0) {
        return scrollDelta / 2;
      } else {
        return scrollDelta;
      }
    }
  }

  @Override
  public int getRowCountForAccessibility(Recycler recycler, State state) {
    return getOrientation() == RecyclerView.VERTICAL ? state.getItemCount() : 1;
  }

  @Override
  public int getColumnCountForAccessibility(Recycler recycler, State state) {
    return getOrientation() == RecyclerView.VERTICAL ? 1 : state.getItemCount();
  }

  @Override
  public int getSelectionModeForAccessibility(Recycler recycler, State state) {
    return CollectionInfoCompat.SELECTION_MODE_SINGLE;
  }
}
