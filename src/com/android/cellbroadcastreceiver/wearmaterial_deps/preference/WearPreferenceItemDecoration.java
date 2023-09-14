package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ItemDecoration;
import androidx.recyclerview.widget.RecyclerView.State;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import androidx.core.content.res.ResourcesCompat;

/** The default item decoration for wear preferences. */
public final class WearPreferenceItemDecoration extends ItemDecoration {

  private final int horizontalPadding;
  private final int verticalPadding;

  public WearPreferenceItemDecoration(Context context) {
    Resources resources = context.getResources();
    int displayWidth = resources.getDisplayMetrics().widthPixels;
    horizontalPadding =
        Math.round(
            ResourcesCompat.getFloat(
                    resources, R.dimen.wear_preference_fragment_horizontal_padding_percent)
                * displayWidth);
    verticalPadding = resources.getDimensionPixelSize(R.dimen.wear_preference_padding_vertical);
  }

  @Override
  public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
    int position = parent.getChildAdapterPosition(view);
    if (position == RecyclerView.NO_POSITION) {
      return;
    }

    boolean isElementAboveBottom = position < state.getItemCount() - 1;
    // Check the layout params and not the view height, since at the time the decorations are
    // computed the views may not be measured yet.
    LayoutParams layoutParams = view.getLayoutParams();
    boolean isZeroHeightItem = layoutParams != null && layoutParams.height == 0;
    int bottomPadding = isElementAboveBottom && !isZeroHeightItem ? verticalPadding : 0;
    outRect.set(horizontalPadding, 0, horizontalPadding, bottomPadding);
  }
}
