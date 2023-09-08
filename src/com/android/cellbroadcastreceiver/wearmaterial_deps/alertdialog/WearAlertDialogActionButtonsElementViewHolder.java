package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import static com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog.NEGATIVE_ACTION_BUTTON_ID;
import static com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog.POSITIVE_ACTION_BUTTON_ID;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearCircularButton;

final class WearAlertDialogActionButtonsElementViewHolder extends ViewHolder {

  private final WearCircularButton positiveButton;
  private final WearCircularButton negativeButton;
  private final WearAlertDialogListener wearAlertDialogListener;

  WearAlertDialogActionButtonsElementViewHolder(
      View actionButtons, WearAlertDialogListener wearAlertDialogListener) {
    super(actionButtons);
    positiveButton = actionButtons.findViewById(R.id.wear_alertdialog_positive_button);
    negativeButton = actionButtons.findViewById(R.id.wear_alertdialog_negative_button);
    this.wearAlertDialogListener = wearAlertDialogListener;
  }

  // dereference of possibly-null reference params
  @SuppressWarnings("nullness:dereference.of.nullable")
  void setWearAlertDialogActionButtonsElement(
      WearAlertDialogActionButtonsElement wearAlertDialogActionButtonsElement) {
    int positiveIconId = wearAlertDialogActionButtonsElement.getPositiveButtonIconId();
    int negativeIconId = wearAlertDialogActionButtonsElement.getNegativeButtonIconId();

    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) negativeButton.getLayoutParams();
    params.setMarginEnd(
        positiveIconId == 0 || negativeIconId == 0
            ? 0
            : itemView.getResources().getDimensionPixelSize(R.dimen.wear_alertdialog_buttons_gap));

    if (positiveIconId != 0) {
      positiveButton.setVisibility(View.VISIBLE);
      positiveButton.setIcon(positiveIconId);
      positiveButton.setOnClickListener(
          v -> wearAlertDialogListener.onActionButtonClicked(POSITIVE_ACTION_BUTTON_ID));
      CharSequence positiveContentDescription =
          wearAlertDialogActionButtonsElement.getPositiveButtonContentDescription();
      if (!TextUtils.isEmpty(positiveContentDescription)) {
        positiveButton.setContentDescription(positiveContentDescription);
      }
    } else {
      positiveButton.setVisibility(View.GONE);
    }

    if (negativeIconId != 0) {
      negativeButton.setVisibility(View.VISIBLE);
      negativeButton.setIcon(negativeIconId);
      negativeButton.setOnClickListener(
          v -> wearAlertDialogListener.onActionButtonClicked(NEGATIVE_ACTION_BUTTON_ID));
      CharSequence negativeContentDescription =
          wearAlertDialogActionButtonsElement.getNegativeButtonContentDescription();
      if (!TextUtils.isEmpty(negativeContentDescription)) {
        negativeButton.setContentDescription(negativeContentDescription);
      }
    } else {
      negativeButton.setVisibility(View.GONE);
    }
  }
}
