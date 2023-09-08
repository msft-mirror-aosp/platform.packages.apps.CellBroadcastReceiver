package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ItemDecoration;
import androidx.recyclerview.widget.RecyclerView.State;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An {@link ItemDecoration} that draws a separator-string between the items of a horizontal {@link
 * RecyclerView}.
 *
 * <p>The {@code HorizontalTextItemDecoration} makes sure that the separator-strings are centered
 * horizontally between the horizontal centers of the list-items.
 */
final class HorizontalTextItemDecoration extends ItemDecoration {

  private static final float MAX_ALPHA = 255;

  /** The {@code TextView} used for rendering the text between the items. */
  private final TextView separatorRenderer;

  private final int separatorSize;

  /** The result of getting the measured width and height of {@link #separatorRenderer}. */
  private final Point measurements = new Point();

  /**
   * Children of the {@code RecyclerView} ordered by their {@code Adapter} position.
   *
   * <p>The {@link #minKeyOrderedChildren} and the {@link #maxKeyOrderedChildren} are the minimum
   * and maximum adapter position values of all the {@code RecyclerView}'s children visible on the
   * screen.
   */
  private final SparseArray<View> orderedChildren = new SparseArray<>();

  private int minKeyOrderedChildren = Integer.MAX_VALUE;
  private int maxKeyOrderedChildren = -1;

  HorizontalTextItemDecoration(TextView separatorRenderer, int separatorSize) {
    this.separatorRenderer = separatorRenderer;
    this.separatorSize = separatorSize;

    ColorStateList colorWithAlpha =
        separatorRenderer
            .getTextColors()
            .withAlpha(round(separatorRenderer.getAlpha() * MAX_ALPHA));
    separatorRenderer.setTextColor(colorWithAlpha);
  }

  @Override
  public void getItemOffsets(Rect outRect, View child, RecyclerView parent, State state) {
    if (state.getItemCount() < 2) {
      return;
    }

    getMeasurements();
    if (hasNoMeasurements()) {
      return;
    }

    // Add half the width of the separator-text as 'padding' on both sides of an item,
    // except for the first and last items, which will get the extra padding only on one side.
    int padding = (separatorSize >= 0 ? separatorSize : measurements.x) / 2;
    int position = parent.getChildAdapterPosition(child);
    if (position == 0) {
      outRect.right += padding;
    } else if (position == state.getItemCount() - 1) {
      outRect.left += padding;
    } else {
      outRect.left += padding;
      outRect.right += padding;
    }
  }

  /**
   * Measures and returns the size of the {@link #separatorRenderer} that contains the
   * separator-text.
   */
  private void getMeasurements() {
    measurements.set(separatorRenderer.getMeasuredWidth(), separatorRenderer.getMeasuredHeight());
  }

  private boolean hasNoMeasurements() {
    return measurements.x == 0 || measurements.y == 0;
  }

  @Override
  public void onDrawOver(Canvas canvas, RecyclerView parent, State state) {
    if (hasNoMeasurements() || state.getItemCount() < 2) {
      return;
    }

    orderChildrenByAdapterPosition(parent, state.getItemCount() - 1);

    drawBetweenOrderedChildren(parent, canvas);

    clearOrderedChildren();
  }

  private void orderChildrenByAdapterPosition(RecyclerView parent, int maxAdapterPosition) {
    for (int i = 0; i < parent.getChildCount(); i++) {
      View child = parent.getChildAt(i);
      int position = parent.getChildAdapterPosition(child);
      if (position != RecyclerView.NO_POSITION) {
        orderedChildren.put(position, child);

        minKeyOrderedChildren = min(position, minKeyOrderedChildren);
        maxKeyOrderedChildren = max(position, maxKeyOrderedChildren);
      }
    }

    minKeyOrderedChildren = max(minKeyOrderedChildren - 1, 0);
    maxKeyOrderedChildren = min(maxKeyOrderedChildren + 1, maxAdapterPosition);
  }

  private void drawBetweenOrderedChildren(RecyclerView parent, Canvas canvas) {
    int separatorTop = getSeparatorTop(parent);

    for (int key = minKeyOrderedChildren; key < maxKeyOrderedChildren; key++) {
      View leftChild = orderedChildren.get(key);
      View rightChild = orderedChildren.get(key + 1);
      drawSeparatorInGap(leftChild, rightChild, separatorTop, canvas);
    }
  }

  private int getSeparatorTop(RecyclerView parent) {
    int parentTop = parent.getPaddingTop();
    int parentBottom = parent.getHeight() - parent.getPaddingBottom();
    int parentCenter = (parentTop + parentBottom) / 2;

    return parentCenter - measurements.y / 2;
  }

  private void drawSeparatorInGap(
      @Nullable View leftChild, @Nullable View rightChild, int top, Canvas canvas) {
    if (leftChild == null || rightChild == null) {
      return;
    }

    int centerOfLeftChild = (leftChild.getLeft() + leftChild.getRight()) / 2;
    int centerOfRightChild = (rightChild.getLeft() + rightChild.getRight()) / 2;
    int centerOfGap = (centerOfLeftChild + centerOfRightChild) / 2;
    int left = centerOfGap - measurements.x / 2 + (int) leftChild.getTranslationX();

    int restorePoint = canvas.save();

    canvas.translate(left, top);
    separatorRenderer.draw(canvas);

    canvas.restoreToCount(restorePoint);
  }

  private void clearOrderedChildren() {
    orderedChildren.clear();
    minKeyOrderedChildren = Integer.MAX_VALUE;
    maxKeyOrderedChildren = -1;
  }
}
