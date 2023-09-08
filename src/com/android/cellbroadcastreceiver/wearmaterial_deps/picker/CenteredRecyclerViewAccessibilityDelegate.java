package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_HOVER_ENTER;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK;
import static com.google.android.clockwork.common.wearable.wearmaterial.util.TypedArrayUtils.getStringAttr;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Class that handles the special accessibility functionality for a {@link CenteredRecyclerView}.
 *
 * <p>This accessibility delegate makes sure that the accessibility-focus and the highlighted
 * item-view are in sync. Item-views that are not the currently highlighted item will never get the
 * accessibility-focus.
 */
final class CenteredRecyclerViewAccessibilityDelegate extends RecyclerViewAccessibilityDelegate {

  private static final int DIRECTION_BACKWARD = -1;
  private static final int DIRECTION_FORWARD = 1;
  private static final long IDLE_RETRY_DELAY_MILLIS = 100;

  private final CenteredRecyclerView recyclerView;

  @SuppressWarnings("methodref.receiver.bound.invalid")
  private final Runnable scrollAndFocusOnItemRunnable = this::scrollAndFocusOnItem;

  @SuppressWarnings("methodref.receiver.bound.invalid")
  private final Runnable clearFocusRunnable = this::clearFocus;

  private @MonotonicNonNull AccessibilityDelegateCompat itemDelegate;

  private boolean hasFocus;
  private int currentHighlightedPosition = -1;
  private int scrollState = RecyclerView.SCROLL_STATE_IDLE;
  private CharSequence itemActionAnnouncement;

  @SuppressWarnings({"method.invocation", "methodref.receiver.bound.invalid"})
  CenteredRecyclerViewAccessibilityDelegate(CenteredRecyclerView recyclerView) {
    super(recyclerView);
    this.recyclerView = recyclerView;

    setItemActionAnnouncement(
        getStringAttr(
            recyclerView.getContext().getTheme(), R.attr.accessibilityActionForItemSelection));

    recyclerView.addOnHighlightedItemIndexChangedListener(this::setA11yFocus);
    recyclerView.addOnScrollListener(
        new OnScrollListener() {
          @Override
          public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            scrollState = newState;
          }
        });
  }

  private void setItemActionAnnouncement(@Nullable CharSequence announcement) {
    itemActionAnnouncement =
        announcement == null
            ? recyclerView.getResources().getString(R.string.wear_picker_a11y_action_select_item)
            : announcement;
  }

  private void setA11yFocus(int position) {
    recyclerView.removeCallbacks(scrollAndFocusOnItemRunnable);
    currentHighlightedPosition = position;

    if (!hasFocus) {
      return;
    }

    LayoutManager layoutManager = recyclerView.getLayoutManager();
    if (!(layoutManager instanceof LinearLayoutManager)) {
      return;
    }

    ViewHolder highlightedViewHolder = recyclerView.findViewHolderForAdapterPosition(position);
    View highlightedView = highlightedViewHolder == null ? null : highlightedViewHolder.itemView;
    if (highlightedView == null || !highlightedView.isImportantForAccessibility()) {
      return;
    }

    highlightedView.performAccessibilityAction(ACTION_ACCESSIBILITY_FOCUS, null);
  }

  // For the 'super.performAccessibilityAction(host, action, args)':
  // TAP Pre-submit requires '@Nullable Bundle args' and yet doesn't accept a nullable 'args' value
  // in the 'default:' clause... Adding this SuppressWarnings to suppress this contradiction.
  @SuppressWarnings("argument.type.incompatible")
  @Override
  public boolean performAccessibilityAction(View host, int action, @Nullable Bundle args) {
    switch (action) {
      case ACTION_SCROLL_BACKWARD:
        return moveHighlightedIndex(DIRECTION_BACKWARD);

      case ACTION_SCROLL_FORWARD:
        return moveHighlightedIndex(DIRECTION_FORWARD);

      default:
        return super.performAccessibilityAction(host, action, args);
    }
  }

  /**
   * Moves the focus from the currently highlighted item to the next ({@code direction=1}) or the
   * previous one ({@code direction=1}) and returns true only when this move was performed
   * successfully without performing another accessibility-action.
   */
  private boolean moveHighlightedIndex(int direction) {
    int highlightedPosition = recyclerView.getHighlightedItemIndex();
    if (highlightedPosition == RecyclerView.NO_POSITION) {
      return false;
    }

    int nextViewIndex = highlightedPosition + direction;

    if (hasFocus) {
      ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(nextViewIndex);
      if (viewHolder != null) {
        viewHolder.itemView.performAccessibilityAction(ACTION_ACCESSIBILITY_FOCUS, null);
      }
      return false;
    } else {
      recyclerView.setHighlightedItemIndex(nextViewIndex);
      return true;
    }
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
    super.onInitializeAccessibilityNodeInfo(host, info);

    LayoutManager layoutManager = recyclerView.getLayoutManager();
    if (layoutManager instanceof LinearLayoutManager) {
      setCollectionInfoAndActions((LinearLayoutManager) layoutManager, info);
    }
  }

  private void setCollectionInfoAndActions(
      LinearLayoutManager layoutManager, AccessibilityNodeInfoCompat info) {

    boolean isHorizontal = layoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL;

    int itemCount = layoutManager.getItemCount();
    int rows = isHorizontal ? 1 : itemCount;
    int columns = isHorizontal ? itemCount : 1;
    info.setCollectionInfo(
        CollectionInfoCompat.obtain(
            rows, columns, false, CollectionInfoCompat.SELECTION_MODE_SINGLE));

    int highlightedPosition = recyclerView.getHighlightedItemIndex();
    info.removeAction(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
    info.removeAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
    if (highlightedPosition > 0) {
      info.addAction(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
    }
    if (highlightedPosition >= 0 && highlightedPosition < layoutManager.getItemCount() - 1) {
      info.addAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
    }
  }

  @Override
  public AccessibilityDelegateCompat getItemDelegate() {
    if (itemDelegate == null) {
      // This must be done lazily and can't be done in the constructor, since this method
      // will be called during the constructor's call to 'super(recyclerView)'.
      itemDelegate = new ItemDelegate();
    }
    return itemDelegate;
  }

  @SuppressLint("SwitchIntDef")
  @Override
  public boolean onRequestSendAccessibilityEvent(
      ViewGroup viewGroup, View view, AccessibilityEvent event) {
    int viewPosition = recyclerView.getChildAdapterPosition(view);
    int highlightedPosition = recyclerView.getHighlightedItemIndex();

    int eventType = event.getEventType();
    switch (eventType) {
      case TYPE_VIEW_HOVER_ENTER:
        return viewPosition == highlightedPosition;

      case TYPE_VIEW_ACCESSIBILITY_FOCUSED:
        recyclerView.removeCallbacks(clearFocusRunnable);

        if (scrollState != RecyclerView.SCROLL_STATE_IDLE) {
          hasFocus = true;
          return true;
        }

        if (viewPosition == RecyclerView.NO_POSITION
            || highlightedPosition == RecyclerView.NO_POSITION) {
          hasFocus = true;
          return false;
        }

        if (viewPosition == highlightedPosition) {
          currentHighlightedPosition = highlightedPosition;
          hasFocus = true;
          return true;
        }

        boolean retVal = hasFocus;
        scrollAndFocusOnItemWhenIdle(hasFocus ? viewPosition : highlightedPosition);
        hasFocus = true;
        return retVal;

      case TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED:
        clearFocusWhenIdle();
        return true;

      default:
        return true;
    }
  }

  private void clearFocusWhenIdle() {
    recyclerView.removeCallbacks(clearFocusRunnable);
    recyclerView.post(clearFocusRunnable);
  }

  private void clearFocus() {
    if (!hasFocus) {
      return;
    }

    if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
      hasFocus = false;
    } else {
      recyclerView.postDelayed(clearFocusRunnable, IDLE_RETRY_DELAY_MILLIS);
    }
  }

  private void scrollAndFocusOnItemWhenIdle(int itemPosition) {
    currentHighlightedPosition = itemPosition;
    recyclerView.removeCallbacks(scrollAndFocusOnItemRunnable);
    recyclerView.post(scrollAndFocusOnItemRunnable);
  }

  private void scrollAndFocusOnItem() {
    if (scrollState != RecyclerView.SCROLL_STATE_IDLE) {
      recyclerView.postDelayed(scrollAndFocusOnItemRunnable, IDLE_RETRY_DELAY_MILLIS);
      return;
    }

    int highlightedPosition = recyclerView.getHighlightedItemIndex();
    if (currentHighlightedPosition >= 0) {
      if (currentHighlightedPosition != highlightedPosition) {
        recyclerView.setHighlightedItemIndex(currentHighlightedPosition);
      } else {
        setA11yFocus(currentHighlightedPosition);
      }
    }
  }

  private final class ItemDelegate extends RecyclerViewAccessibilityDelegate.ItemDelegate {

    ItemDelegate() {
      super(CenteredRecyclerViewAccessibilityDelegate.this);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
      super.onInitializeAccessibilityNodeInfo(host, info);

      info.setClickable(true);
      info.setLongClickable(false);
      info.removeAction(ACTION_CLICK);
      info.addAction(new AccessibilityActionCompat(ACTION_CLICK.getId(), itemActionAnnouncement));
    }

    // For the 'super.performAccessibilityAction(host, action, args)':
    // TAP PreSubmit requires '@Nullable Bundle args' and yet doesn't accept a nullable 'args' value
    // in the call to 'super'. Adding this SuppressWarnings to suppress this contradiction.
    @SuppressWarnings("argument.type.incompatible")
    @Override
    public boolean performAccessibilityAction(View host, int action, @Nullable Bundle args) {
      if (action == AccessibilityNodeInfoCompat.ACTION_CLICK) {
        recyclerView.notifyItemA11ySelected();
        return true;
      }
      return super.performAccessibilityAction(host, action, args);
    }
  }
}
