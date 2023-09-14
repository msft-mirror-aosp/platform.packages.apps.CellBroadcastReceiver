package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static java.lang.Math.max;
import static java.lang.Math.min;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

/** A {@link PaddingMeasurements} for a vertical {@link CenteredRecyclerView}. */
class VerticalPaddingMeasurements implements PaddingMeasurements {

  @Override
  public float getTranslation(View view) {
    return view.getTranslationY();
  }

  @Override
  public int getSize(View view) {
    return view.getHeight();
  }

  @Override
  public int getCenter(View view) {
    return (view.getTop() + view.getBottom()) / 2;
  }

  @Override
  public int getPaddingSize(View view) {
    return view.getPaddingTop();
  }

  @Override
  public void setPaddingSize(View view, int paddingSize) {
    int endPadding = max(0, min(view.getHeight() - paddingSize - 1, paddingSize));
    view.setPadding(view.getPaddingLeft(), paddingSize, view.getPaddingRight(), endPadding);
  }

  @Override
  public void offsetChildren(RecyclerView view, int offset) {
    view.offsetChildrenVertical(offset);
  }

  @Override
  public boolean needsCenteredPadding(RecyclerView view) {
    return true;
  }

  @Override
  public boolean isMatchingParent(View view) {
    LayoutParams layoutParams = view.getLayoutParams();
    return layoutParams != null && layoutParams.height == LayoutParams.MATCH_PARENT;
  }
}
