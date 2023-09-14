package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import static com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog.POSITIVE_ACTION_CHIP_ID;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.View;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearChipButton;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearChipButton.TextHorizontalPos;

/** A ViewHolder for showing WearAlertDialogPositiveChipElement. */
final class WearAlertDialogPositiveChipElementViewHolder extends ViewHolder {

  private final WearChipButton positiveChipButton;

  private int positiveChipButtonId;

  WearAlertDialogPositiveChipElementViewHolder(
      View positiveChipButton, WearAlertDialogListener wearAlertDialogListener) {
    super(positiveChipButton);
    this.positiveChipButton = (WearChipButton) positiveChipButton;

    WearAlertDialogUtils.applyStyleColors(
        this.positiveChipButton, R.attr.wearAlertDialogButtonPositiveStyle);

    positiveChipButton.setOnClickListener(
        v -> wearAlertDialogListener.onActionButtonClicked(positiveChipButtonId));
  }

  void setWearAlertDialogPositiveChipElement(
      WearAlertDialogPositiveChipElement wearAlertDialogPositiveChipElement) {
    this.positiveChipButtonId = POSITIVE_ACTION_CHIP_ID;
    positiveChipButton.setPrimaryText(wearAlertDialogPositiveChipElement.getPositiveChipText());
    positiveChipButton.setPrimaryTextHorizontalPosition(TextHorizontalPos.CENTER);
  }
}
