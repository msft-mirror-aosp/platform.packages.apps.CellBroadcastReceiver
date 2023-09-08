package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import android.graphics.Point;
import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import android.view.View;
import androidx.core.util.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Provides a set of static utility methods that deal with {@link RecyclerView}s. */
public abstract class RecyclerViewUtils {

  private RecyclerViewUtils() {}

  /**
   * Calls the {@link Consumer#accept(Object)} with the {@link RecyclerView.ViewHolder} of each
   * non-recycled item of the given {@code RecyclerView} {@code view}.
   *
   * @param view The given {@link RecyclerView}
   * @param callback The callback that will be called for each of the {@code RecyclerView}'s items
   * @param <V> The type of the {@link RecyclerView.ViewHolder}
   */
  public static <V extends RecyclerView.ViewHolder> void doForEachViewHolder(
      RecyclerView view, Consumer<V> callback) {
    for (int i = 0; i < view.getChildCount(); i++) {
      View child = view.getChildAt(i);

      @SuppressWarnings("unchecked")
      V viewHolder = (V) view.getChildViewHolder(child);
      callback.accept(viewHolder);
    }
  }

  /**
   * Snaps or smoothly scrolls the {@code view} to the item for the given adapter {@code position}.
   *
   * <p>When the {@code view} has been laid out and has a width and height, a smooth-scroll will
   * happen. If not, the {@code view} will snap to the given {@code position} immediately.
   */
  public static void snapScrollToPosition(RecyclerView view, int position) {
    boolean itemsCentered =
        view instanceof CenteredRecyclerView && !((CenteredRecyclerView) view).areItemsCentered();
    if (view.isAttachedToWindow()
        && view.getWidth() != 0
        && view.getHeight() != 0
        && !itemsCentered) {
      view.smoothScrollToPosition(position);
    } else if (itemsCentered) {
      ((CenteredRecyclerView) view).notifyHighlightedItemIndexChanged(position);
    } else {
      view.scrollToPosition(position);
    }
  }

  /**
   * Determines and returns the offset of a {@code child}'s position within its {@code
   * layoutManager} to keep the highlighted {@code child} centered within a {@code
   * CenteredRecyclerView}'s area.
   *
   * <p>This determination corrects any unwanted offset from the center that an item's {@code
   * ItemDecoration}s may have caused.
   */
  public static Point getOffsetToKeepChildCentered(
      @Nullable LayoutManager layoutManager, View child) {
    if (layoutManager == null) {
      return new Point(0, 0);
    }

    Rect rect = new Rect();
    layoutManager.calculateItemDecorationsForChild(child, rect);

    return new Point((rect.left - rect.right) / 2, (rect.top - rect.bottom) / 2);
  }
}
