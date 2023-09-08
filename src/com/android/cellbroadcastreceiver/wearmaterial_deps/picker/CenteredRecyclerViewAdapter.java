package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnTouchListener;
import androidx.annotation.VisibleForTesting;
import com.google.android.clockwork.common.wearable.wearmaterial.rotaryinput.RotaryInputHapticsHelper;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This must the base class of any adapter for a {@link CenteredRecyclerView}.
 *
 * @param <V> The type of the {@code ViewHolder}s that will be created by this Adapter
 */
public abstract class CenteredRecyclerViewAdapter<V extends ViewHolder>
    extends RecyclerView.Adapter<V> implements OnGenericMotionListener, OnTouchListener {

  private static final int UNKNOWN_SCROLL_STATE = -1;

  /**
   * Threshold delta from center within which the item is determined to have reached the center.
   *
   * <p>This value should be small.
   */
  private static final int CENTER_ITEM_DELTA_PX = 2;

  /**
   * Remembers the current scroll-state of the {@link CenteredRecyclerView} to which it is attached.
   */
  private int currentScrollState = UNKNOWN_SCROLL_STATE;

  /** Remembers the index of the item targeted to become highlighted (and centered). */
  private int targetHighlightedItemIndex = NO_POSITION;

  /** Remembers the index of the currently highlighted (and centered) item. */
  private int visibleHighlightedItemIndex = NO_POSITION;

  /** Remembers the index of the last item that triggered the haptic. */
  private int lastHapticTriggerItemIndex = NO_POSITION;

  /** Remembers if the last input was a rotary input. */
  private boolean isLastInputRotary = false;

  /** Remembers the current scroll direction via rotary input. */
  private int currentRotaryScrollDirection = 0;

  /** Listens to scroll-state changes. */
  // Suppression needed to correct linter: Listener is called later, not during construction.
  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  final OnScrollListener onScrollListener =
      new OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
          CenteredRecyclerViewAdapter.this.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
          CenteredRecyclerViewAdapter.this.onScrolled(recyclerView, dx, dy);
        }
      };

  private @Nullable RotaryInputHapticsHelper hapticsHelper;

  /** Returns the index of the highlighted (and centered) item. */
  protected int getHighlightedItemIndex() {
    return targetHighlightedItemIndex;
  }

  /**
   * Changes the {@link #targetHighlightedItemIndex} and returns true if the new value is not yet
   * equal to the {@link #visibleHighlightedItemIndex}.
   */
  boolean setTargetHighlightedItemIndex(int index) {
    targetHighlightedItemIndex = index;
    return visibleHighlightedItemIndex != index;
  }

  /** Changes the {@link #visibleHighlightedItemIndex} and returns true if its value has changed. */
  
  boolean setVisibleHighlightedItemIndex(int index) {
    if (visibleHighlightedItemIndex != index) {
      visibleHighlightedItemIndex = index;
      lastHapticTriggerItemIndex = index;
      return true;
    }
    return false;
  }

  /** Notifies the adapter that any ongoing series of scroll-state changes has been cancelled. */
  void cancelScroll() {
    currentScrollState = SCROLL_STATE_IDLE;
  }

  /** Clears the state of this adapter. */
  void clear() {
    currentScrollState = UNKNOWN_SCROLL_STATE;
    targetHighlightedItemIndex = NO_POSITION;
    visibleHighlightedItemIndex = NO_POSITION;
    lastHapticTriggerItemIndex = NO_POSITION;
  }

  void setHapticsHelper(@Nullable RotaryInputHapticsHelper hapticsHelper) {
    this.hapticsHelper = hapticsHelper;
  }

  private void onScrollStateChanged(RecyclerView recyclerView, int newState) {
    CenteredRecyclerView centeredRecyclerView = (CenteredRecyclerView) recyclerView;
    boolean mustWaitForSecondIdleEvent = centeredRecyclerView.mustWaitForSecondStateIdleEvent();

    boolean scrollingHasStopped = false;

    if (newState != currentScrollState) {
      if (currentScrollState == SCROLL_STATE_DRAGGING && newState == SCROLL_STATE_IDLE) {
        scrollingHasStopped = true;
      }

      currentScrollState = newState;

      if (!mustWaitForSecondIdleEvent && newState == SCROLL_STATE_IDLE) {
        scrollingHasStopped = true;
      }
    } else if (mustWaitForSecondIdleEvent && newState == SCROLL_STATE_IDLE) {
      // In this case, the 'newState == currentScrollState == SCROLL_STATE_IDLE'.
      // This happens when the CenteredRecyclerView's 'snapHelper' must settle after the user
      // has released their finger without doing a fling.
      scrollingHasStopped = true;
    }

    if (scrollingHasStopped) {
      determineHighlightedItem(centeredRecyclerView);
    }

    if (newState == SCROLL_STATE_IDLE) {
      // Reset the 'mustWaitForSecondStateIdleEvent' to its default value.
      centeredRecyclerView.setMustWaitForSecondStateIdleEvent();
    }
  }

  @Override
  public boolean onGenericMotion(View view, MotionEvent event) {
    currentRotaryScrollDirection = event.getAxisValue(MotionEvent.AXIS_SCROLL) > 0 ? -1 : 1;
    isLastInputRotary = true;
    return true;
  }

  @Override
  public boolean onTouch(View view, MotionEvent event) {
    isLastInputRotary = false;
    return true;
  }

  private void determineHighlightedItem(CenteredRecyclerView view) {
    int centeredChildIndex = determineHighlightedItemIndex(view);
    if (centeredChildIndex != NO_POSITION && centeredChildIndex != visibleHighlightedItemIndex) {
      visibleHighlightedItemIndex = centeredChildIndex;
      targetHighlightedItemIndex = centeredChildIndex;
      lastHapticTriggerItemIndex = centeredChildIndex;

      view.notifyHighlightedItemIndexChanged(centeredChildIndex);
    }
  }

  @VisibleForTesting
  int determineHighlightedItemIndex(RecyclerView view) {
    View listItemAtCenter = getListItemAtCenter(view);
    return listItemAtCenter != null ? view.getChildAdapterPosition(listItemAtCenter) : NO_POSITION;
  }

  private @Nullable View getListItemAtCenter(RecyclerView view) {
    int childCount = view.getChildCount();
    if (childCount == 0) {
      return null;
    }
    View child = view.findChildViewUnder(view.getWidth() / 2f, view.getHeight() / 2f);
    return child != null && child.isAttachedToWindow() ? child : null;
  }

  private void onScrolled(RecyclerView view, int dx, int dy) {
    maybeTriggerHaptic(view, dx, dy);
  }

  @SuppressWarnings("unused") // Including unused dx for readability.
  private void maybeTriggerHaptic(RecyclerView view, int dx, int dy) {
    RotaryInputHapticsHelper hapticsHelper = this.hapticsHelper;
    if (hapticsHelper == null || !isLastInputRotary) {
      return;
    }

    View centerItem = getListItemAtCenter(view);
    if (centerItem == null) {
      return;
    }
    int centerItemIndex = view.getChildAdapterPosition(centerItem);
    if (centerItemIndex == NO_POSITION) {
      return;
    }

    boolean isHorizontal = dy == 0;

    boolean hasReachedCenter =
        hasItemReachedListCenter(view, centerItem, isHorizontal, currentRotaryScrollDirection);

    if (lastHapticTriggerItemIndex != centerItemIndex && hasReachedCenter) {
      hapticsHelper.triggerClickHaptic();
      lastHapticTriggerItemIndex = centerItemIndex;
    }
  }

  private boolean hasItemReachedListCenter(
      RecyclerView view, View item, boolean isHorizontal, int scrollDirection) {
    int listCenter = (isHorizontal ? view.getWidth() : view.getHeight()) / 2;
    int itemCenter =
        (isHorizontal ? (item.getLeft() + item.getRight()) : (item.getTop() + item.getBottom()))
            / 2;

    // Relax the calculation of item reaching center with a tiny threshold. This is especially
    // needed for when list settles down on snapping at the end, as the last item sometimes settles
    // into a final position 1-2px away from center.
    return (scrollDirection > 0)
        ? (itemCenter <= listCenter + CENTER_ITEM_DELTA_PX)
        : (itemCenter >= listCenter - CENTER_ITEM_DELTA_PX);
  }
}
