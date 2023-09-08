package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import androidx.annotation.VisibleForTesting;
import com.google.common.collect.ImmutableList;

/** ItemDecoration that manages the layout bounds of WearAlertDialog elements. */
final class ItemOffsetDecoration extends RecyclerView.ItemDecoration {

  private final Resources resources;

  @VisibleForTesting int topMargin;
  @VisibleForTesting int topMarginIcon;
  @VisibleForTesting int topMarginMessage;
  @VisibleForTesting int bottomMargin;
  @VisibleForTesting int bottomChipButtonMargin;
  @VisibleForTesting int sideMargin;
  @VisibleForTesting int elementVerticalPadding;
  @VisibleForTesting int titlePadding;
  @VisibleForTesting int messageTextPadding;

  ItemOffsetDecoration(Context context) {
    resources = context.getResources();
    DisplayMetrics metrics = resources.getDisplayMetrics();
    sideMargin =
        (int)
            resources.getFraction(
                R.fraction.wear_alertdialog_side_padding_fraction,
                metrics.widthPixels,
                /* pbase= */ 1);
    topMargin =
        (int)
            resources.getFraction(
                R.fraction.wear_alertdialog_top_padding_fraction,
                metrics.heightPixels,
                /* pbase= */ 1);
    topMarginIcon =
        (int)
            resources.getFraction(
                R.fraction.wear_alertdialog_top_padding_icon_fraction,
                metrics.heightPixels,
                /* pbase= */ 1);
    topMarginMessage =
        (int)
            resources.getFraction(
                R.fraction.wear_alertdialog_top_padding_message_fraction,
                metrics.heightPixels,
                /* pbase= */ 1);
    bottomMargin =
        (int)
            resources.getFraction(
                R.fraction.wear_alertdialog_bottom_padding_fraction,
                metrics.heightPixels,
                /* pbase= */ 1);
    bottomChipButtonMargin =
        (int)
            resources.getFraction(
                R.fraction.wear_alertdialog_bottom_padding_scrollable_fraction,
                metrics.heightPixels,
                /* pbase= */ 1);
    elementVerticalPadding =
        (int) resources.getDimension(R.dimen.wear_alertdialog_element_vertical_padding);

    titlePadding = (int) resources.getDimension(R.dimen.wear_alertdialog_title_margin_top);

    messageTextPadding = (int) resources.getDimension(R.dimen.wear_alertdialog_message_margin_top);
  }

  @Override
  public void getItemOffsets(
      Rect outRect, View view, RecyclerView recyclerView, RecyclerView.State state) {
    outRect.setEmpty();

    WearAlertDialogRecyclerAdapter adapter =
        (WearAlertDialogRecyclerAdapter) recyclerView.getAdapter();
    int position = recyclerView.getChildAdapterPosition(view);
    if (position == RecyclerView.NO_POSITION || adapter == null) {
      return;
    }

    int viewType = adapter.getItemViewType(position);

    outRect.set(
        sideMargin,
        getTopMargin(position, viewType, adapter),
        sideMargin,
        getBottomMargin(position, adapter));
  }

  private int getBottomMargin(int position, WearAlertDialogRecyclerAdapter adapter) {
    int itemCount = adapter.getItemCount();
    // check if its last element of the list.
    if (itemCount < 1 || position != itemCount - 1) {
      return 0;
    }
    // If last item is a chip button, add a larger bottom margin
    if (adapter.getItemViewType(position) == WearAlertDialogViewType.CHIP_BUTTONS) {
      return bottomChipButtonMargin;
    } else {
      return bottomMargin;
    }
  }

  private int getTopMargin(int position, int viewType, WearAlertDialogRecyclerAdapter adapter) {
    ImmutableList<WearAlertDialogElement> elementList = adapter.getElements();
    if (position == 0) {
      switch (viewType) {
        case WearAlertDialogViewType.ICON:
          return topMarginIcon;
        case WearAlertDialogViewType.MESSAGE_TEXT:
          return topMarginMessage;
        default:
          return topMargin + titlePadding;
      }
    } else {
      switch (viewType) {
        case WearAlertDialogViewType.TITLE:
          return isUpperElementViewTypeIcon(position, elementList) ? this.titlePadding : 0;
        case WearAlertDialogViewType.MESSAGE_TEXT:
          return isUpperElementViewTypeTitle(position, elementList) ? this.messageTextPadding : 0;
        case WearAlertDialogViewType.POSITIVE_CHIP:
        case WearAlertDialogViewType.NEGATIVE_CHIP:
          return isUpperElementEitherPositivenOrNegativeChip(position, elementList)
              ? 0
              : elementVerticalPadding;
        default:
          return elementList.get(position - 1).getViewType() != viewType
              ? elementVerticalPadding
              : 0;
      }
    }
  }

  private boolean isUpperElementViewTypeIcon(
      int position, ImmutableList<WearAlertDialogElement> elementList) {
    return elementList.get(position - 1).getViewType() == WearAlertDialogViewType.ICON;
  }

  private boolean isUpperElementViewTypeTitle(
      int position, ImmutableList<WearAlertDialogElement> elementList) {
    return elementList.get(position - 1).getViewType() == WearAlertDialogViewType.TITLE;
  }

  private boolean isUpperElementEitherPositivenOrNegativeChip(
      int position, ImmutableList<WearAlertDialogElement> elementList) {
    int viewType = elementList.get(position - 1).getViewType();
    return viewType == WearAlertDialogViewType.POSITIVE_CHIP
        || viewType == WearAlertDialogViewType.NEGATIVE_CHIP;
  }
}
