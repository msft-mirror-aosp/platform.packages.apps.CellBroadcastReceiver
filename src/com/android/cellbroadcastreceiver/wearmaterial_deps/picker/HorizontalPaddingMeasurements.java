package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static java.lang.Math.max;
import static java.lang.Math.min;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

/** A {@link PaddingMeasurements} for a horizontal {@link CenteredRecyclerView}. */
class HorizontalPaddingMeasurements implements PaddingMeasurements {

  @Override
  public float getTranslation(View view) {
    return view.getTranslationX();
  }

  @Override
  public int getSize(View view) {
    return view.getWidth();
  }

  @Override
  public int getCenter(View view) {
    return (view.getLeft() + view.getRight()) / 2;
  }

  @Override
  public int getPaddingSize(View view) {
    return view.getPaddingLeft();
  }

  @Override
  public void setPaddingSize(View view, int paddingSize) {
    int endPadding = max(0, min(view.getWidth() - paddingSize - 1, paddingSize));
    view.setPadding(paddingSize, view.getPaddingTop(), endPadding, view.getPaddingBottom());
  }

  @Override
  public void offsetChildren(RecyclerView view, int offset) {
    view.offsetChildrenHorizontal(offset);
  }

  @Override
  public boolean needsCenteredPadding(RecyclerView view) {
    int range = view.computeHorizontalScrollRange();
    return range > (view.getWidth() - view.getPaddingLeft() - view.getPaddingRight());
  }

  @Override
  public boolean isMatchingParent(View view) {
    LayoutParams layoutParams = view.getLayoutParams();
    return layoutParams != null && layoutParams.width == LayoutParams.MATCH_PARENT;
  }
}
