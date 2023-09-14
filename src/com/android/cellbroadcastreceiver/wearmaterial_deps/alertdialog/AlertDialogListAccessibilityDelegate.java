package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Accessibility Delegate for a {@link WearAlertDialog} that host its View inside a {@link
 * RecyclerView}.
 *
 * <p>It makes sure that the initial accessibility focus does not land on the {@code
 * WearAlertDialog}'s Title, but on the next item instead.
 */
class AlertDialogListAccessibilityDelegate extends RecyclerViewAccessibilityDelegate {

  private final RecyclerView recyclerView;
  private boolean ignoreA11yEvents;

  public AlertDialogListAccessibilityDelegate(RecyclerView recyclerView) {
    super(recyclerView);
    this.recyclerView = recyclerView;
  }

  @Override
  public boolean onRequestSendAccessibilityEvent(
      ViewGroup host, View child, AccessibilityEvent event) {
    if (ignoreA11yEvents || event.getEventType() != TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
      return super.onRequestSendAccessibilityEvent(host, child, event);
    }

    LayoutManager layoutManager = recyclerView.getLayoutManager();
    if (layoutManager == null) {
      return super.onRequestSendAccessibilityEvent(host, child, event);
    }

    ignoreA11yEvents = true;

    ViewHolder viewHolder = recyclerView.findContainingViewHolder(child);
    if (!(viewHolder instanceof WearAlertDialogTitleElementViewHolder)) {
      return super.onRequestSendAccessibilityEvent(host, child, event);
    }

    int position = viewHolder.getBindingAdapterPosition();
    moveA11yFocus(child, layoutManager.findViewByPosition(position + 1));
    return false;
  }

  private void moveA11yFocus(View fromView, @Nullable View toView) {
    recyclerView.requestSendAccessibilityEvent(
        fromView, AccessibilityEvent.obtain(TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED));
    fromView.performAccessibilityAction(ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);

    if (toView != null) {
      recyclerView.requestSendAccessibilityEvent(
          toView, AccessibilityEvent.obtain(TYPE_VIEW_ACCESSIBILITY_FOCUSED));
      toView.performAccessibilityAction(ACTION_ACCESSIBILITY_FOCUS, null);
    }
  }
}
